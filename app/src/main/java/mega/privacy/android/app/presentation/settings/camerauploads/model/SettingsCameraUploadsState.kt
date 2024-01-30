package mega.privacy.android.app.presentation.settings.camerauploads.model

import androidx.annotation.StringRes
import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.entity.settings.camerauploads.UploadOption

/**
 * Data class representing the state of Camera Uploads in Settings
 *
 * @property accessMediaLocationRationaleText Displays the Access Media Location rationale with the message
 * @property areUploadFileNamesKept Checks whether the File Names are kept or not when uploading content
 * @property areLocationTagsIncluded When uploading Photos, this checks whether Location Tags should be embedded in each Photo or not
 * @property isCameraUploadsEnabled Checks whether Camera Uploads is enabled or not
 * @property isMediaUploadsEnabled Checks whether Media Upload is enabled or not
 * @property isChargingRequiredForVideoCompression Checks whether compressing videos require the device to be charged or not
 * @property invalidFolderSelectedTextId Displays the message when an invalid Folder is selected
 * @property primaryUploadSyncHandle Primary Upload Sync Handle
 * @property primaryFolderPath The Primary Folder path
 * @property primaryFolderName Name of the Camera Uploads Folder
 * @property secondaryUploadSyncHandle Secondary Upload Sync Handle
 * @property secondaryFolderName Name of the Camera Uploads Folder
 * @property secondaryFolderPath The Secondary Folder path
 * @property shouldShowBusinessAccountPrompt Checks whether the Dialog indicating that the account is a Business Account should be shown or not
 * @property shouldShowMediaPermissionsRationale Checks whether the Media Permissions Rationale should be shown or not
 * @property uploadConnectionType Determines the connection type for uploading content in Camera Uploads
 * @property uploadOption Determines what content should be uploaded
 * @property videoCompressionSizeLimit The maximum video file size that can be compressed
 * @property showNewVideoCompressionSizePrompt Checks whether the new Video Compression Size prompt
 * should be shown or not
 * @property clearNewVideoCompressionSizeInput If true, this signals to reset the inputted New Video
 * Compression Size in the prompt from [showNewVideoCompressionSizePrompt]
 * @property videoQuality Determines the Video Quality of videos to be uploaded
 * @property shouldShowError Should display an error
 * @property shouldTriggerPermissionDialog Checks whether the Permissions Dialog should be shown or not
 */
data class SettingsCameraUploadsState(
    @StringRes val accessMediaLocationRationaleText: Int? = null,
    val areLocationTagsIncluded: Boolean = false,
    val areUploadFileNamesKept: Boolean = false,
    val isCameraUploadsEnabled: Boolean = false,
    val isMediaUploadsEnabled: Boolean = false,
    val isChargingRequiredForVideoCompression: Boolean = false,
    @StringRes val invalidFolderSelectedTextId: Int? = null,
    val primaryUploadSyncHandle: Long? = null,
    val primaryFolderPath: String = "",
    val primaryFolderName: String = "",
    val secondaryUploadSyncHandle: Long? = null,
    val secondaryFolderName: String = "",
    val secondaryFolderPath: String = "",
    val shouldShowBusinessAccountPrompt: Boolean = false,
    val shouldShowMediaPermissionsRationale: Boolean = false,
    val uploadConnectionType: UploadConnectionType? = null,
    val uploadOption: UploadOption? = null,
    val videoCompressionSizeLimit: Int = 0,
    val showNewVideoCompressionSizePrompt: Boolean = false,
    val clearNewVideoCompressionSizeInput: Boolean = false,
    val videoQuality: VideoQuality? = null,
    val shouldShowError: Boolean = false,
    val shouldTriggerPermissionDialog: Boolean = false,
)
