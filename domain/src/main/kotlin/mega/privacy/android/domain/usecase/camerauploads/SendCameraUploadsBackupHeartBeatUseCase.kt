package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.camerauploads.HeartbeatStatus
import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Send Camera Uploads Backup Heart Beat Use Case
 * @param cameraUploadRepository [CameraUploadRepository]
 */
class SendCameraUploadsBackupHeartBeatUseCase @Inject constructor(private val cameraUploadRepository: CameraUploadRepository) {

    /**
     * Invocation function
     * @param heartbeatStatus   Heartbeat status
     * @param lastNodeHandle    Last node handle to be synced
     */
    suspend operator fun invoke(
        heartbeatStatus: HeartbeatStatus,
        lastNodeHandle: Long,
    ) {
        if (cameraUploadRepository.isCameraUploadsEnabled() == true) {
            cameraUploadRepository.getCuBackUpId()?.let { backupId ->
                cameraUploadRepository.sendBackupHeartbeat(
                    backupId = backupId,
                    heartbeatStatus = heartbeatStatus,
                    ups = 0,
                    downs = 0,
                    ts = 0,
                    lastNode = lastNodeHandle,
                )
            }
        }
    }
}
