package mega.privacy.android.app.presentation.photos.albums.photosselection

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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.app.presentation.photos.albums.photosselection.AlbumPhotosSelectionActivity.Companion.ALBUM_ID
import mega.privacy.android.app.presentation.photos.model.UIPhoto
import mega.privacy.android.app.presentation.photos.model.UIPhoto.PhotoItem
import mega.privacy.android.app.presentation.photos.model.UIPhoto.Separator
import mega.privacy.android.app.presentation.photos.timeline.model.TimelinePhotosSource
import mega.privacy.android.app.presentation.photos.timeline.model.TimelinePhotosSource.ALL_PHOTOS
import mega.privacy.android.app.presentation.photos.timeline.model.TimelinePhotosSource.CAMERA_UPLOAD
import mega.privacy.android.app.presentation.photos.timeline.model.TimelinePhotosSource.CLOUD_DRIVE
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.usecase.AddPhotosToAlbum
import mega.privacy.android.domain.usecase.DownloadThumbnail
import mega.privacy.android.domain.usecase.FilterCameraUploadPhotos
import mega.privacy.android.domain.usecase.FilterCloudDrivePhotos
import mega.privacy.android.domain.usecase.GetAlbumPhotos
import mega.privacy.android.domain.usecase.GetTimelinePhotos
import mega.privacy.android.domain.usecase.GetUserAlbum
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class AlbumPhotosSelectionViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getUserAlbum: GetUserAlbum,
    private val getAlbumPhotos: GetAlbumPhotos,
    private val getTimelinePhotos: GetTimelinePhotos,
    private val downloadThumbnail: DownloadThumbnail,
    private val filterCloudDrivePhotos: FilterCloudDrivePhotos,
    private val filterCameraUploadPhotos: FilterCameraUploadPhotos,
    private val addPhotosToAlbum: AddPhotosToAlbum,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val _state = MutableStateFlow(AlbumPhotosSelectionState())
    val state: StateFlow<AlbumPhotosSelectionState> = _state

    private var addPhotosJob: Job? = null

    init {
        fetchAlbum()
        fetchPhotos()
    }

    private fun fetchAlbum() = savedStateHandle.getStateFlow<Long?>(ALBUM_ID, null)
        .filterNotNull()
        .flatMapLatest { id -> getUserAlbum(albumId = AlbumId(id)) }
        .onEach(::updateAlbum)
        .filterNotNull()
        .flatMapLatest { album -> getAlbumPhotos(album.id) }
        .onEach(::updateAlbumPhotos)
        .catch { exception -> Timber.e(exception) }
        .launchIn(viewModelScope)

    private fun updateAlbum(album: Album.UserAlbum?) {
        _state.update {
            it.copy(
                album = album,
                isInvalidAlbum = album == null,
            )
        }
    }

    private suspend fun updateAlbumPhotos(albumPhotos: List<Photo>) {
        _state.update {
            val albumPhotoIds = withContext(defaultDispatcher) {
                albumPhotos.map { photo -> photo.id }.toSet()
            }
            it.copy(albumPhotoIds = albumPhotoIds)
        }
    }

    private fun fetchPhotos() = getTimelinePhotos()
        .mapLatest(::sortPhotos)
        .mapLatest(::determineLocation)
        .onEach { filterPhotos() }
        .catch { exception -> Timber.e(exception) }
        .launchIn(viewModelScope)

    private suspend fun sortPhotos(photos: List<Photo>): List<Photo> =
        withContext(defaultDispatcher) {
            photos.sortedByDescending { it.modificationTime }
        }

    private suspend fun determineLocation(photos: List<Photo>) {
        val numCloudDrivePhotos = filterCloudDrivePhotos(photos).size
        val numCameraUploadPhotos = filterCameraUploadPhotos(photos).size

        val currentLocation = _state.value.selectedLocation
        val (candidateLocation, showFilterMenu) = when {
            numCloudDrivePhotos > 0 && numCameraUploadPhotos > 0 -> ALL_PHOTOS to true
            numCloudDrivePhotos > 0 -> CLOUD_DRIVE to false
            numCameraUploadPhotos > 0 -> CAMERA_UPLOAD to false
            else -> ALL_PHOTOS to false
        }
        val newLocation = when {
            currentLocation == ALL_PHOTOS && (numCloudDrivePhotos == 0 || numCameraUploadPhotos == 0) -> candidateLocation
            currentLocation == CLOUD_DRIVE && numCloudDrivePhotos == 0 -> candidateLocation
            currentLocation == CAMERA_UPLOAD && numCameraUploadPhotos == 0 -> candidateLocation
            else -> currentLocation
        }
        val isLocationDetermined = candidateLocation != ALL_PHOTOS || showFilterMenu

        _state.update {
            it.copy(
                photos = photos,
                selectedLocation = newLocation,
                isLocationDetermined = isLocationDetermined,
                showFilterMenu = showFilterMenu,
            )
        }
    }

    fun updateLocation(location: TimelinePhotosSource) {
        _state.update {
            it.copy(selectedLocation = location)
        }
    }

    fun filterPhotos() = viewModelScope.launch {
        val photos = _state.value.photos
        val location = _state.value.selectedLocation

        val uiPhotos = when (location) {
            ALL_PHOTOS -> photos
            CLOUD_DRIVE -> filterCloudDrivePhotos(photos)
            CAMERA_UPLOAD -> filterCameraUploadPhotos(photos)
        }.toUIPhotos()

        _state.update {
            it.copy(uiPhotos = uiPhotos)
        }
    }

    private suspend fun List<Photo>.toUIPhotos(): List<UIPhoto> =
        withContext(defaultDispatcher) {
            flatMapIndexed { index, photo ->
                val comparePeriods = {
                    val currentDate = photo.modificationTime.toLocalDate()
                    val previousDate = get(index - 1).modificationTime.toLocalDate()
                    currentDate.month == previousDate.month
                }
                val showDateSeparator = index == 0 || !comparePeriods()

                listOfNotNull(
                    Separator(photo.modificationTime).takeIf { showDateSeparator },
                    PhotoItem(photo),
                )
            }
        }

    suspend fun downloadPhoto(
        photo: Photo,
        callback: (Boolean) -> Unit,
    ) = withContext(defaultDispatcher) {
        val thumbnailFilePath = photo.thumbnailFilePath ?: return@withContext

        if (File(thumbnailFilePath).exists()) callback(true)
        else downloadThumbnail(nodeId = photo.id, callback)
    }

    fun selectAllPhotos() = viewModelScope.launch {
        _state.update {
            val selectedPhotoIds = withContext(defaultDispatcher) {
                val photoIds = it.uiPhotos
                    .filterIsInstance<PhotoItem>()
                    .map { item -> item.photo.id }
                it.selectedPhotoIds + photoIds
            }
            it.copy(selectedPhotoIds = selectedPhotoIds)
        }
    }

    fun clearSelection() {
        _state.update {
            it.copy(selectedPhotoIds = setOf())
        }
    }

    fun selectPhoto(photo: Photo) = viewModelScope.launch {
        _state.update {
            val selectedPhotoIds = withContext(defaultDispatcher) {
                it.selectedPhotoIds + photo.id
            }
            it.copy(selectedPhotoIds = selectedPhotoIds)
        }
    }

    fun unselectPhoto(photo: Photo) = viewModelScope.launch {
        _state.update {
            val selectedPhotoIds = withContext(defaultDispatcher) {
                it.selectedPhotoIds - photo.id
            }
            it.copy(selectedPhotoIds = selectedPhotoIds)
        }
    }

    fun addPhotos(album: Album.UserAlbum, selectedPhotoIds: Set<Long>) {
        addPhotosJob?.cancel()
        addPhotosJob = viewModelScope.launch {
            val photoIds = withContext(defaultDispatcher) {
                val albumPhotoIds = _state.value.albumPhotoIds
                selectedPhotoIds - albumPhotoIds
            }
            addPhotosToAlbum(
                albumId = album.id,
                photoIds = photoIds.map { NodeId(it) },
            )

            _state.update {
                it.copy(
                    isSelectionCompleted = true,
                    numCommittedPhotos = photoIds.size,
                )
            }
        }
    }
}
