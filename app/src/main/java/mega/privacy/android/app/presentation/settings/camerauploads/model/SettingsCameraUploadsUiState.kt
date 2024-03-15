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
 * @property showMediaPermissionsRationale true if a Rationale explaining why the Media Permissions
 * need to be granted should be shown
 * @property uploadConnectionType Determines the connection type for uploading content in Camera Uploads
 */
internal data class SettingsCameraUploadsUiState(
    val businessAccountPromptType: EnableCameraUploadsStatus? = null,
    val isCameraUploadsEnabled: Boolean = false,
    val isMediaUploadsEnabled: Boolean = false,
    val requestPermissions: StateEvent = consumed,
    val showMediaPermissionsRationale: Boolean = false,
    val uploadConnectionType: UploadConnectionType = UploadConnectionType.WIFI,
)
