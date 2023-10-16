package mega.privacy.android.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
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
 * @property lastModifiedTime
 */
@Entity(MegaDatabaseConstant.TABLE_OFFLINE)
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
    @ColumnInfo(name = "lastModifiedTime") val lastModifiedTime: Long?
)