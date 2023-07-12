package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsState
import mega.privacy.android.domain.entity.camerauploads.HeartbeatStatus
import mega.privacy.android.domain.usecase.IsSecondaryFolderEnabled
import javax.inject.Inject

/**
 * Update backup heartbeat status
 *
 * @param reportUploadStatusUseCase [ReportUploadStatusUseCase]
 */
class UpdateCameraUploadsBackupHeartbeatStatusUseCase @Inject constructor(
    private val isCameraUploadsEnabledUseCase: IsCameraUploadsEnabledUseCase,
    private val isSecondaryFolderEnabled: IsSecondaryFolderEnabled,
    private val reportUploadStatusUseCase: ReportUploadStatusUseCase,
) {

    /**
     * Update backup heartbeat status
     *
     * @param heartbeatStatus the heartbeat status to send
     * @param cameraUploadsState the current state of Camera Uploads
     */
    suspend operator fun invoke(
        heartbeatStatus: HeartbeatStatus,
        cameraUploadsState: CameraUploadsState,
    ) {
        if (isCameraUploadsEnabledUseCase()) {
            with(cameraUploadsState.primaryCameraUploadsState) {
                reportUploadStatusUseCase(
                    cameraUploadFolderType = CameraUploadFolderType.Primary,
                    heartbeatStatus = heartbeatStatus,
                    pendingUploads = pendingCount,
                    lastNodeHandle = lastHandle,
                    lastTimestamp = lastTimestamp,
                )
            }

            if (isSecondaryFolderEnabled()) {
                with(cameraUploadsState.secondaryCameraUploadsState) {
                    reportUploadStatusUseCase(
                        cameraUploadFolderType = CameraUploadFolderType.Secondary,
                        heartbeatStatus = heartbeatStatus,
                        pendingUploads = pendingCount,
                        lastNodeHandle = lastHandle,
                        lastTimestamp = lastTimestamp,
                    )
                }
            }
        }
    }
}
