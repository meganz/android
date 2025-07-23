package mega.privacy.android.app.presentation.photos.albums.coverselection

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.app.featuretoggle.ApiFeatures
import mega.privacy.android.app.presentation.photos.albums.AlbumScreenWrapperActivity.Companion.ALBUM_ID
import mega.privacy.android.app.presentation.photos.model.MediaListItem
import mega.privacy.android.app.presentation.photos.model.MediaListItem.PhotoItem
import mega.privacy.android.core.formatter.mapper.DurationInSecondsTextMapper
import mega.privacy.android.domain.entity.account.business.BusinessAccountStatus
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.usecase.GetAlbumPhotos
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.GetUserAlbum
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.photos.UpdateAlbumCoverUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.DownloadThumbnailUseCase
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class AlbumCoverSelectionViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getUserAlbum: GetUserAlbum,
    private val getAlbumPhotos: GetAlbumPhotos,
    private val downloadThumbnailUseCase: DownloadThumbnailUseCase,
    private val updateAlbumCoverUseCase: UpdateAlbumCoverUseCase,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val getBusinessStatusUseCase: GetBusinessStatusUseCase,
    private val durationInSecondsTextMapper: DurationInSecondsTextMapper,
) : ViewModel() {
    private val _state = MutableStateFlow(AlbumCoverSelectionState())
    val state: StateFlow<AlbumCoverSelectionState> = _state

    private var fetchPhotosJob: Job? = null

    @VisibleForTesting
    internal var showHiddenItems: Boolean? = null

    init {
        viewModelScope.launch {
            if (isHiddenNodesActive()) {
                fetchShowHiddenItems()
                fetchAccountDetail()
            }

            fetchAlbum()
        }
    }

    private suspend fun isHiddenNodesActive(): Boolean {
        val result = runCatching {
            getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)
        }
        return result.getOrNull() ?: false
    }

    private fun fetchAlbum() = savedStateHandle.getStateFlow<Long?>(ALBUM_ID, null)
        .filterNotNull()
        .flatMapLatest { id -> getUserAlbum(albumId = AlbumId(id)) }
        .onEach(::updateAlbum)
        .filterNotNull()
        .onEach(::fetchPhotos)
        .catch { exception -> Timber.e(exception) }
        .launchIn(viewModelScope)

    private suspend fun fetchShowHiddenItems() {
        showHiddenItems = monitorShowHiddenItemsUseCase().firstOrNull() ?: true
    }

    private suspend fun fetchAccountDetail() {
        val accountDetail = monitorAccountDetailUseCase().firstOrNull()
        val accountType = accountDetail?.levelDetail?.accountType
        val businessStatus =
            if (accountType?.isBusinessAccount == true) {
                getBusinessStatusUseCase()
            } else null

        _state.update {
            it.copy(
                accountType = accountType,
                isBusinessAccountExpired = businessStatus == BusinessAccountStatus.Expired,
                hiddenNodeEnabled = true,
            )
        }
    }

    private fun updateAlbum(album: Album.UserAlbum?) {
        val showHiddenItems = showHiddenItems ?: true
        val isPaid = _state.value.accountType?.isPaid ?: false

        _state.update { state ->
            state.copy(
                album = album,
                isInvalidAlbum = album == null,
                selectedPhoto = album?.cover?.let { photo ->
                    if (showHiddenItems || !isPaid) {
                        photo
                    } else {
                        photo.takeIf { !it.isSensitive && !it.isSensitiveInherited }
                    }
                },
            )
        }
    }

    private fun fetchPhotos(album: Album.UserAlbum) {
        if (fetchPhotosJob != null) return
        fetchPhotosJob = viewModelScope.launch(defaultDispatcher) {
            val photos = getAlbumPhotos(album.id).firstOrNull() ?: return@launch
            updatePhotos(photos)
        }
    }

    private suspend fun updatePhotos(photos: List<Photo>) {
        val sortedPhotos = sortPhotos(photos)
        val nonSensitivePhotos = filterNonSensitivePhotos(sortedPhotos)
        val uiPhotos = nonSensitivePhotos.toUIPhotos()

        _state.update { state ->
            state.copy(
                photos = sortedPhotos,
                mediaListItems = uiPhotos,
                selectedPhoto = state.selectedPhoto ?: nonSensitivePhotos.firstOrNull(),
            )
        }
    }

    private suspend fun sortPhotos(photos: List<Photo>): List<Photo> =
        withContext(defaultDispatcher) {
            photos.sortedWith(compareByDescending<Photo> { it.modificationTime }.thenByDescending { it.id })
        }

    private fun filterNonSensitivePhotos(photos: List<Photo>): List<Photo> {
        val showHiddenItems = showHiddenItems ?: return photos
        val isPaid = _state.value.accountType?.isPaid ?: return photos

        return if (showHiddenItems || !isPaid || _state.value.isBusinessAccountExpired) {
            photos
        } else {
            photos.filter { !it.isSensitive && !it.isSensitiveInherited }
        }
    }

    private suspend fun List<Photo>.toUIPhotos(): List<MediaListItem> =
        withContext(defaultDispatcher) {
            this@toUIPhotos.map {
                when (it) {
                    is Photo.Image -> PhotoItem(it)
                    is Photo.Video -> MediaListItem.VideoItem(
                        it,
                        durationInSecondsTextMapper(it.fileTypeInfo.duration)
                    )
                }
            }
        }

    suspend fun downloadPhoto(
        photo: Photo,
        callback: (Boolean) -> Unit,
    ) = withContext(defaultDispatcher) {
        val thumbnailFilePath = photo.thumbnailFilePath ?: return@withContext

        if (File(thumbnailFilePath).exists()) callback(true)
        else downloadThumbnailUseCase(nodeId = photo.id, callback)
    }

    fun selectPhoto(photo: Photo) {
        _state.update { state ->
            state.copy(
                selectedPhoto = photo,
                hasSelectedPhoto = state.hasSelectedPhoto.let { hasSelectedPhoto ->
                    if (hasSelectedPhoto) hasSelectedPhoto
                    else photo != state.selectedPhoto
                },
            )
        }
    }

    fun updateCover(album: Album.UserAlbum?, photo: Photo?) = viewModelScope.launch {
        val albumId = album?.id ?: return@launch
        val elementId = photo?.albumPhotoId?.let { NodeId(it) } ?: return@launch

        updateAlbumCoverUseCase(albumId, elementId)

        _state.update {
            it.copy(isSelectionCompleted = true)
        }
    }
}
