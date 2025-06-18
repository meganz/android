package mega.privacy.android.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
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
 * @property displayPath
 * @property isOffline
 * @property timestamp
 * @property error
 * @property errorCode
 * @property originalPath
 * @property parentHandle
 *
 */
@Entity(
    MegaDatabaseConstant.TABLE_COMPLETED_TRANSFERS,
    indices = [Index(value = ["transferstate", "transfertimestamp"])]
)
internal data class CompletedTransferEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Int? = null,
    @ColumnInfo(name = "transferfilename") val fileName: String,
    @ColumnInfo(name = "transfertype") val type: Int,
    @ColumnInfo(name = "transferstate") val state: Int,
    @ColumnInfo(name = "transfersize") val size: String,
    @ColumnInfo(name = "transferhandle") val handle: Long,
    @ColumnInfo(name = "transferpath") var path: String,
    @ColumnInfo(name = "transferdisplaypath") val displayPath: String?,
    @ColumnInfo(name = "transferoffline") var isOffline: Boolean?,
    @ColumnInfo(name = "transfertimestamp") val timestamp: Long,
    @ColumnInfo(name = "transfererror") val error: String?,
    @ColumnInfo(name = "transfererrorcode") val errorCode: Int?,
    @ColumnInfo(name = "transferoriginalpath") val originalPath: String,
    @ColumnInfo(name = "transferparenthandle") val parentHandle: Long,
    @ColumnInfo(name = "transferappdata") val appData: String?,
)
