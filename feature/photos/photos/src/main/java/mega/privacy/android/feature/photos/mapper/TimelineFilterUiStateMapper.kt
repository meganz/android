package mega.privacy.android.feature.photos.mapper

import mega.privacy.android.domain.entity.media.MediaTimelineFilter
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.TimelinePreferencesJSON
import mega.privacy.android.feature.photos.model.FilterMediaSource
import mega.privacy.android.domain.entity.photos.FilterMediaType
import mega.privacy.android.feature.photos.presentation.timeline.TimelineFilterUiState
import javax.inject.Inject

class TimelineFilterUiStateMapper @Inject constructor() {

    /**
     * Invoke.
     *
     * @param preferenceMap The preference values map.
     * @param shouldApplyFilterFromPreference Whether to apply the filter from the preference.
     * If true, we will apply the filter from the preference even though the filter is not remembered.
     */
    operator fun invoke(
        preferenceMap: Map<String, String?>?,
        shouldApplyFilterFromPreference: Boolean,
    ): TimelineFilterUiState {
        if (preferenceMap == null) return TimelineFilterUiState()

        val arePreferencesRemembered =
            preferenceMap[TimelinePreferencesJSON.JSON_KEY_REMEMBER_PREFERENCES.value].toBoolean()
        return if (arePreferencesRemembered || shouldApplyFilterFromPreference) {
            val mediaType = preferenceMap[TimelinePreferencesJSON.JSON_KEY_MEDIA_TYPE.value]
                ?: TimelinePreferencesJSON.JSON_VAL_MEDIA_TYPE_ALL_MEDIA.value
            val mediaSource = preferenceMap[TimelinePreferencesJSON.JSON_KEY_LOCATION.value]
                ?: TimelinePreferencesJSON.JSON_VAL_LOCATION_ALL_LOCATION.value
            TimelineFilterUiState(
                isRemembered = arePreferencesRemembered,
                mediaType = mediaType.toFilterMediaType(),
                mediaSource = mediaSource.toFilterMediaSource()
            )
        } else TimelineFilterUiState()
    }

    /**
     * Converts a [TimelineFilterUiState] into a [MediaTimelineFilter] for the revamp timeline.
     *
     * Media type maps directly to a [MediaTimelineFilter.Category]. Media source maps to folder-handle
     * scoping using the supplied [cameraUploadFolderHandles] (the Camera Upload / Media Upload folder
     * handles, already resolved and validated by the caller):
     * - [FilterMediaSource.AllPhotos] → no handle scoping (the location scope alone).
     * - [FilterMediaSource.CloudDrive] → exclude those folders.
     * - [FilterMediaSource.CameraUpload] → restrict to those folders.
     *
     * Sort, sensitivity and granularity stay at their defaults until the later parity steps.
     *
     * @param filterUiState The current filter UI state.
     * @param cameraUploadFolderHandles The Camera Upload + Media Upload folder handles.
     */
    operator fun invoke(
        filterUiState: TimelineFilterUiState,
        cameraUploadFolderHandles: List<NodeId>,
    ): MediaTimelineFilter {
        val category = when (filterUiState.mediaType) {
            FilterMediaType.ALL_MEDIA -> MediaTimelineFilter.Category.All
            FilterMediaType.IMAGES -> MediaTimelineFilter.Category.Photos
            FilterMediaType.VIDEOS -> MediaTimelineFilter.Category.Videos
        }
        val baseFilter = MediaTimelineFilter(
            granularity = MediaTimelineFilter.Granularity.Day,
            category = category,
            location = MediaTimelineFilter.Location.CloudDriveAndVault,
            sensitivity = MediaTimelineFilter.Sensitivity.ShowAll,
        )
        return when (filterUiState.mediaSource) {
            FilterMediaSource.AllPhotos -> baseFilter
            FilterMediaSource.CloudDrive ->
                baseFilter.copy(excludeLocationHandles = cameraUploadFolderHandles)

            FilterMediaSource.CameraUpload ->
                baseFilter.copy(includeLocationHandles = cameraUploadFolderHandles)
        }
    }

    private fun String?.toFilterMediaType(): FilterMediaType =
        when (this) {
            TimelinePreferencesJSON.JSON_VAL_MEDIA_TYPE_ALL_MEDIA.value -> FilterMediaType.ALL_MEDIA
            TimelinePreferencesJSON.JSON_VAL_MEDIA_TYPE_IMAGES.value -> FilterMediaType.IMAGES
            TimelinePreferencesJSON.JSON_VAL_MEDIA_TYPE_VIDEOS.value -> FilterMediaType.VIDEOS
            else -> FilterMediaType.DEFAULT
        }

    private fun String?.toFilterMediaSource(): FilterMediaSource =
        when (this) {
            TimelinePreferencesJSON.JSON_VAL_LOCATION_ALL_LOCATION.value -> FilterMediaSource.AllPhotos
            TimelinePreferencesJSON.JSON_VAL_LOCATION_CLOUD_DRIVE.value -> FilterMediaSource.CloudDrive
            TimelinePreferencesJSON.JSON_VAL_LOCATION_CAMERA_UPLOAD.value -> FilterMediaSource.CameraUpload
            else -> FilterMediaSource.AllPhotos
        }
}
