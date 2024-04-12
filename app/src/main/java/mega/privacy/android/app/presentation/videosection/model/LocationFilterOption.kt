package mega.privacy.android.app.presentation.videosection.model

import mega.privacy.android.shared.resources.R

/**
 * Enum class to represent the location filter option.
 *
 * @param titleResId The title resource id of the filter option.
 */
enum class LocationFilterOption(val titleResId: Int) {

    /**
     * All locations filter option.
     */
    AllLocations(R.string.video_section_videos_location_option_all_locations),

    /**
     * Cloud drive filter option.
     */
    CloudDrive(R.string.video_section_videos_location_option_cloud_drive),

    /**
     * Camera uploads filter option.
     */
    CameraUploads(R.string.video_section_videos_location_option_camera_uploads),

    /**
     * Shared items filter option.
     */
    SharedItems(R.string.video_section_videos_location_option_shared_items),
}