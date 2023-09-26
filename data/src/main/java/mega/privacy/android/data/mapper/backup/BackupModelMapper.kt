package mega.privacy.android.data.mapper.backup

import mega.privacy.android.data.cryptography.DecryptData
import mega.privacy.android.data.database.entity.BackupEntity
import mega.privacy.android.data.mapper.camerauploads.BackupStateMapper
import mega.privacy.android.domain.entity.backup.Backup
import javax.inject.Inject

/**
 * Mapper that converts the [BackupEntity] to  [Backup]
 */
internal class BackupModelMapper @Inject constructor(
    private val decryptData: DecryptData,
    private val backupStateMapper: BackupStateMapper,
) {
    /**
     * Invocation function
     *
     * @param entity [BackupEntity]
     * @return [Backup]
     */
    suspend operator fun invoke(entity: BackupEntity): Backup? {
        return Backup(
            id = entity.id,
            backupId = decryptData(entity.encryptedBackupId)?.toLong() ?: return null,
            backupType = entity.backupType,
            targetNode = decryptData(entity.encryptedTargetNode)?.toLong() ?: return null,
            localFolder = decryptData(entity.encryptedLocalFolder) ?: return null,
            backupName = decryptData(entity.encryptedBackupName) ?: return null,
            state = backupStateMapper(entity.state),
            subState = entity.subState,
            extraData = decryptData(entity.encryptedExtraData) ?: return null,
            startTimestamp = decryptData(entity.encryptedStartTimestamp)?.toLong() ?: return null,
            lastFinishTimestamp = decryptData(entity.encryptedLastFinishTimestamp)?.toLong()
                ?: return null,
            targetFolderPath = decryptData(entity.encryptedTargetFolderPath) ?: return null,
            isExcludeSubFolders = decryptData(entity.encryptedShouldExcludeSubFolders)?.toBoolean()
                ?: return null,
            isDeleteEmptySubFolders = decryptData(entity.encryptedShouldDeleteEmptySubFolders)?.toBoolean()
                ?: return null,
            outdated = decryptData(entity.encryptedIsOutdated)?.toBoolean() ?: return null,
        )
    }
}
