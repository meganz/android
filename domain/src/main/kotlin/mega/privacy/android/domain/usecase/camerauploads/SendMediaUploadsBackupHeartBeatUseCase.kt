package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.camerauploads.HeartbeatStatus
import mega.privacy.android.domain.repository.CameraUploadsRepository
import java.time.Instant
import javax.inject.Inject

/**
 * Send Media Uploads Backup Heart Beat Use Case
 * @param cameraUploadsRepository [CameraUploadsRepository]
 */
class SendMediaUploadsBackupHeartBeatUseCase @Inject constructor(private val cameraUploadsRepository: CameraUploadsRepository) {

    /**
     * Invocation function
     * @param heartbeatStatus   Heartbeat status
     * @param lastNodeHandle    Last node handle to be synced
     */
    suspend operator fun invoke(
        heartbeatStatus: HeartbeatStatus,
        lastNodeHandle: Long,
    ) {
        if (cameraUploadsRepository.isMediaUploadsEnabled() == true) {
            cameraUploadsRepository.getMuBackUpId()?.let { backupId ->
                // ups (uploads) and downs (downloads) are set to -1 because they should not be changed on the server side
                // when sending this heartbeat. This behavior is consistent with SDK/API conventions where -1 indicates "no change".
                cameraUploadsRepository.sendBackupHeartbeat(
                    backupId = backupId,
                    heartbeatStatus = heartbeatStatus,
                    ups = -1,
                    downs = -1,
                    ts = Instant.now().epochSecond,
                    lastNode = lastNodeHandle,
                )
            }
        }
    }
}
