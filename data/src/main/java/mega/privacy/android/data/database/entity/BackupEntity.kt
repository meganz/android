package mega.privacy.android.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import mega.privacy.android.data.database.MegaDatabaseConstant

/**
 * Backup entity
 *
 * @property id
 * @property encryptedBackupId
 * @property backupType
 * @property encryptedTargetNode
 * @property encryptedLocalFolder
 * @property encryptedBackupName
 * @property state
 * @property subState
 * @property encryptedExtraData
 * @property encryptedStartTimestamp
 * @property encryptedLastFinishTimestamp
 * @property encryptedTargetFolderPath
 * @property encryptedShouldExcludeSubFolders
 * @property encryptedShouldDeleteEmptySubFolders
 * @property encryptedIsOutdated
 */
@Entity(MegaDatabaseConstant.TABLE_BACKUPS)
internal data class BackupEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Int? = null,
    @ColumnInfo(name = "backup_id") val encryptedBackupId: String,
    @ColumnInfo(name = "backup_type") val backupType: Int,
    @ColumnInfo(name = "target_node") val encryptedTargetNode: String,
    @ColumnInfo(name = "local_folder") val encryptedLocalFolder: String,
    @ColumnInfo(name = "backup_name") val encryptedBackupName: String,
    @ColumnInfo(name = "state") val state: Int,
    @ColumnInfo(name = "sub_state") val subState: Int,
    @ColumnInfo(name = "extra_data") val encryptedExtraData: String,
    @ColumnInfo(name = "start_timestamp") val encryptedStartTimestamp: String,
    @ColumnInfo(name = "last_sync_timestamp") val encryptedLastFinishTimestamp: String,
    @ColumnInfo(name = "target_folder_path") val encryptedTargetFolderPath: String,
    @ColumnInfo(name = "exclude_subFolders") val encryptedShouldExcludeSubFolders: String,
    @ColumnInfo(name = "delete_empty_subFolders") val encryptedShouldDeleteEmptySubFolders: String,
    @ColumnInfo(name = "outdated") val encryptedIsOutdated: String,
)
