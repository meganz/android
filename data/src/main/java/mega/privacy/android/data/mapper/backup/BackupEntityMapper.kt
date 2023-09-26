package mega.privacy.android.data.mapper.backup

import mega.privacy.android.data.cryptography.EncryptData
import mega.privacy.android.data.database.entity.BackupEntity
import mega.privacy.android.data.mapper.camerauploads.BackupStateIntMapper
import mega.privacy.android.domain.entity.backup.Backup
import javax.inject.Inject

/**
 * Mapper that converts the [Backup] to [BackupEntity]
 */
internal class BackupEntityMapper @Inject constructor(
    private val encryptData: EncryptData,
    private val backupStateIntMapper: BackupStateIntMapper,
) {
    /**
     * Invocation function
     *
     * @param backup [Backup]
     * @return [BackupEntity]
     */
    suspend operator fun invoke(backup: Backup): BackupEntity? {
        return BackupEntity(
            id = backup.id,
            encryptedBackupId = encryptData(backup.backupId.toString()) ?: return null,
            backupType = backup.backupType,
            encryptedTargetNode = encryptData(backup.targetNode.toString()) ?: return null,
            encryptedLocalFolder = encryptData(backup.localFolder) ?: return null,
            encryptedBackupName = encryptData(backup.backupName) ?: return null,
            state = backupStateIntMapper(backup.state),
            subState = backup.subState,
            encryptedExtraData = encryptData(backup.extraData) ?: return null,
            encryptedStartTimestamp = encryptData(backup.startTimestamp.toString()) ?: return null,
            encryptedLastFinishTimestamp = encryptData(backup.lastFinishTimestamp.toString())
                ?: return null,
            encryptedTargetFolderPath = encryptData(backup.targetFolderPath) ?: return null,
            encryptedShouldExcludeSubFolders = encryptData(backup.isExcludeSubFolders.toString())
                ?: return null,
            encryptedShouldDeleteEmptySubFolders = encryptData(backup.isDeleteEmptySubFolders.toString())
                ?: return null,
            encryptedIsOutdated = encryptData(backup.outdated.toString()) ?: return null,
        )
    }
}
