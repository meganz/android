package mega.privacy.android.feature.photos.presentation

import mega.privacy.android.domain.entity.camerauploads.CameraUploadsFinishedReason
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsStatusInfo

/**
 * Due to the time constraint, this class contains a copy of CU-related properties from
 * TimelineViewState.
 */
data class MediaCameraUploadUiState(
    val status: CUStatusUiState = CUStatusUiState.None,
    val cameraUploadsProgress: Float = 0f,
    val cameraUploadsTotalUploaded: Int = 0,
    val cameraUploadsFinishedReason: CameraUploadsFinishedReason? = null,
    val showCameraUploadsCompletedMessage: Boolean = false,
    val shouldShowCUBanner: Boolean = false,
    val enableCameraUploadPageShowing: Boolean = false,
    val cameraUploadsMessage: String = "",
)

sealed interface CUStatusUiState {

    data object None : CUStatusUiState

    data object Disabled : CUStatusUiState

    data object Sync : CUStatusUiState

    data class UploadInProgress(
        val progress: Float,
        val pending: Int,
    ) : CUStatusUiState

    data object UploadComplete : CUStatusUiState

    data object UpToDate : CUStatusUiState

    data object Pause : CUStatusUiState

    sealed interface Warning : CUStatusUiState {

        sealed interface BannerOnly : Warning {

            data object AccountStorageOverQuota : BannerOnly
        }

        sealed interface BannerAndAction : Warning {

            data object DeviceChargingRequirementNotMet : Warning

            data object BatteryLevelTooLow : Warning

            data object NetworkConnectionRequirementNotMet : Warning

            data object HasLimitedAccess : Warning
        }

        data object Unknown : Warning
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
