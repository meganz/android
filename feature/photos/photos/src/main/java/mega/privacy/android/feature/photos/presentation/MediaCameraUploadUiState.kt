package mega.privacy.android.feature.photos.presentation

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsStatusInfo

typealias TotalUploaded = Int

data class MediaCameraUploadUiState(
    val status: CUStatusUiState = CUStatusUiState.None,
    val uploadComplete: StateEventWithContent<TotalUploaded> = consumed(),
    val enableCameraUploadPageShowing: Boolean = false,
    /**
     * Counts the taps on the warning status icon. The timeline scrolls to its header, which holds
     * the banner that explains the current warning, each time this count increases.
     *
     * This is a count and not a one-shot event because the timeline recomposes many times while
     * the user scrolls the list. A one-shot event that is raised again before the previous event
     * is consumed stops the icon from responding for the rest of the session.
     */
    val revealWarningBannerRequest: Int = 0,
    /**
     * True after the user closes the warning banner, for as long as the Media screen exists. The
     * banner is then hidden, but the status icon remains in the toolbar, and a tap on the icon
     * shows the banner again. A close therefore hides the banner for now, not permanently.
     *
     * A close applies to every [CUStatusUiState.Warning] and not only to the warning that the
     * user saw. If the reason changes, for example from a low battery to a full storage, the new
     * banner also stays hidden until the user taps the icon. This matches the behaviour before
     * the regression that this fix repairs.
     */
    val isWarningBannerDismissed: Boolean = false,
)

sealed interface CUStatusUiState {

    data object None : CUStatusUiState

    data class Disabled(val shouldNotifyUser: Boolean = false) : CUStatusUiState

    data object Sync : CUStatusUiState

    data class UploadInProgress(
        val progress: Float,
        val pending: Int,
    ) : CUStatusUiState

    data class UploadComplete(val totalUploaded: Int) : CUStatusUiState

    data object UpToDate : CUStatusUiState

    data object Pause : CUStatusUiState

    sealed interface Warning : CUStatusUiState {

        data object AccountStorageOverQuota : Warning

        data object DeviceChargingRequirementNotMet : Warning

        data object BatteryLevelTooLow : Warning

        data object NetworkConnectionRequirementNotMet : Warning

        data object HasLimitedAccess : Warning

        data object NotificationNotGranted : Warning
    }
}

enum class MediaCUPermissionsState {
    Granted,
    Denied,
    Unknown
}

data class CUStatusFlowTransition(
    val permissionsState: MediaCUPermissionsState,
    val notificationPermissionGranted: Boolean?,
    val previousStatus: CameraUploadsStatusInfo?,
    val currentStatus: CameraUploadsStatusInfo?,
)
