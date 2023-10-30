package mega.privacy.android.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import mega.privacy.android.data.database.MegaDatabaseConstant
import mega.privacy.android.data.database.entity.BackupEntity

@Dao
internal interface BackupDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateBackup(entity: BackupEntity)

    @Query("SELECT * FROM ${MegaDatabaseConstant.TABLE_BACKUPS}")
    suspend fun getAllBackups(): List<BackupEntity>

    @Query("SELECT * FROM ${MegaDatabaseConstant.TABLE_BACKUPS} WHERE backup_type = :backupType AND outdated = :encryptedIsOutdated ORDER BY id")
    suspend fun getBackupByType(backupType: Int, encryptedIsOutdated: String): List<BackupEntity>

    @Query("SELECT backup_id FROM ${MegaDatabaseConstant.TABLE_BACKUPS} WHERE backup_type = :backupType AND outdated = :encryptedIsOutdated ORDER BY id")
    suspend fun getBackupIdByType(backupType: Int, encryptedIsOutdated: String): List<String?>

    @Query("SELECT * FROM ${MegaDatabaseConstant.TABLE_BACKUPS} WHERE backup_id = :encryptedBackupId")
    suspend fun getBackupById(encryptedBackupId: String): BackupEntity

    @Query("UPDATE ${MegaDatabaseConstant.TABLE_BACKUPS} SET outdated = :encryptedIsOutdated WHERE backup_id = :encryptedBackupId")
    suspend fun updateBackupAsOutdated(encryptedBackupId: String, encryptedIsOutdated: String)

    @Query("DELETE FROM ${MegaDatabaseConstant.TABLE_BACKUPS}")
    suspend fun deleteAllBackups()

    @Query("DELETE FROM ${MegaDatabaseConstant.TABLE_BACKUPS} WHERE backup_id = :encryptedBackupId")
    suspend fun deleteBackupByBackupId(encryptedBackupId: String)
}
