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
@Deprecated(
    "This entity is deprecated and replaced by CompletedTransferEntity since we don't need column based encryption as the whole data base is encrypted. This entity will be removed in future versions",
    replaceWith = ReplaceWith("mega.privacy.android.data.database.entityCompletedTransferEntity")
)
@Entity(MegaDatabaseConstant.TABLE_COMPLETED_TRANSFERS_LEGACY)
internal data class CompletedTransferEntityLegacy(
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
    @ColumnInfo(name = "transferappdata") val appData: String?,
)
