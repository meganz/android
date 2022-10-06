package mega.privacy.android.app.presentation.settings.camerauploads.model

/**
 * Data class representing the state of Camera Uploads in Settings
 *
 * @param shouldShowBusinessAccountPrompt Checks whether the Dialog indicating that the account is a Business Account should be shown or not
 * @param shouldShowBusinessAccountSuspendedPrompt Checks whether the Dialog indicating that the account is a suspended Business account should be shown or not
 * @param shouldTriggerCameraUploads Checks whether the Camera Uploads functionality in Settings should be triggered or not
 */
data class SettingsCameraUploadsState(
    val shouldShowBusinessAccountPrompt: Boolean = false,
    val shouldShowBusinessAccountSuspendedPrompt: Boolean = false,
    val shouldTriggerCameraUploads: Boolean = false,
)