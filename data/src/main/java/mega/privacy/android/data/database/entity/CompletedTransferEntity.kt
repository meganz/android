package mega.privacy.android.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import mega.privacy.android.data.database.MegaDatabaseConstant

/**
 * Completed transfer entity
 *
 * @property id
 * @property fileName
 * @property type
 * @property state
 * @property size
 * @property handle
 * @property path
 * @property isOffline
 * @property timestamp
 * @property error
 * @property originalPath
 * @property parentHandle
 *
 */
@Entity(MegaDatabaseConstant.TABLE_COMPLETED_TRANSFERS)
internal data class CompletedTransferEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Int? = null,
    @ColumnInfo(name = "transferfilename") val fileName: String?,
    @ColumnInfo(name = "transfertype") val type: String?,
    @ColumnInfo(name = "transferstate") val state: String?,
    @ColumnInfo(name = "transfersize") val size: String?,
    @ColumnInfo(name = "transferhandle") val handle: String?,
    @ColumnInfo(name = "transferpath") val path: String?,
    @ColumnInfo(name = "transferoffline") val isOffline: String?,
    @ColumnInfo(name = "transfertimestamp") val timestamp: String?,
    @ColumnInfo(name = "transfererror") val error: String?,
    @ColumnInfo(name = "transferoriginalpath") val originalPath: String?,
    @ColumnInfo(name = "transferparenthandle") val parentHandle: String?,
)
