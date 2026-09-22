package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.entity.camerauploads.HeartbeatStatus
import mega.privacy.android.domain.repository.CameraUploadsRepository
import javax.inject.Inject

/**
 * Report Camera Uploads Status Backup Heart Beat Use Case
 * @param cameraUploadsRepository [CameraUploadsRepository]
 */
class ReportUploadStatusUseCase @Inject constructor(
    private val cameraUploadsRepository: CameraUploadsRepository,
) {

    /**
     * Invocation function
     * @param cameraUploadFolderType    Camera upload folder type
     * @param heartbeatStatus           Heartbeat status
     * @param pendingUploads            Pending uploads
     * @param lastNodeHandle            Last node handle to be synced
     * @param lastTimestamp             Last time stamp in camera uploads worker
     */
    suspend operator fun invoke(
        cameraUploadFolderType: CameraUploadFolderType,
        heartbeatStatus: HeartbeatStatus,
        pendingUploads: Int,
        lastNodeHandle: Long,
        lastTimestamp: Long,
    ) {
        when (cameraUploadFolderType) {
            CameraUploadFolderType.Primary -> cameraUploadsRepository.getCuBackUpId()
            CameraUploadFolderType.Secondary -> cameraUploadsRepository.getMuBackUpId()
        }?.let { backupId ->
            cameraUploadsRepository.sendBackupHeartbeat(
                backupId = backupId,
                heartbeatStatus = heartbeatStatus,
                ups = pendingUploads,
                downs = 0,
                ts = lastTimestamp,
                lastNode = lastNodeHandle,
            )
        }
    }
}
