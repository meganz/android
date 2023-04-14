package mega.privacy.android.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import mega.privacy.android.data.database.MegaDatabaseConstant

/**
 * Contact entity
 *
 * @property id
 * @property handle
 * @property mail
 * @property firstName
 * @property lastName
 * @property nickName
 */
@Entity(MegaDatabaseConstant.TABLE_CONTACTS)
internal data class ContactEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Int? = null,
    @ColumnInfo(name = "handle") val handle: String?,
    @ColumnInfo(name = "mail") val mail: String?,
    @ColumnInfo(name = "name") val firstName: String?,
    @ColumnInfo(name = "lastname") val lastName: String?,
    @ColumnInfo(name = "nickname") val nickName: String?,
)