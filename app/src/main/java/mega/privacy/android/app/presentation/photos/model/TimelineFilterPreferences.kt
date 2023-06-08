package mega.privacy.android.app.presentation.photos.model

import mega.privacy.android.app.presentation.photos.timeline.model.TimelinePhotosSource
import mega.privacy.android.domain.entity.photos.TimelinePreferencesJSON

/**
 * Sealed interface for the Timeline Filter Preference
 */
sealed interface TimelineFilterPreferences

/**
 * Remember Timeline Filter Preferences
 *
 * @property value
 */
data class RememberPreferences(val value: Boolean) : TimelineFilterPreferences

/**
 * Location Preference
 *
 * @property value
 * @property stringValue
 */
data class LocationPreference(
    val value: TimelinePhotosSource,
    val stringValue: String = when (value) {
        TimelinePhotosSource.ALL_PHOTOS -> TimelinePreferencesJSON.JSON_VAL_LOCATION_ALL_LOCATION.value
        TimelinePhotosSource.CLOUD_DRIVE -> TimelinePreferencesJSON.JSON_VAL_LOCATION_CLOUD_DRIVE.value
        TimelinePhotosSource.CAMERA_UPLOAD -> TimelinePreferencesJSON.JSON_VAL_LOCATION_CAMERA_UPLOAD.value
    }
) : TimelineFilterPreferences

/**
 * Media Type preference
 *
 * @property value
 * @property stringValue
 */
data class MediaTypePreference(
    val value: FilterMediaType,
    val stringValue: String = when (value) {
        FilterMediaType.ALL_MEDIA -> TimelinePreferencesJSON.JSON_VAL_MEDIA_TYPE_ALL_MEDIA.value
        FilterMediaType.IMAGES -> TimelinePreferencesJSON.JSON_VAL_MEDIA_TYPE_IMAGES.value
        FilterMediaType.VIDEOS -> TimelinePreferencesJSON.JSON_VAL_MEDIA_TYPE_VIDEOS.value
    }
) : TimelineFilterPreferences
