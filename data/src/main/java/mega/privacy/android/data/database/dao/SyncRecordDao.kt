package mega.privacy.android.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import mega.privacy.android.data.database.MegaDatabaseConstant
import mega.privacy.android.data.database.entity.SyncRecordEntity

@Dao
internal interface SyncRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSyncRecord(entity: SyncRecordEntity)

    @Query("SELECT * FROM ${MegaDatabaseConstant.TABLE_SYNC_RECORDS}")
    suspend fun getAllSyncRecords(): List<SyncRecordEntity>

    @Query("UPDATE ${MegaDatabaseConstant.TABLE_SYNC_RECORDS} SET sync_state = :state WHERE sync_type = 2")
    suspend fun updateVideoState(state: Int)

    @Query("SELECT COUNT(id) FROM ${MegaDatabaseConstant.TABLE_SYNC_RECORDS} WHERE sync_filename = :fileName AND sync_secondary = :secondary")
    suspend fun getSyncRecordCountByFileName(fileName: String?, secondary: String): Int

    @Query("SELECT COUNT(id) FROM ${MegaDatabaseConstant.TABLE_SYNC_RECORDS} WHERE sync_filepath_origin = :originalPath AND sync_secondary = :secondary")
    suspend fun getSyncRecordCountByOriginalPath(originalPath: String?, secondary: String): Int

    @Query("SELECT * FROM ${MegaDatabaseConstant.TABLE_SYNC_RECORDS} WHERE sync_fingerprint_origin = :originalFingerprint AND sync_secondary = :secondary AND sync_copyonly = :copyOnly")
    suspend fun getSyncRecordByOriginalFingerprint(
        originalFingerprint: String?,
        secondary: String,
        copyOnly: String,
    ): SyncRecordEntity?

    @Query("SELECT * FROM ${MegaDatabaseConstant.TABLE_SYNC_RECORDS} WHERE sync_filepath_origin = :originalPath AND sync_secondary = :secondary")
    suspend fun getSyncRecordByOriginalPathAndIsSecondary(
        originalPath: String,
        secondary: String,
    ): SyncRecordEntity?

    @Query("SELECT * FROM ${MegaDatabaseConstant.TABLE_SYNC_RECORDS} WHERE sync_filepath_new = :newPath")
    suspend fun getSyncRecordByNewPath(newPath: String): SyncRecordEntity?

    @Query("SELECT * FROM ${MegaDatabaseConstant.TABLE_SYNC_RECORDS} WHERE sync_state = :syncState")
    suspend fun getSyncRecordsBySyncState(syncState: Int): List<SyncRecordEntity>

    @Query("SELECT * FROM ${MegaDatabaseConstant.TABLE_SYNC_RECORDS} WHERE sync_state = :syncState AND sync_type = :syncType")
    suspend fun getSyncRecordsBySyncStateAndType(
        syncState: Int,
        syncType: Int,
    ): List<SyncRecordEntity>

    @Query("DELETE FROM ${MegaDatabaseConstant.TABLE_SYNC_RECORDS}")
    suspend fun deleteAllSyncRecords()

    @Query("DELETE FROM ${MegaDatabaseConstant.TABLE_SYNC_RECORDS} WHERE sync_type = :syncType")
    suspend fun deleteSyncRecordsByType(syncType: Int)

    @Query("DELETE FROM ${MegaDatabaseConstant.TABLE_SYNC_RECORDS} WHERE sync_secondary = :secondary")
    suspend fun deleteSyncRecordsByIsSecondary(secondary: String)

    @Query("DELETE FROM ${MegaDatabaseConstant.TABLE_SYNC_RECORDS} WHERE sync_state = :syncState AND sync_type = :syncType")
    suspend fun deleteSyncRecordsBySyncStateAndType(syncState: Int, syncType: Int)

    @Query("DELETE FROM ${MegaDatabaseConstant.TABLE_SYNC_RECORDS} WHERE sync_filepath_origin = :path OR sync_filepath_new = :path AND sync_secondary = :secondary")
    suspend fun deleteSyncRecordByOriginalPathOrNewPathAndIsSecondary(
        path: String,
        secondary: String,
    )

    @Query("DELETE FROM ${MegaDatabaseConstant.TABLE_SYNC_RECORDS} WHERE sync_filepath_origin = :originalPath AND sync_secondary = :secondary")
    suspend fun deleteSyncRecordByOriginalPathAndIsSecondary(
        originalPath: String,
        secondary: String,
    )

    @Query("DELETE FROM ${MegaDatabaseConstant.TABLE_SYNC_RECORDS} WHERE sync_filepath_new = :newPath")
    suspend fun deleteSyncRecordByNewPath(newPath: String)

    @Query("DELETE FROM ${MegaDatabaseConstant.TABLE_SYNC_RECORDS} WHERE sync_filename = :fileName OR sync_filepath_origin LIKE :fileName")
    suspend fun deleteSyncRecordByFileName(fileName: String)

    @Query("DELETE FROM ${MegaDatabaseConstant.TABLE_SYNC_RECORDS} WHERE sync_fingerprint_origin = :originalFingerPrint OR sync_fingerprint_new = :newFingerprint AND sync_secondary = :secondary")
    suspend fun deleteSyncRecordByFingerprintsAndIsSecondary(
        originalFingerPrint: String?,
        newFingerprint: String?,
        secondary: String,
    )

    @Query("UPDATE ${MegaDatabaseConstant.TABLE_SYNC_RECORDS} SET sync_state = :state WHERE sync_filepath_origin = :originalPath AND sync_secondary = :secondary")
    suspend fun updateSyncRecordStateByOriginalPathAndIsSecondary(
        state: Int,
        originalPath: String?,
        secondary: String,
    )

    @Query("SELECT sync_timestamp FROM ${MegaDatabaseConstant.TABLE_SYNC_RECORDS} WHERE sync_secondary = :secondary AND sync_type = :syncType")
    suspend fun getAllTimestampsByIsSecondaryAndSyncType(
        secondary: String,
        syncType: Int,
    ): List<String>
}
