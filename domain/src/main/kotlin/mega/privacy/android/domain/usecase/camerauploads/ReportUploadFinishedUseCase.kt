package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.entity.camerauploads.HeartbeatStatus
import javax.inject.Inject

/**
 * Report Uploads Status Finished Backup Heart Beat Use Case
 * @param reportUploadStatusUseCase [ReportUploadStatusUseCase]
 */
class ReportUploadFinishedUseCase @Inject constructor(
    private val reportUploadStatusUseCase: ReportUploadStatusUseCase,
) {

    /**
     * Invocation function
     * @param lastPrimaryNodeHandle     Last primary node handle to be synced
     * @param lastSecondaryNodeHandle   Last secondary node handle to be synced
     * @param lastPrimaryTimestamp      Last primary timestamp to by synced
     * @param lastSecondaryTimestamp    Last secondary timestamp to be synced
     */
    suspend operator fun invoke(
        lastPrimaryNodeHandle: Long,
        lastSecondaryNodeHandle: Long,
        lastPrimaryTimestamp: Long,
        lastSecondaryTimestamp: Long,
    ) {
        reportUploadStatusUseCase(
            cameraUploadFolderType = CameraUploadFolderType.Primary,
            heartbeatStatus = HeartbeatStatus.UP_TO_DATE,
            pendingUploads = 0,
            lastNodeHandle = lastPrimaryNodeHandle,
            lastTimestamp = lastPrimaryTimestamp,
        )
        reportUploadStatusUseCase(
            cameraUploadFolderType = CameraUploadFolderType.Secondary,
            heartbeatStatus = HeartbeatStatus.UP_TO_DATE,
            pendingUploads = 0,
            lastNodeHandle = lastSecondaryNodeHandle,
            lastTimestamp = lastSecondaryTimestamp,
        )
    }
}
