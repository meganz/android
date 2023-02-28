package mega.privacy.android.app.presentation.settings.camerauploads.model

import mega.privacy.android.domain.entity.settings.camerauploads.UploadOption

/**
 * Data class representing the state of Camera Uploads in Settings
 *
 * @property isCameraUploadsRunning Checks whether Camera Uploads is running or not
 * @property shouldShowBusinessAccountPrompt Checks whether the Dialog indicating that the account is a Business Account should be shown or not
 * @property shouldShowBusinessAccountSuspendedPrompt Checks whether the Dialog indicating that the account is a suspended Business account should be shown or not
 * @property shouldTriggerCameraUploads Checks whether the Camera Uploads functionality in Settings should be triggered or not
 * @property shouldShowMediaPermissionsRationale Checks whether the Media Permissions Rationale should be shown or not
 * @property shouldShowNotificationPermissionRationale Checks whether the Notification Permission Rationale should be shown or not
 * @property uploadConnectionType Determines the connection type for uploading content in Camera Uploads
 * @property uploadOption Determines what content should be uploaded
 */
data class SettingsCameraUploadsState(
    val isCameraUploadsRunning: Boolean = false,
    val shouldShowBusinessAccountPrompt: Boolean = false,
    val shouldShowBusinessAccountSuspendedPrompt: Boolean = false,
    val shouldTriggerCameraUploads: Boolean = false,
    val shouldShowMediaPermissionsRationale: Boolean = false,
    val shouldShowNotificationPermissionRationale: Boolean = false,
    val uploadConnectionType: UploadConnectionType? = null,
    val uploadOption: UploadOption? = null,
)