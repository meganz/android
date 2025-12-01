package mega.privacy.android.feature.photos.model

import androidx.annotation.StringRes
import mega.privacy.android.domain.entity.photos.TimelinePreferencesJSON
import mega.privacy.android.shared.resources.R as sharedR

enum class FilterMediaSource(@StringRes val nameResId: Int) {
    /**
     * All Cloud drive images + videos in Camera upload and media upload folders
     */
    AllPhotos(nameResId = sharedR.string.video_section_videos_location_option_all_locations),

    /**
     *  Cloud drive without CAMERA_UPLOAD
     */
    CloudDrive(nameResId = sharedR.string.video_section_videos_location_option_cloud_drive),

    /**
     * Camera upload and media upload folders
     */
    CameraUpload(nameResId = sharedR.string.video_section_videos_location_option_camera_uploads);

    companion object {
        fun FilterMediaSource.toLocationValue() = when (this) {
            AllPhotos -> TimelinePreferencesJSON.JSON_VAL_LOCATION_ALL_LOCATION.value
            CloudDrive -> TimelinePreferencesJSON.JSON_VAL_LOCATION_CLOUD_DRIVE.value
            CameraUpload -> TimelinePreferencesJSON.JSON_VAL_LOCATION_CAMERA_UPLOAD.value
        }
    }
}
