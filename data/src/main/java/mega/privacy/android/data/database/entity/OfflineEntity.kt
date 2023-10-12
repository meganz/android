package mega.privacy.android.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import mega.privacy.android.data.database.MegaDatabaseConstant

/**
 * Offline entity
 * @property id
 * @property handle
 * @property path
 * @property name
 * @property parentId
 * @property type
 * @property incoming
 * @property incomingHandle
 */
@Entity(MegaDatabaseConstant.TABLE_OFFLINE)
internal class OfflineEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Int? = null,
    @ColumnInfo(name = "handle") val handle: String?,
    @ColumnInfo(name = "path") val path: String?,
    @ColumnInfo(name = "name") val name: String?,
    @ColumnInfo(name = "parentId") val parentId: Int?,
    @ColumnInfo(name = "type") val type: Int?,
    @ColumnInfo(name = "incoming") val incoming: Int?,
    @ColumnInfo(name = "incomingHandle") val incomingHandle: Int?
)