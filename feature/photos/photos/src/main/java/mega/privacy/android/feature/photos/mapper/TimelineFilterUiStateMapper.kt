package mega.privacy.android.feature.photos.mapper

import mega.privacy.android.domain.entity.photos.TimelinePreferencesJSON
import mega.privacy.android.feature.photos.model.FilterMediaSource
import mega.privacy.android.feature.photos.model.FilterMediaType
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
