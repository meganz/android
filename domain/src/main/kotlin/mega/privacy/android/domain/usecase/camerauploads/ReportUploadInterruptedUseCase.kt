package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.entity.camerauploads.HeartbeatStatus
import javax.inject.Inject

/**
 * Report Uploads Status Interrupted Backup Heart Beat Use Case
 * @param reportUploadStatusUseCase [ReportUploadStatusUseCase]
 */
class ReportUploadInterruptedUseCase @Inject constructor(private val reportUploadStatusUseCase: ReportUploadStatusUseCase) {

    /**
     * Invocation function
     * @param pendingPrimaryUploads     Pending primary uploads
     * @param pendingSecondaryUploads   Pending secondary uploads
     * @param lastPrimaryNodeHandle     Last primary node handle to be synced
     * @param lastSecondaryNodeHandle   Last secondary node handle to be synced
     * @param updatePrimaryTimeStamp    Update primary time stamp in camera uploads worker
     * @param updateSecondaryTimeStamp  Update secondary time stamp in camera uploads worker
     */
    suspend operator fun invoke(
        pendingPrimaryUploads: Int,
        pendingSecondaryUploads: Int,
        lastPrimaryNodeHandle: Long,
        lastSecondaryNodeHandle: Long,
        updatePrimaryTimeStamp: () -> Long,
        updateSecondaryTimeStamp: () -> Long,
    ) {
        reportUploadStatusUseCase(
            cameraUploadFolderType = CameraUploadFolderType.Primary,
            heartbeatStatus = HeartbeatStatus.INACTIVE,
            pendingUploads = pendingPrimaryUploads,
            lastNodeHandle = lastPrimaryNodeHandle,
            updateTimeStamp = updatePrimaryTimeStamp
        )
        reportUploadStatusUseCase(
            cameraUploadFolderType = CameraUploadFolderType.Secondary,
            heartbeatStatus = HeartbeatStatus.INACTIVE,
            pendingUploads = pendingSecondaryUploads,
            lastNodeHandle = lastSecondaryNodeHandle,
            updateTimeStamp = updateSecondaryTimeStamp
        )
    }
}
