package mega.privacy.android.feature.photos.presentation

import mega.privacy.android.domain.entity.camerauploads.CameraUploadsFinishedReason
import mega.privacy.android.feature.photos.model.CameraUploadsStatus

/**
 * Due to the time constraint, this class contains a copy of CU-related properties from
 * TimelineViewState.
 */
data class MediaCameraUploadUiState(
    val cameraUploadsStatus: CameraUploadsStatus = CameraUploadsStatus.None,
    val showCameraUploadsComplete: Boolean = false,
    val showCameraUploadsWarning: Boolean = false,
    val pending: Int = 0,
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
