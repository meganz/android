package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.entity.camerauploads.HeartbeatStatus
import javax.inject.Inject

/**
 * Report Uploads Status Interrupted Backup Heart Beat Use Case
 * @param reportUploadStatusUseCase [ReportUploadStatusUseCase]
 */
class ReportUploadInterruptedUseCase @Inject constructor(
    private val reportUploadStatusUseCase: ReportUploadStatusUseCase,
) {

    /**
     * Invocation function
     * @param pendingPrimaryUploads     Pending primary uploads
     * @param pendingSecondaryUploads   Pending secondary uploads
     * @param lastPrimaryNodeHandle     Last primary node handle to be synced
     * @param lastSecondaryNodeHandle   Last secondary node handle to be synced
     */
    suspend operator fun invoke(
        pendingPrimaryUploads: Int,
        pendingSecondaryUploads: Int,
        lastPrimaryNodeHandle: Long,
        lastSecondaryNodeHandle: Long,
        lastPrimaryTimestamp: Long,
        lastSecondaryTimestamp: Long
    ) {
        reportUploadStatusUseCase(
            cameraUploadFolderType = CameraUploadFolderType.Primary,
            heartbeatStatus = HeartbeatStatus.INACTIVE,
            pendingUploads = pendingPrimaryUploads,
            lastNodeHandle = lastPrimaryNodeHandle,
            lastTimestamp = lastPrimaryTimestamp,
        )
        reportUploadStatusUseCase(
            cameraUploadFolderType = CameraUploadFolderType.Secondary,
            heartbeatStatus = HeartbeatStatus.INACTIVE,
            pendingUploads = pendingSecondaryUploads,
            lastNodeHandle = lastSecondaryNodeHandle,
            lastTimestamp = lastSecondaryTimestamp,
        )
    }
}
