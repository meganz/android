package mega.privacy.android.feature.photos.mapper

import mega.privacy.android.domain.entity.photos.TimelinePreferencesJSON
import mega.privacy.android.feature.photos.model.FilterMediaSource
import mega.privacy.android.feature.photos.model.FilterMediaType
import mega.privacy.android.feature.photos.presentation.MediaFilterUiState
import javax.inject.Inject

class MediaFilterUiStateMapper @Inject constructor() {

    operator fun invoke(preferenceMap: Map<String, String?>?): MediaFilterUiState {
        if (preferenceMap == null) return MediaFilterUiState()

        val arePreferencesRemembered =
            preferenceMap[TimelinePreferencesJSON.JSON_KEY_REMEMBER_PREFERENCES.value].toBoolean()
        return if (arePreferencesRemembered) {
            val mediaType = preferenceMap[TimelinePreferencesJSON.JSON_KEY_MEDIA_TYPE.value]
                ?: TimelinePreferencesJSON.JSON_VAL_MEDIA_TYPE_ALL_MEDIA.value
            val mediaSource = preferenceMap[TimelinePreferencesJSON.JSON_KEY_LOCATION.value]
                ?: TimelinePreferencesJSON.JSON_VAL_LOCATION_ALL_LOCATION.value
            MediaFilterUiState(
                isRemembered = true,
                mediaType = mediaType.toFilterMediaType(),
                mediaSource = mediaSource.toFilterMediaSource()
            )
        } else MediaFilterUiState()
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
