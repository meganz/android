package mega.privacy.android.domain.entity.backup

import mega.privacy.android.domain.entity.BackupState

/**
 * Backup data object.
 * @property id local id if exists
 * @property backupId ID of the backup, generate by server when set backup.
 * @property backupType [Int]
 * @property targetNode Handle of the MegaNode where the backup targets to.
 * @property localFolder  Path of the local folder where the backup uploads from.
 * @property backupName
 * @property state [BackupState]
 * @property subState
 * @property extraData
 * @property startTimestamp
 * @property lastFinishTimestamp
 * @property targetFolderPath
 * @property isExcludeSubFolders
 * @property isDeleteEmptySubFolders
 * @property outdated
 */
data class Backup(
    val id: Int? = null,
    val backupId: Long,
    val backupType: Int,
    val targetNode: Long,
    val localFolder: String,
    val backupName: String,
    var state: BackupState = BackupState.ACTIVE,
    val subState: Int,
    val extraData: String,
    val startTimestamp: Long = 0L,
    val lastFinishTimestamp: Long = 0L,
    val targetFolderPath: String,
    val isExcludeSubFolders: Boolean = false,
    val isDeleteEmptySubFolders: Boolean = false,
    val outdated: Boolean = false,
)
