package mega.privacy.android.feature.photos.presentation.albums.photosselection

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
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
import mega.privacy.android.domain.usecase.AddPhotosToAlbum
import mega.privacy.android.domain.usecase.FilterCameraUploadPhotos
import mega.privacy.android.domain.usecase.FilterCloudDrivePhotos
import mega.privacy.android.domain.usecase.GetAlbumPhotos
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.GetUserAlbum
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.photos.GetTimelinePhotosUseCase
import mega.privacy.android.domain.usecase.photos.MonitorPaginatedTimelinePhotosUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.DownloadThumbnailUseCase
import mega.privacy.android.feature.photos.mapper.PhotoUiStateMapper
import mega.privacy.android.feature.photos.model.AlbumFlow
import mega.privacy.android.feature.photos.model.PhotoNodeUiState
import mega.privacy.android.feature.photos.model.PhotoUiState
import mega.privacy.android.feature.photos.model.PhotosNodeContentType
import mega.privacy.android.feature.photos.model.TimelinePhotosSource
import mega.privacy.android.feature.photos.model.TimelinePhotosSource.ALL_PHOTOS
import mega.privacy.android.feature.photos.model.TimelinePhotosSource.CAMERA_UPLOAD
import mega.privacy.android.feature.photos.model.TimelinePhotosSource.CLOUD_DRIVE
import mega.privacy.android.feature_flags.AppFeatures
import timber.log.Timber
import java.io.File

@HiltViewModel(assistedFactory = AlbumPhotosSelectionViewModel.Factory::class)
@OptIn(ExperimentalCoroutinesApi::class)
class AlbumPhotosSelectionViewModel @AssistedInject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getUserAlbum: GetUserAlbum,
    private val getAlbumPhotos: GetAlbumPhotos,
    private val getTimelinePhotosUseCase: GetTimelinePhotosUseCase,
    private val monitorPaginatedTimelinePhotosUseCase: MonitorPaginatedTimelinePhotosUseCase,
    private val downloadThumbnailUseCase: DownloadThumbnailUseCase,
    private val filterCloudDrivePhotos: FilterCloudDrivePhotos,
    private val filterCameraUploadPhotos: FilterCameraUploadPhotos,
    private val addPhotosToAlbum: AddPhotosToAlbum,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val getBusinessStatusUseCase: GetBusinessStatusUseCase,
    private val photoUiStateMapper: PhotoUiStateMapper,
    private val fileTypeIconMapper: FileTypeIconMapper,
    @Assisted private val albumId: Long?,
    @Assisted private val selectionMode: Int?,
) : ViewModel() {
    private val _state = MutableStateFlow(AlbumPhotosSelectionState())
    val state: StateFlow<AlbumPhotosSelectionState> = _state

    private var showHiddenItems: Boolean? = null

    init {
        extractAlbumFlow()

        viewModelScope.launch {
            monitorShowHiddenItems()
            monitorAccountDetail()

            fetchAlbum()
            fetchPhotos()
        }
    }

    private fun extractAlbumFlow() = savedStateHandle
        .getStateFlow("album_flow", selectionMode ?: 0)
        .onEach(::updateAlbumFlow)
        .catch { exception -> Timber.e(exception) }
        .launchIn(viewModelScope)

    private fun updateAlbumFlow(type: Int) {
        _state.update {
            it.copy(albumFlow = AlbumFlow.entries[type])
        }
    }

    private fun fetchAlbum() = savedStateHandle
        .getStateFlow("album_id", albumId)
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

    private fun getPhotos() = flow {
        val isPaginationEnabled = runCatching {
            getFeatureFlagValueUseCase(AppFeatures.TimelinePhotosPagination)
        }.getOrDefault(false)

        if (isPaginationEnabled) {
            emitAll(monitorPaginatedTimelinePhotosUseCase())
        } else {
            emitAll(getTimelinePhotosUseCase())
        }
    }

    private fun fetchPhotos() = getPhotos()
        .mapLatest(::sortPhotos)
        .mapLatest(::saveSourcePhotos)
        .mapLatest(::filterNonSensitivePhotos)
        .mapLatest(::determineLocation)
        .onEach {
            filterPhotos()
            updateSelection(photos = _state.value.photos)
        }
        .catch { exception -> Timber.e(exception) }
        .launchIn(viewModelScope)

    private suspend fun sortPhotos(photos: List<Photo>): List<Photo> =
        withContext(defaultDispatcher) {
            photos.sortedByDescending { it.modificationTime }
        }

    private fun saveSourcePhotos(photos: List<Photo>): List<Photo> {
        _state.update {
            it.copy(sourcePhotos = photos)
        }
        return photos
    }

    private suspend fun filterNonSensitivePhotos(photos: List<Photo>): List<Photo> =
        withContext(defaultDispatcher) {
            val showHiddenItems = showHiddenItems ?: true
            val isPaid = _state.value.accountType?.isPaid ?: false

            if (showHiddenItems || !isPaid || _state.value.isBusinessAccountExpired) {
                photos
            } else {
                photos.filter { !it.isSensitive && !it.isSensitiveInherited }
            }
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
                isLoading = false,
            )
        }
    }

    fun updateLocation(location: TimelinePhotosSource) {
        _state.update {
            it.copy(selectedLocation = location)
        }
        filterPhotos()
    }

    fun filterPhotos() {
        viewModelScope.launch {
            val photos = _state.value.photos
            val location = _state.value.selectedLocation
            val selectedPhotoIds = _state.value.selectedPhotoIds

            val filteredPhotos = when (location) {
                ALL_PHOTOS -> photos
                CLOUD_DRIVE -> filterCloudDrivePhotos(photos)
                CAMERA_UPLOAD -> filterCameraUploadPhotos(photos)
            }

            val uiPhotos = filteredPhotos.toUIPhotos(selectedPhotoIds)
            _state.update {
                it.copy(photosNodeContentTypes = uiPhotos)
            }
        }
    }

    private suspend fun monitorShowHiddenItems() {
        showHiddenItems = monitorShowHiddenItemsUseCase().firstOrNull()
    }

    private fun monitorAccountDetail() = monitorAccountDetailUseCase()
        .onEach { accountDetail ->
            val accountType = accountDetail.levelDetail?.accountType
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
            if (_state.value.sourcePhotos.isEmpty()) return@onEach

            val filteredPhotos = filterNonSensitivePhotos(photos = _state.value.sourcePhotos)
            _state.update {
                it.copy(photos = filteredPhotos)
            }

            filterPhotos()
            updateSelection(photos = filteredPhotos)
        }
        .launchIn(viewModelScope)

    private fun updateSelection(photos: List<Photo>) {
        val selectedPhotoIds = _state.value.selectedPhotoIds.filter { id ->
            photos.any { it.id == id }
        }.toSet()

        _state.update {
            it.copy(selectedPhotoIds = selectedPhotoIds)
        }
        filterPhotos()
    }

    private suspend fun List<Photo>.toUIPhotos(selectedPhotoIds: Set<Long>): List<PhotosNodeContentType> =
        withContext(defaultDispatcher) {
            flatMapIndexed { index, photo ->
                val comparePeriods = {
                    val currentDate = photo.modificationTime.toLocalDate()
                    val previousDate = get(index - 1).modificationTime.toLocalDate()
                    currentDate.month == previousDate.month
                }
                val showDateSeparator = index == 0 || !comparePeriods()

                val photoUiState = photoUiStateMapper(photo)
                val isSelected = photo.id in selectedPhotoIds

                listOfNotNull(
                    PhotosNodeContentType.HeaderItem(
                        time = photo.modificationTime,
                        shouldShowGridSizeSettings = false,
                    ).takeIf { showDateSeparator },
                    PhotosNodeContentType.PhotoNodeItem(
                        node = PhotoNodeUiState(
                            photo = photoUiState,
                            isSensitive = photo.isSensitive || photo.isSensitiveInherited,
                            isSelected = isSelected,
                            defaultIcon = fileTypeIconMapper(photo.fileTypeInfo.extension),
                        )
                    ),
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

    fun selectAllPhotos() = viewModelScope.launch {
        _state.update {
            val selectedPhotoIds = withContext(defaultDispatcher) {
                val photoIds = it.photosNodeContentTypes
                    .filterIsInstance<PhotosNodeContentType.PhotoNodeItem>()
                    .map { item -> item.node.photo.id }
                it.selectedPhotoIds + photoIds
            }
            val updatedPhotosNodeContentTypes =
                it.photosNodeContentTypes.withSelection(selectedPhotoIds)
            it.copy(
                selectedPhotoIds = selectedPhotoIds,
                photosNodeContentTypes = updatedPhotosNodeContentTypes,
            )
        }
    }

    fun clearSelection() {
        _state.update {
            val updatedPhotosNodeContentTypes = it.photosNodeContentTypes.withSelection(setOf())
            it.copy(
                selectedPhotoIds = setOf(),
                photosNodeContentTypes = updatedPhotosNodeContentTypes,
            )
        }
    }

    fun selectPhoto(photo: PhotoUiState) {
        synchronized(Unit) {
            if (_state.value.selectedPhotoIds.size < MAX_SELECTION_NUM) {
                val selectedPhotoIds = _state.value.selectedPhotoIds + photo.id
                _state.update {
                    val updatedPhotosNodeContentTypes =
                        it.photosNodeContentTypes.withSelection(selectedPhotoIds)
                    it.copy(
                        selectedPhotoIds = selectedPhotoIds,
                        photosNodeContentTypes = updatedPhotosNodeContentTypes,
                    )
                }
            }
        }
    }

    fun unselectPhoto(photo: PhotoUiState) {
        val selectedPhotoIds = _state.value.selectedPhotoIds - photo.id
        _state.update {
            val updatedPhotosNodeContentTypes =
                it.photosNodeContentTypes.withSelection(selectedPhotoIds)
            it.copy(
                selectedPhotoIds = selectedPhotoIds,
                photosNodeContentTypes = updatedPhotosNodeContentTypes,
            )
        }
    }

    private fun List<PhotosNodeContentType>.withSelection(
        selectedPhotoIds: Set<Long>,
    ): List<PhotosNodeContentType> {
        return this.map { contentType ->
            when (contentType) {
                is PhotosNodeContentType.PhotoNodeItem -> {
                    val isSelected = contentType.node.photo.id in selectedPhotoIds
                    contentType.copy(node = contentType.node.copy(isSelected = isSelected))
                }

                is PhotosNodeContentType.HeaderItem -> contentType
            }
        }
    }

    fun addPhotos(album: Album.UserAlbum, selectedPhotoIds: Set<Long>) {
        viewModelScope.launch {
            val photoIds = withContext(defaultDispatcher) {
                val albumPhotoIds = _state.value.albumPhotoIds
                selectedPhotoIds - albumPhotoIds
            }

            if (photoIds.isEmpty()) return@launch

            runCatching {
                addPhotosToAlbum(
                    albumId = album.id,
                    photoIds = photoIds.map { NodeId(it) },
                    isAsync = false,
                )
            }.onSuccess {
                _state.update {
                    it.copy(photosSelectionCompletedEvent = triggered(photoIds.size))
                }
            }
        }
    }

    fun resetPhotosSelectionCompletedEvent() {
        _state.update {
            it.copy(photosSelectionCompletedEvent = consumed())
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(albumId: Long?, selectionMode: Int?): AlbumPhotosSelectionViewModel
    }

    companion object {
        /** Max selection number **/
        const val MAX_SELECTION_NUM = 150
    }
}
