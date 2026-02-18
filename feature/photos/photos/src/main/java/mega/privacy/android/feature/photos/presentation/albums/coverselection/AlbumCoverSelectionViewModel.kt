package mega.privacy.android.feature.photos.presentation.albums.coverselection

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.core.nodecomponents.mapper.FileTypeIconMapper
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
import mega.privacy.android.domain.usecase.photos.UpdateAlbumCoverUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.DownloadThumbnailUseCase
import mega.privacy.android.feature.photos.mapper.PhotoUiStateMapper
import mega.privacy.android.feature.photos.model.PhotoNodeUiState
import mega.privacy.android.feature.photos.model.PhotoUiState
import mega.privacy.android.feature.photos.model.PhotosNodeContentItem
import timber.log.Timber
import java.io.File

@HiltViewModel(assistedFactory = AlbumCoverSelectionViewModel.Factory::class)
@OptIn(ExperimentalCoroutinesApi::class)
class AlbumCoverSelectionViewModel @AssistedInject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getUserAlbum: GetUserAlbum,
    private val getAlbumPhotos: GetAlbumPhotos,
    private val downloadThumbnailUseCase: DownloadThumbnailUseCase,
    private val updateAlbumCoverUseCase: UpdateAlbumCoverUseCase,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val getBusinessStatusUseCase: GetBusinessStatusUseCase,
    private val photoUiStateMapper: PhotoUiStateMapper,
    private val fileTypeIconMapper: FileTypeIconMapper,
    @Assisted private val albumId: Long?,
) : ViewModel() {
    private val _state = MutableStateFlow(AlbumCoverSelectionState())
    val state: StateFlow<AlbumCoverSelectionState> = _state

    private var fetchPhotosJob: Job? = null

    @VisibleForTesting
    internal var showHiddenItems: Boolean? = null

    private var currentCoverId: Long? = null

    private val currentAlbumId: Long?
        get() = savedStateHandle["album_id"] ?: albumId

    init {
        viewModelScope.launch {
            fetchShowHiddenItems()
            fetchAccountDetail()
            fetchAlbum()
        }
    }

    private fun fetchAlbum() {
        val id = currentAlbumId ?: return
        getUserAlbum(albumId = AlbumId(id))
            .onEach(::updateAlbum)
            .filterNotNull()
            .onEach(::fetchPhotos)
            .catch { exception -> Timber.e(exception) }
            .launchIn(viewModelScope)
    }

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
        currentCoverId = album
            ?.cover
            ?.let { photo ->
                if (showHiddenItems || !isPaid) {
                    photo
                } else {
                    photo.takeIf { !it.isSensitive && !it.isSensitiveInherited }
                }
            }?.id

        _state.update { state ->
            state.copy(
                album = album,
                isInvalidAlbum = album == null,
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
                photosNodeContentItems = uiPhotos,
                currentCoverId = currentCoverId ?: nonSensitivePhotos.firstOrNull()?.id
            )
        }
    }

    private suspend fun sortPhotos(photos: List<Photo>): List<PhotoUiState> =
        withContext(defaultDispatcher) {
            val photosUiState = photos.map(photoUiStateMapper::invoke)
            photosUiState.sortedWith(compareByDescending<PhotoUiState> { it.modificationTime }.thenByDescending { it.id })
        }

    private fun filterNonSensitivePhotos(photos: List<PhotoUiState>): List<PhotoUiState> {
        val showHiddenItems = showHiddenItems ?: return photos
        val isPaid = _state.value.accountType?.isPaid ?: return photos
        return if (showHiddenItems || !isPaid || _state.value.isBusinessAccountExpired) {
            photos
        } else {
            photos.filter { !it.isSensitive && !it.isSensitiveInherited }
        }
    }

    private suspend fun List<PhotoUiState>.toUIPhotos(): List<PhotosNodeContentItem> =
        withContext(defaultDispatcher) {
            val isPaid = _state.value.accountType?.isPaid == true
            val isBusinessExpired = _state.value.isBusinessAccountExpired
            this@toUIPhotos.map { photo ->
                PhotosNodeContentItem.PhotoNodeItem(
                    node = PhotoNodeUiState(
                        photo = photo,
                        isSensitive = isPaid &&
                                !isBusinessExpired &&
                                (photo.isSensitive || photo.isSensitiveInherited),
                        defaultIcon = fileTypeIconMapper(photo.fileTypeInfo.extension),
                    )
                )
            }
        }

    suspend fun downloadPhoto(
        photo: Photo,
        callback: (Boolean) -> Unit,
    ) = withContext(defaultDispatcher) {
        val thumbnailFilePath = photo.thumbnailFilePath ?: return@withContext

        if (File(thumbnailFilePath).exists()) callback(true)
        else {
            runCatching { downloadThumbnailUseCase(photo.id) }
                .onSuccess { callback(true) }
                .onFailure { callback(false) }
        }
    }

    fun updateCover(selectedId: Long) {
        val currentState = _state.value
        val selectedPhotoNode = currentState.photosNodeContentItems
            .firstOrNull {
                it is PhotosNodeContentItem.PhotoNodeItem && it.node.photo.id == selectedId
            } as? PhotosNodeContentItem.PhotoNodeItem ?: return
        val albumId = currentState.album?.id ?: return
        val elementId = selectedPhotoNode.node.photo.albumPhotoId?.let { NodeId(it) } ?: return

        viewModelScope.launch {
            runCatching {
                updateAlbumCoverUseCase(albumId, elementId)
            }.onSuccess {
                _state.update {
                    it.copy(isSelectionCompleted = true)
                }
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(albumId: Long?): AlbumCoverSelectionViewModel
    }
}
