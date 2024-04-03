package mega.privacy.android.app.presentation.settings.camerauploads.model

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed
import mega.privacy.android.domain.entity.account.EnableCameraUploadsStatus

/**
 * The UI State class for Settings Camera Uploads
 *
 * @property businessAccountPromptType The type of prompt to be shown when a Business Account User
 * attempts to enable Camera Uploads
 * @property isCameraUploadsEnabled true if Camera Uploads is enabled
 * @property isMediaUploadsEnabled true if Media Uploads is enabled
 * @property maximumNonChargingVideoCompressionSize The maximum aggregated Video Size that can be
 * compressed without having to charge the Device
 * @property primaryFolderName The Camera Uploads Cloud Drive Folder name, which can be null
 * @property primaryFolderPath The Camera Uploads Local Primary Folder Path
 * @property requestPermissions State Event that triggers a request to grant Camera Uploads
 * permissions
 * @property requireChargingDuringVideoCompression, If true, the Device needs to be charged when
 * compressing Videos
 * @property secondaryFolderPath The Media Uploads Local Secondary Folder Path
 * @property shouldIncludeLocationTags If true, Location Tags are added when uploading Photos through
 * Camera Uploads
 * @property shouldKeepUploadFileNames true if the content being uploaded should retain their filenames
 * @property uploadConnectionType Determines the connection type for uploading content in Camera Uploads
 * @property uploadOptionUiItem Determines the type of content that Camera Uploads can upload
 * @property videoQualityUiItem Determines the Video Quality of Videos being uploaded by Camera Uploads
 */
internal data class SettingsCameraUploadsUiState(
    val businessAccountPromptType: EnableCameraUploadsStatus? = null,
    val isCameraUploadsEnabled: Boolean = false,
    val isMediaUploadsEnabled: Boolean = false,
    val maximumNonChargingVideoCompressionSize: Int = 200,
    val primaryFolderName: String? = null,
    val primaryFolderPath: String = "",
    val requestPermissions: StateEvent = consumed,
    val requireChargingDuringVideoCompression: Boolean = true,
    val secondaryFolderPath: String = "",
    val shouldIncludeLocationTags: Boolean = false,
    val shouldKeepUploadFileNames: Boolean = false,
    val uploadConnectionType: UploadConnectionType = UploadConnectionType.WIFI,
    val uploadOptionUiItem: UploadOptionUiItem = UploadOptionUiItem.PhotosOnly,
    val videoQualityUiItem: VideoQualityUiItem = VideoQualityUiItem.Original,
) {
    /**
     * Checks if the User can change the setting to include / exclude Location Tags when
     * uploading Photos
     */
    val canChangeLocationTagsState
        get() = uploadOptionUiItem != UploadOptionUiItem.VideosOnly

    /**
     * Checks if the User can change whether or not charging is required during Video Compression
     */
    val canChangeChargingDuringVideoCompressionState
        get() = uploadOptionUiItem != UploadOptionUiItem.PhotosOnly && videoQualityUiItem != VideoQualityUiItem.Original

    /**
     * Checks if the User can select what Video Quality to use when uploading Videos
     */
    val canChangeVideoQuality
        get() = uploadOptionUiItem != UploadOptionUiItem.PhotosOnly
}
