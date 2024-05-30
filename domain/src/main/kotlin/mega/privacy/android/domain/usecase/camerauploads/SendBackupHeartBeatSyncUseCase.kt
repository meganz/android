package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import mega.privacy.android.domain.entity.BackupState
import mega.privacy.android.domain.entity.backup.Backup
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsFolderState
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsState
import mega.privacy.android.domain.repository.CameraUploadsRepository
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * While camera upload process is running, send heartbeat every 30 seconds
 */
private const val ACTIVE_HEARTBEAT_INTERVAL_SECONDS = 30L

/**
 * Send Backup Heart Beat Use Case when camera uploads process is ongoing
 * @param cameraUploadsRepository [CameraUploadsRepository]
 */
class SendBackupHeartBeatSyncUseCase @Inject constructor(
    private val cameraUploadsRepository: CameraUploadsRepository,
    private val isCameraUploadsEnabledUseCase: IsCameraUploadsEnabledUseCase,
    private val isMediaUploadsEnabledUseCase: IsMediaUploadsEnabledUseCase,
) {

    /**
     * Invocation function
     * @param cameraUploadsStateProvider    provide current state of camera uploads process
     */
    operator fun invoke(
        cameraUploadsStateProvider: () -> CameraUploadsState,
    ) = flow {
        while (true) {
            emit(Unit)
            cameraUploadsRepository.getCuBackUp()?.let {
                sendCameraUploadsHeartbeatIfNeeded(
                    cameraUploadsFolderState = cameraUploadsStateProvider().primaryCameraUploadsState,
                    backup = it,
                    cameraUploadsFolderType = CameraUploadFolderType.Primary
                )
            }

            cameraUploadsRepository.getMuBackUp()?.let {
                sendCameraUploadsHeartbeatIfNeeded(
                    cameraUploadsFolderState = cameraUploadsStateProvider().secondaryCameraUploadsState,
                    backup = it,
                    cameraUploadsFolderType = CameraUploadFolderType.Secondary
                )
            }
            delay(TimeUnit.SECONDS.toMillis(ACTIVE_HEARTBEAT_INTERVAL_SECONDS))
        }
    }

    private suspend fun sendCameraUploadsHeartbeatIfNeeded(
        cameraUploadsFolderState: CameraUploadsFolderState,
        backup: Backup,
        cameraUploadsFolderType: CameraUploadFolderType,
    ) {
        with(cameraUploadsFolderState) {
            if (shouldSendCameraUploadsHeartbeat(this, backup, cameraUploadsFolderType)) {
                cameraUploadsRepository.sendBackupHeartbeatSync(
                    backupId = backup.backupId,
                    progress = progress,
                    ups = pendingCount,
                    downs = 0,
                    timeStamp = lastTimestamp,
                    lastNode = lastHandle,
                )
            }
        }
    }

    private suspend fun shouldSendCameraUploadsHeartbeat(
        cameraUploadsFolderState: CameraUploadsFolderState,
        backup: Backup,
        cameraUploadFolderType: CameraUploadFolderType,
    ): Boolean {
        val isEnabled = when (cameraUploadFolderType) {
            CameraUploadFolderType.Primary ->
                isCameraUploadsEnabledUseCase()

            CameraUploadFolderType.Secondary ->
                isMediaUploadsEnabledUseCase()
        }
        return isEnabled &&
                cameraUploadsFolderState.bytesToUploadCount != 0L &&
                backup.state != BackupState.PAUSE_UPLOADS
    }
}
