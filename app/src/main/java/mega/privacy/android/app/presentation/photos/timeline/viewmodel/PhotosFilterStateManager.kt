package mega.privacy.android.app.presentation.photos.timeline.viewmodel

import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.app.presentation.photos.timeline.model.ApplyFilterMediaType
import mega.privacy.android.app.presentation.photos.timeline.model.FilterMediaType
import mega.privacy.android.app.presentation.photos.timeline.model.TimelinePhotosSource

fun TimelineViewModel.updateFilterMediaType(mediaType: FilterMediaType) {
    _state.update {
        it.copy(currentFilterMediaType = mediaType)
    }
}

fun TimelineViewModel.updateMediaSource(source: TimelinePhotosSource) {
    _state.update {
        it.copy(currentMediaSource = source)
    }
}

fun TimelineViewModel.updateApplyFilterMediaType(applyFilterMediaType: ApplyFilterMediaType) {
    _state.update {
        it.copy(applyFilterMediaType = applyFilterMediaType)
    }
}

fun TimelineViewModel.onMediaTypeSelected(mediaType: FilterMediaType) {
    updateFilterMediaType(mediaType)
}

fun TimelineViewModel.onSourceSelected(source: TimelinePhotosSource) {
    updateMediaSource(source)
}

suspend fun TimelineViewModel.applyFilter() {
    withContext(ioDispatcher) {
        createAndUpdateFilterType()
        handleAndUpdatePhotosUIState(_state.value.photos, filterMedias(_state.value.photos))
    }
}

fun TimelineViewModel.createAndUpdateFilterType() {
    val mediaType = _state.value.currentFilterMediaType
    val source = _state.value.currentMediaSource
    if (mediaType == FilterMediaType.ALL_MEDIA && source == TimelinePhotosSource.ALL_PHOTOS) {
        updateApplyFilterMediaType(ApplyFilterMediaType.ALL_MEDIA_IN_CD_AND_CU)
    } else if (mediaType == FilterMediaType.ALL_MEDIA && source == TimelinePhotosSource.CLOUD_DRIVE) {
        updateApplyFilterMediaType(ApplyFilterMediaType.ALL_MEDIA_IN_CD)
    } else if (mediaType == FilterMediaType.ALL_MEDIA && source == TimelinePhotosSource.CAMERA_UPLOAD) {
        updateApplyFilterMediaType(ApplyFilterMediaType.ALL_MEDIA_IN_CU)
    } else if (mediaType == FilterMediaType.IMAGES && source == TimelinePhotosSource.ALL_PHOTOS) {
        updateApplyFilterMediaType(ApplyFilterMediaType.IMAGES_IN_CD_AND_CU)
    } else if (mediaType == FilterMediaType.IMAGES && source == TimelinePhotosSource.CLOUD_DRIVE) {
        updateApplyFilterMediaType(ApplyFilterMediaType.IMAGES_IN_CD)
    } else if (mediaType == FilterMediaType.IMAGES && source == TimelinePhotosSource.CAMERA_UPLOAD) {
        updateApplyFilterMediaType(ApplyFilterMediaType.IMAGES_IN_CU)
    } else if (mediaType == FilterMediaType.VIDEOS && source == TimelinePhotosSource.ALL_PHOTOS) {
        updateApplyFilterMediaType(ApplyFilterMediaType.VIDEOS_IN_CD_AND_CU)
    } else if (mediaType == FilterMediaType.VIDEOS && source == TimelinePhotosSource.CLOUD_DRIVE) {
        updateApplyFilterMediaType(ApplyFilterMediaType.VIDEOS_IN_CD)
    } else if (mediaType == FilterMediaType.VIDEOS && source == TimelinePhotosSource.CAMERA_UPLOAD) {
        updateApplyFilterMediaType(ApplyFilterMediaType.VIDEOS_IN_CU)
    }
}

internal suspend fun TimelineViewModel.filterMedias(photos: List<Photo>): List<Photo> {
    return when (_state.value.applyFilterMediaType) {
        ApplyFilterMediaType.ALL_MEDIA_IN_CD_AND_CU -> filterAllPhotos(photos)
        ApplyFilterMediaType.ALL_MEDIA_IN_CD -> filterCloudDrivePhotos(photos)
        ApplyFilterMediaType.ALL_MEDIA_IN_CU -> filterCameraUploadPhotos(photos)
        ApplyFilterMediaType.IMAGES_IN_CD_AND_CU -> filterAllImages(photos)
        ApplyFilterMediaType.IMAGES_IN_CD -> filterCloudDriveImages(photos)
        ApplyFilterMediaType.IMAGES_IN_CU -> filterCameraUploadImages(photos)
        ApplyFilterMediaType.VIDEOS_IN_CD_AND_CU -> filterAllVideos(photos)
        ApplyFilterMediaType.VIDEOS_IN_CD -> filterCloudDriveVideos(photos)
        ApplyFilterMediaType.VIDEOS_IN_CU -> filterCameraUploadVideos(photos)
    }
}

internal fun TimelineViewModel.updateFilterState(
    showFilterDialog: Boolean,
    scrollStartIndex: Int,
    scrollStartOffset: Int = 0
) {
    val type = _state.value.applyFilterMediaType.type
    val source = _state.value.applyFilterMediaType.source
    _state.update {
        it.copy(
            currentFilterMediaType = type,
            currentMediaSource = source,
            showingFilterPage = showFilterDialog,
            scrollStartIndex = scrollStartIndex,
            scrollStartOffset = scrollStartOffset,
            enableCameraUploadPageShowing = false,
        )
    }
}

private fun TimelineViewModel.filterAllPhotos(photos: List<Photo>): List<Photo> = photos

private suspend fun TimelineViewModel.filterCloudDrivePhotos(photos: List<Photo>): List<Photo> =
    getCloudDrivePhotos(photos)

private suspend fun TimelineViewModel.filterCameraUploadPhotos(photos: List<Photo>): List<Photo> =
    getCameraUploadPhotos(photos)


private fun TimelineViewModel.filterAllImages(photos: List<Photo>): List<Photo> =
    photos.filterIsInstance<Photo.Image>()


private suspend fun TimelineViewModel.filterCloudDriveImages(photos: List<Photo>): List<Photo> =
    getCloudDrivePhotos(photos).filterIsInstance<Photo.Image>()

private suspend fun TimelineViewModel.filterCameraUploadImages(photos: List<Photo>): List<Photo> =
    getCameraUploadPhotos(photos).filterIsInstance<Photo.Image>()

private fun TimelineViewModel.filterAllVideos(photos: List<Photo>): List<Photo> =
    photos.filterIsInstance<Photo.Video>()


private suspend fun TimelineViewModel.filterCloudDriveVideos(photos: List<Photo>): List<Photo> =
    getCloudDrivePhotos(photos).filterIsInstance<Photo.Video>()

private suspend fun TimelineViewModel.filterCameraUploadVideos(photos: List<Photo>): List<Photo> =
    getCameraUploadPhotos(photos).filterIsInstance<Photo.Video>()



















