package mega.privacy.android.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import mega.privacy.android.data.database.MegaDatabaseConstant

/**
 * SyncRecord entity
 *
 * @property id
 * @property localPath
 * @property newPath
 * @property originalFingerPrint
 * @property newFingerprint
 * @property timestamp
 * @property state
 * @property fileName
 * @property nodeHandle
 * @property isCopyOnly
 * @property isSecondary
 * @property type
 * @property latitude
 * @property longitude
 */
@Entity(MegaDatabaseConstant.TABLE_SYNC_RECORDS)
internal data class SyncRecordEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Int? = null,
    @ColumnInfo(name = "sync_filepath_origin") val originalPath: String?,
    @ColumnInfo(name = "sync_filepath_new") val newPath: String?,
    @ColumnInfo(name = "sync_fingerprint_origin") val originalFingerPrint: String?,
    @ColumnInfo(name = "sync_fingerprint_new") val newFingerprint: String?,
    @ColumnInfo(name = "sync_timestamp") val timestamp: String?,
    @ColumnInfo(name = "sync_filename") val fileName: String?,
    @ColumnInfo(name = "sync_handle") val nodeHandle: String?,
    @ColumnInfo(name = "sync_copyonly") val isCopyOnly: String?,
    @ColumnInfo(name = "sync_secondary") val isSecondary: String?,
    @ColumnInfo(name = "sync_latitude") val latitude: String?,
    @ColumnInfo(name = "sync_longitude") val longitude: String?,
    @ColumnInfo(name = "sync_state") val state: Int?,
    @ColumnInfo(name = "sync_type") val type: Int?,
)
