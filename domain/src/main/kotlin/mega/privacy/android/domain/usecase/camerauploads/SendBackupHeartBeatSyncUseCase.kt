package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import mega.privacy.android.domain.entity.BackupState
import mega.privacy.android.domain.entity.backup.Backup
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsState
import mega.privacy.android.domain.repository.CameraUploadRepository
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * While camera upload process is running, send heartbeat every 30 seconds
 */
private const val ACTIVE_HEARTBEAT_INTERVAL_SECONDS = 30L

/**
 * Send Backup Heart Beat Use Case when camera uploads process is ongoing
 * @param cameraUploadRepository [CameraUploadRepository]
 */
class SendBackupHeartBeatSyncUseCase @Inject constructor(private val cameraUploadRepository: CameraUploadRepository) {

    /**
     * Invocation function
     * @param cameraUploadsState    current state of camera uploads process
     */
    operator fun invoke(
        cameraUploadsState: CameraUploadsState,
    ) = flow {
        while (true) {
            emit(Unit)
            cameraUploadRepository.getCuBackUp()?.let {
                sendCameraUploadsHeartbeatIfNeeded(
                    cameraUploadsState = cameraUploadsState,
                    backup = it,
                )
            }

            cameraUploadRepository.getMuBackUp()?.let {
                sendMediaUploadsHeartbeatIfNeeded(
                    cameraUploadsState = cameraUploadsState,
                    backup = it,
                )
            }
            delay(TimeUnit.SECONDS.toMillis(ACTIVE_HEARTBEAT_INTERVAL_SECONDS))
        }
    }

    private suspend fun sendCameraUploadsHeartbeatIfNeeded(
        cameraUploadsState: CameraUploadsState,
        backup: Backup,
    ) {
        with(cameraUploadsState) {
            if (shouldSendCameraUploadsHeartbeat(cameraUploadsState, backup)) {
                cameraUploadRepository.sendBackupHeartbeatSync(
                    backupId = backup.backupId,
                    progress = (primaryTotalUploadedBytes / primaryTotalUploadBytes.toFloat() * 100).toInt(),
                    ups = primaryPendingUploads,
                    downs = 0,
                    timeStamp = lastPrimaryTimeStamp,
                    lastNode = lastPrimaryHandle,
                )
            }
        }
    }

    private suspend fun sendMediaUploadsHeartbeatIfNeeded(
        cameraUploadsState: CameraUploadsState,
        backup: Backup,
    ) {
        with(cameraUploadsState) {
            if (shouldSendMediaUploadsHeartbeat(cameraUploadsState, backup)) {
                cameraUploadRepository.sendBackupHeartbeatSync(
                    backupId = backup.backupId,
                    progress = (secondaryTotalUploadedBytes / secondaryTotalUploadBytes.toFloat() * 100).toInt(),
                    ups = secondaryPendingUploads,
                    downs = 0,
                    timeStamp = lastSecondaryTimeStamp,
                    lastNode = lastSecondaryHandle,
                )
            }
        }
    }

    private suspend fun shouldSendCameraUploadsHeartbeat(
        cameraUploadsState: CameraUploadsState,
        backup: Backup,
    ) = cameraUploadRepository.isCameraUploadsEnabled() &&
            cameraUploadsState.primaryTotalUploadBytes != 0L &&
            backup.state != BackupState.PAUSE_UPLOADS

    private suspend fun shouldSendMediaUploadsHeartbeat(
        cameraUploadsState: CameraUploadsState,
        backup: Backup,
    ) = cameraUploadRepository.isSecondaryMediaFolderEnabled() &&
            cameraUploadsState.secondaryTotalUploadBytes != 0L &&
            backup.state != BackupState.PAUSE_UPLOADS
}
