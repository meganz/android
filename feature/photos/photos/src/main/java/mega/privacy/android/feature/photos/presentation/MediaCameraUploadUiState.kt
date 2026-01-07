package mega.privacy.android.feature.photos.presentation

import mega.privacy.android.domain.entity.camerauploads.CameraUploadsFinishedReason

/**
 * Due to the time constraint, this class contains a copy of CU-related properties from
 * TimelineViewState.
 */
data class MediaCameraUploadUiState(
    val status: CUStatusUiState = CUStatusUiState.None,
    val showCameraUploadsComplete: Boolean = false,
    val showCameraUploadsWarning: Boolean = false,
    val cameraUploadsProgress: Float = 0f,
    val cameraUploadsTotalUploaded: Int = 0,
    val cameraUploadsFinishedReason: CameraUploadsFinishedReason? = null,
    val showCameraUploadsCompletedMessage: Boolean = false,
    val isWarningBannerShown: Boolean = false,
    val isCameraUploadsLimitedAccess: Boolean = false,
    val enableCameraUploadButtonShowing: Boolean = true,
    val enableCameraUploadPageShowing: Boolean = false,
    val cameraUploadsMessage: String = "",
    val shouldShowEnableCUBanner: Boolean = false,
)

sealed interface CUStatusUiState {

    data object None : CUStatusUiState

    data object Sync : CUStatusUiState

    data class UploadInProgress(
        val progress: Float,
        val pending: Int,
    ) : CUStatusUiState

    data object UploadComplete : CUStatusUiState

    data object UpToDate : CUStatusUiState

    data object Pause : CUStatusUiState

    data object Warning : CUStatusUiState
}
