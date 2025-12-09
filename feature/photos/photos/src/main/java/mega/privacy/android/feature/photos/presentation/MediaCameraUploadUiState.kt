package mega.privacy.android.feature.photos.presentation

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsFinishedReason
import mega.privacy.android.feature.photos.model.CameraUploadsStatus

/**
 * Due to the time constraint, this class contains a copy of CU-related properties from
 * TimelineViewState.
 */
data class MediaCameraUploadUiState(
    val isCameraUploadsEnabled: Boolean = false,
    val cameraUploadsStatus: CameraUploadsStatus = CameraUploadsStatus.None,
    val showCameraUploadsPaused: Boolean = false,
    val showCameraUploadsComplete: Boolean = false,
    val showCameraUploadsWarning: Boolean = false,
    val pending: Int = 0,
    val cameraUploadsProgress: Float = 0f,
    val cameraUploadsTotalUploaded: Int = 0,
    val cameraUploadsFinishedReason: CameraUploadsFinishedReason? = null,
    val showCameraUploadsCompletedMessage: Boolean = false,
    val isWarningBannerShown: Boolean = false,
    val isCUPausedWarningBannerEnabled: Boolean = false,
    val isCameraUploadsTransferScreenEnabled: Boolean = false,
    val isCameraUploadsLimitedAccess: Boolean = false,
    val shouldTriggerMediaPermissionsDeniedLogic: Boolean = false,
    val shouldTriggerCameraUploads: Boolean = false,
    val enableCameraUploadButtonShowing: Boolean = true,
    val enableCameraUploadPageShowing: Boolean = false,
    val cuUploadsVideos: Boolean = false,
    val cuUseCellularConnection: Boolean = false,
    val showCameraUploadsChangePermissionsMessage: Boolean = false,
    val cameraUploadsMessage: String = "",
    val popBackFromCameraUploadsTransferScreenEvent: StateEvent = consumed,
    val shouldShowEnableCUBanner: Boolean = false,
)
