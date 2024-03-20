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
 * @property requestPermissions State Event that triggers a request to grant Camera Uploads
 * permissions
 * @property shouldKeepUploadFileNames true if the content being uploaded should retain their filenames
 * @property uploadConnectionType Determines the connection type for uploading content in Camera Uploads
 * @property uploadOptionUiItem Determines the type of content that Camera Uploads can upload
 * @property videoQualityUiItem Determines the Video Quality of Videos being uploaded by Camera Uploads
 */
internal data class SettingsCameraUploadsUiState(
    val businessAccountPromptType: EnableCameraUploadsStatus? = null,
    val isCameraUploadsEnabled: Boolean = false,
    val isMediaUploadsEnabled: Boolean = false,
    val requestPermissions: StateEvent = consumed,
    val shouldKeepUploadFileNames: Boolean = false,
    val uploadConnectionType: UploadConnectionType = UploadConnectionType.WIFI,
    val uploadOptionUiItem: UploadOptionUiItem = UploadOptionUiItem.PhotosOnly,
    val videoQualityUiItem: VideoQualityUiItem = VideoQualityUiItem.Original,
)
