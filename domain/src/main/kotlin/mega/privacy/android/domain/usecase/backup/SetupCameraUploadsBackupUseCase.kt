package mega.privacy.android.domain.usecase.backup

import mega.privacy.android.domain.entity.BackupState
import mega.privacy.android.domain.entity.backup.BackupInfoType
import mega.privacy.android.domain.entity.camerauploads.HeartbeatStatus
import mega.privacy.android.domain.usecase.camerauploads.GetPrimaryFolderPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetPrimarySyncHandleUseCase
import mega.privacy.android.domain.usecase.camerauploads.SendCameraUploadsBackupHeartBeatUseCase
import javax.inject.Inject

/**
 * Setup Camera Uploads backup
 */
class SetupCameraUploadsBackupUseCase @Inject constructor(
    private val setBackupUseCase: SetBackupUseCase,
    private val getPrimarySyncHandleUseCase: GetPrimarySyncHandleUseCase,
    private val getPrimaryFolderPathUseCase: GetPrimaryFolderPathUseCase,
    private val broadcastBackupInfoTypeUseCase: BroadcastBackupInfoTypeUseCase,
    private val sendCameraUploadsBackupHeartBeatUseCase: SendCameraUploadsBackupHeartBeatUseCase,
) {

    /**
     * Invocation function
     * @param cameraUploadFolderName
     */
    suspend operator fun invoke(cameraUploadFolderName: String) {
        val handle = getPrimarySyncHandleUseCase()
        if (handle == -1L) {
            broadcastBackupInfoTypeUseCase(BackupInfoType.CAMERA_UPLOADS)
            return
        }
        val primaryFolderPath = getPrimaryFolderPathUseCase()

        if (primaryFolderPath.isEmpty()) {
            broadcastBackupInfoTypeUseCase(BackupInfoType.CAMERA_UPLOADS)
            return
        }

        setBackupUseCase(
            backupType = BackupInfoType.CAMERA_UPLOADS,
            targetNode = handle,
            localFolder = primaryFolderPath,
            backupName = cameraUploadFolderName,
            state = BackupState.ACTIVE,
        ).also {
            broadcastBackupInfoTypeUseCase(BackupInfoType.CAMERA_UPLOADS)
        }
        runCatching {
            sendCameraUploadsBackupHeartBeatUseCase(
                heartbeatStatus = HeartbeatStatus.UNKNOWN,
                lastNodeHandle = -1
            )
        }
    }
}
