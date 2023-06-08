package mega.privacy.android.app.presentation.mapper

import mega.privacy.android.app.presentation.photos.model.FilterMediaType
import mega.privacy.android.app.presentation.photos.model.LocationPreference
import mega.privacy.android.app.presentation.photos.model.MediaTypePreference
import mega.privacy.android.app.presentation.photos.model.RememberPreferences
import mega.privacy.android.app.presentation.photos.timeline.model.TimelinePhotosSource
import mega.privacy.android.domain.entity.photos.TimelinePreferencesJSON
import javax.inject.Inject

class TimelinePreferencesMapper @Inject constructor() {

    operator fun invoke(preferences: Map<String, String?>) = mapOf(
        Pair(
            TimelinePreferencesJSON.JSON_KEY_REMEMBER_PREFERENCES.value,
            RememberPreferences(
                value = preferences[TimelinePreferencesJSON.JSON_KEY_REMEMBER_PREFERENCES.value].toBoolean()
            )
        ),
        Pair(
            TimelinePreferencesJSON.JSON_KEY_MEDIA_TYPE.value,
            MediaTypePreference(
                value = mapMediaTypeToPreferenceState(
                    preferences[TimelinePreferencesJSON.JSON_KEY_MEDIA_TYPE.value]
                )
            )
        ),
        Pair(
            TimelinePreferencesJSON.JSON_KEY_LOCATION.value,
            LocationPreference(
                value = mapLocationToPreferenceState(
                    preferences[TimelinePreferencesJSON.JSON_KEY_LOCATION.value]
                )
            )
        ),
    )

    private fun mapMediaTypeToPreferenceState(mediaTypePreference: String?): FilterMediaType =
        when (mediaTypePreference) {
            TimelinePreferencesJSON.JSON_VAL_MEDIA_TYPE_ALL_MEDIA.value -> FilterMediaType.ALL_MEDIA
            TimelinePreferencesJSON.JSON_VAL_MEDIA_TYPE_IMAGES.value -> FilterMediaType.IMAGES
            TimelinePreferencesJSON.JSON_VAL_MEDIA_TYPE_VIDEOS.value -> FilterMediaType.VIDEOS
            else -> FilterMediaType.DEFAULT
        }

    private fun mapLocationToPreferenceState(locationPreference: String?): TimelinePhotosSource =
        when (locationPreference) {
            TimelinePreferencesJSON.JSON_VAL_LOCATION_ALL_LOCATION.value -> TimelinePhotosSource.ALL_PHOTOS
            TimelinePreferencesJSON.JSON_VAL_LOCATION_CLOUD_DRIVE.value -> TimelinePhotosSource.CLOUD_DRIVE
            TimelinePreferencesJSON.JSON_VAL_LOCATION_CAMERA_UPLOAD.value -> TimelinePhotosSource.CAMERA_UPLOAD
            else -> TimelinePhotosSource.DEFAULT
        }

    internal fun mapMediaTypeToString(mediaTypePreference: FilterMediaType): String =
        when (mediaTypePreference) {
            FilterMediaType.ALL_MEDIA -> TimelinePreferencesJSON.JSON_VAL_MEDIA_TYPE_ALL_MEDIA.value
            FilterMediaType.IMAGES -> TimelinePreferencesJSON.JSON_VAL_MEDIA_TYPE_IMAGES.value
            FilterMediaType.VIDEOS -> TimelinePreferencesJSON.JSON_VAL_MEDIA_TYPE_VIDEOS.value
        }

    internal fun mapLocationToString(locationPreference: TimelinePhotosSource): String =
        when (locationPreference) {
            TimelinePhotosSource.ALL_PHOTOS -> TimelinePreferencesJSON.JSON_VAL_LOCATION_ALL_LOCATION.value
            TimelinePhotosSource.CLOUD_DRIVE -> TimelinePreferencesJSON.JSON_VAL_LOCATION_CLOUD_DRIVE.value
            TimelinePhotosSource.CAMERA_UPLOAD -> TimelinePreferencesJSON.JSON_VAL_LOCATION_CAMERA_UPLOAD.value
        }
}