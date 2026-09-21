package mega.privacy.android.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import mega.privacy.android.data.database.MegaDatabaseConstant

/**
 * Offline entity
 * @property id
 * @property encryptedHandle
 * @property encryptedPath
 * @property encryptedName
 * @property parentId
 * @property encryptedType
 * @property incoming
 * @property lastModifiedTime
 * @property isOfflineRoot true when the user explicitly made this node available offline, as
 * opposed to it being stored only as an ancestor of such a node
 */
@Entity(
    tableName = MegaDatabaseConstant.TABLE_OFFLINE,
    indices = [Index(value = ["handle", "path"], unique = true)]
)
internal data class OfflineEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Int? = null,
    @ColumnInfo(name = "handle") val encryptedHandle: String?,
    @ColumnInfo(name = "path") val encryptedPath: String?,
    @ColumnInfo(name = "name") val encryptedName: String?,
    @ColumnInfo(name = "parentId") val parentId: Int?,
    @ColumnInfo(name = "type") val encryptedType: String?,
    @ColumnInfo(name = "incoming") val incoming: Int?,
    @ColumnInfo(name = "incomingHandle") val encryptedIncomingHandle: String?,
    @ColumnInfo(name = "lastModifiedTime") val lastModifiedTime: Long?,
    @ColumnInfo(name = "isOfflineRoot", defaultValue = "0") val isOfflineRoot: Boolean = false,
)
