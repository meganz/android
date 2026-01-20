package mega.privacy.android.feature.photos.presentation

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsStatusInfo

typealias TotalUploaded = Int

data class MediaCameraUploadUiState(
    val status: CUStatusUiState = CUStatusUiState.None,
    val uploadComplete: StateEventWithContent<TotalUploaded> = consumed(),
    val enableCameraUploadPageShowing: Boolean = false,
    val cameraUploadsMessage: String = "",
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
    }
}

enum class MediaCUPermissionsState {
    Granted,
    Denied,
    Unknown
}

data class CUStatusFlowTransition(
    val permissionsState: MediaCUPermissionsState,
    val previousStatus: CameraUploadsStatusInfo?,
    val currentStatus: CameraUploadsStatusInfo?,
)
