package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.SyncTimeStamp
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Default implementation of [BackupTimeStampsAndFolderHandle]
 */
class DefaultBackupTimeStampsAndFolderHandle @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
    private val settingsRepository: SettingsRepository,
) : BackupTimeStampsAndFolderHandle {
    override suspend fun invoke() {
        cameraUploadRepository.run {
            settingsRepository.backupTimestampsAndFolderHandle(
                getPrimarySyncHandle()
                    ?: getInvalidHandle(),
                getSecondarySyncHandle()
                    ?: getInvalidHandle(),
                getSyncTimeStamp(SyncTimeStamp.PRIMARY_PHOTO),
                getSyncTimeStamp(SyncTimeStamp.PRIMARY_VIDEO),
                getSyncTimeStamp(SyncTimeStamp.SECONDARY_PHOTO),
                getSyncTimeStamp(SyncTimeStamp.SECONDARY_VIDEO)
            )
        }

    }

}