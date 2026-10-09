package mega.privacy.android.app.presentation.imagepreview.fetcher

import mega.privacy.android.domain.entity.media.MediaTimelineFilter
import mega.privacy.android.domain.entity.photos.FilterMediaType
import mega.privacy.android.domain.usecase.camerauploads.GetCameraUploadFolderHandlesUseCase
import mega.privacy.android.feature.photos.mapper.TimelineFilterUiStateMapper
import mega.privacy.android.feature.photos.model.FilterMediaSource
import mega.privacy.android.feature.photos.model.TimelinePhotosSource
import mega.privacy.android.feature.photos.presentation.timeline.TimelineFilterUiState
import timber.log.Timber
import javax.inject.Inject

/**
 * Builds the SDK timeline query filter matching the grid's media type, source and sensitivity.
 */
class TimelineMediaFilterBuilder @Inject constructor(
    private val getCameraUploadFolderHandlesUseCase: GetCameraUploadFolderHandlesUseCase,
    private val timelineFilterUiStateMapper: TimelineFilterUiStateMapper,
) {

    suspend operator fun invoke(
        mediaType: FilterMediaType,
        source: TimelinePhotosSource,
        hideSensitive: Boolean,
    ): MediaTimelineFilter {
        val cameraUploadFolderHandles = runCatching { getCameraUploadFolderHandlesUseCase() }
            .onFailure { Timber.e(it, "Failed to resolve camera upload folder handles") }
            .getOrDefault(emptyList())
        val baseFilter = timelineFilterUiStateMapper(
            TimelineFilterUiState(
                mediaType = mediaType,
                mediaSource = source.toFilterMediaSource()
            ),
            cameraUploadFolderHandles,
        )
        return baseFilter.copy(
            sensitivity = if (hideSensitive) {
                MediaTimelineFilter.Sensitivity.HideSensitive
            } else {
                MediaTimelineFilter.Sensitivity.ShowAll
            },
        )
    }

    private fun TimelinePhotosSource.toFilterMediaSource(): FilterMediaSource = when (this) {
        TimelinePhotosSource.ALL_PHOTOS -> FilterMediaSource.AllPhotos
        TimelinePhotosSource.CLOUD_DRIVE -> FilterMediaSource.CloudDrive
        TimelinePhotosSource.CAMERA_UPLOAD -> FilterMediaSource.CameraUpload
    }
}
