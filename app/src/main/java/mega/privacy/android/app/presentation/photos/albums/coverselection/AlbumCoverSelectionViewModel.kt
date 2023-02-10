package mega.privacy.android.app.presentation.photos.albums.coverselection

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
import mega.privacy.android.app.presentation.photos.albums.AlbumScreenWrapperActivity.Companion.ALBUM_ID
import mega.privacy.android.app.presentation.photos.model.UIPhoto
import mega.privacy.android.app.presentation.photos.model.UIPhoto.PhotoItem
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.usecase.DownloadThumbnail
import mega.privacy.android.domain.usecase.GetAlbumPhotos
import mega.privacy.android.domain.usecase.GetUserAlbum
import mega.privacy.android.domain.usecase.UpdateAlbumCover
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class AlbumCoverSelectionViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getUserAlbum: GetUserAlbum,
    private val getAlbumPhotos: GetAlbumPhotos,
    private val downloadThumbnail: DownloadThumbnail,
    private val updateAlbumCover: UpdateAlbumCover,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val _state = MutableStateFlow(AlbumCoverSelectionState())
    val state: StateFlow<AlbumCoverSelectionState> = _state

    private var fetchPhotosJob: Job? = null

    init {
        fetchAlbum()
    }

    private fun fetchAlbum() = savedStateHandle.getStateFlow<Long?>(ALBUM_ID, null)
        .filterNotNull()
        .flatMapLatest { id -> getUserAlbum(albumId = AlbumId(id)) }
        .onEach(::updateAlbum)
        .filterNotNull()
        .onEach(::fetchPhotos)
        .catch { exception -> Timber.e(exception) }
        .launchIn(viewModelScope)

    private fun updateAlbum(album: Album.UserAlbum?) {
        _state.update {
            it.copy(
                album = album,
                isInvalidAlbum = album == null,
                selectedPhoto = album?.cover,
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
        val uiPhotos = sortedPhotos.toUIPhotos()

        _state.update { state ->
            state.copy(
                photos = sortedPhotos,
                uiPhotos = uiPhotos,
                selectedPhoto = state.selectedPhoto ?: sortedPhotos.firstOrNull(),
            )
        }
    }

    private suspend fun sortPhotos(photos: List<Photo>): List<Photo> =
        withContext(defaultDispatcher) {
            photos.sortedByDescending { it.modificationTime }
        }

    private suspend fun List<Photo>.toUIPhotos(): List<UIPhoto> =
        withContext(defaultDispatcher) {
            this@toUIPhotos.map { PhotoItem(it) }
        }

    suspend fun downloadPhoto(
        photo: Photo,
        callback: (Boolean) -> Unit,
    ) = withContext(defaultDispatcher) {
        val thumbnailFilePath = photo.thumbnailFilePath ?: return@withContext

        if (File(thumbnailFilePath).exists()) callback(true)
        else downloadThumbnail(nodeId = photo.id, callback)
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

        updateAlbumCover(albumId, elementId)

        _state.update {
            it.copy(isSelectionCompleted = true)
        }
    }
}
