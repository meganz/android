package mega.privacy.android.domain.usecase.backup

import mega.privacy.android.domain.entity.BackupState
import mega.privacy.android.domain.entity.backup.BackupInfoType
import mega.privacy.android.domain.entity.camerauploads.HeartbeatStatus
import mega.privacy.android.domain.usecase.camerauploads.GetSecondaryFolderPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetSecondarySyncHandleUseCase
import mega.privacy.android.domain.usecase.camerauploads.SendMediaUploadsBackupHeartBeatUseCase
import javax.inject.Inject

/**
 * Setup Media Uploads backup
 */
class SetupMediaUploadsBackupUseCase @Inject constructor(
    private val setBackupUseCase: SetBackupUseCase,
    private val getSecondarySyncHandleUseCase: GetSecondarySyncHandleUseCase,
    private val getSecondaryFolderPathUseCase: GetSecondaryFolderPathUseCase,
    private val broadcastBackupInfoTypeUseCase: BroadcastBackupInfoTypeUseCase,
    private val sendMediaUploadsBackupHeartBeatUseCase: SendMediaUploadsBackupHeartBeatUseCase,
) {

    /**
     * Invocation function
     * @param mediaUploadsFolderName
     */
    suspend operator fun invoke(mediaUploadsFolderName: String) {
        val handle = getSecondarySyncHandleUseCase()
        if (handle == -1L) {
            broadcastBackupInfoTypeUseCase(BackupInfoType.MEDIA_UPLOADS)
            return
        }
        val secondaryFolderPath = getSecondaryFolderPathUseCase()

        if (secondaryFolderPath.isEmpty()) {
            broadcastBackupInfoTypeUseCase(BackupInfoType.MEDIA_UPLOADS)
            return
        }

        setBackupUseCase(
            backupType = BackupInfoType.MEDIA_UPLOADS,
            targetNode = handle,
            localFolder = secondaryFolderPath,
            backupName = mediaUploadsFolderName,
            state = BackupState.ACTIVE,
        ).also {
            broadcastBackupInfoTypeUseCase(BackupInfoType.MEDIA_UPLOADS)
        }
        runCatching {
            sendMediaUploadsBackupHeartBeatUseCase(
                heartbeatStatus = HeartbeatStatus.UNKNOWN,
                lastNodeHandle = -1
            )
        }
    }
}
