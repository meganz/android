package mega.privacy.android.app.presentation.settings.camerauploads.model

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed

/**
 * The UI State class for Settings Camera Uploads
 *
 * @property isCameraUploadsEnabled true if Camera Uploads is enabled
 * @property isMediaUploadsEnabled true if Media Uploads is enabled
 * @property requestPermissions State Event that triggers a request to grant Camera Uploads
 * permissions
 * @property showBusinessAccountPrompt true if a prompt should be shown informing the User is under
 * a Business Account
 * @property showBusinessAccountSubUserSuspendedPrompt true if a prompt should be shown that the
 * Business Account Sub-User has been suspended
 * @property showBusinessAccountAdministratorSuspendedPrompt true if a prompt should be shown that
 * the Business Account Administrator has been suspended
 * @property showMediaPermissionsRationale true if a Rationale explaining why the Media Permissions
 * need to be granted should be shown
 * @property uploadConnectionType Determines the connection type for uploading content in Camera Uploads
 */
internal data class SettingsCameraUploadsUiState(
    val isCameraUploadsEnabled: Boolean = false,
    val isMediaUploadsEnabled: Boolean = false,
    val requestPermissions: StateEvent = consumed,
    val showBusinessAccountPrompt: Boolean = false,
    val showBusinessAccountSubUserSuspendedPrompt: Boolean = false,
    val showBusinessAccountAdministratorSuspendedPrompt: Boolean = false,
    val showMediaPermissionsRationale: Boolean = false,
    val uploadConnectionType: UploadConnectionType = UploadConnectionType.WIFI,
)
