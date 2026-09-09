package mega.privacy.android.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import mega.privacy.android.data.database.MegaDatabaseConstant

/**
 * Non contact entity
 *
 * Cache of the names and email of users the current user is not a contact of.
 *
 * @property handle
 * @property fullName
 * @property firstName
 * @property lastName
 * @property email
 */
@Entity(MegaDatabaseConstant.TABLE_NON_CONTACTS)
internal data class NonContactEntity(
    @PrimaryKey
    @ColumnInfo(name = "handle") val handle: Long,
    @ColumnInfo(name = "full_name") val fullName: String? = null,
    @ColumnInfo(name = "first_name") val firstName: String? = null,
    @ColumnInfo(name = "last_name") val lastName: String? = null,
    @ColumnInfo(name = "email") val email: String? = null,
)
