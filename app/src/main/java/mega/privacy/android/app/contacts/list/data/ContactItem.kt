package mega.privacy.android.app.contacts.list.data

import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.annotation.ColorRes
import androidx.recyclerview.widget.DiffUtil
import mega.privacy.android.app.utils.AvatarUtil

/**
 * View item that represents a Contact Item at UI level.
 * This sealed class can be either a `Data` or a `Header` data class.
 *
 * @property id     Contact item Id.
 */
sealed class ContactItem(val id: Long) {

    abstract fun getSection(): String

    /**
     * View item that represents the data of a Contact at UI level.
     *
     * @property handle         User handle
     * @property email          User email
     * @property fullName       User email
     * @property alias          User alias
     * @property status         User status code
     * @property statusColor    User status color
     * @property avatarUri      User avatar uri
     * @property placeholder    User avatar placeholder
     * @property lastSeen       User last seen description
     * @property isNew          User flag to know if it has been added recently
     * @property isVerified     User flag to know if the contact has been verified
     */
    data class Data constructor(
        val handle: Long,
        val email: String,
        val fullName: String? = null,
        val alias: String? = null,
        val status: Int? = null,
        @ColorRes val statusColor: Int? = null,
        var avatarUri: Uri? = null,
        val placeholder: Drawable,
        val lastSeen: String? = null,
        val isNew: Boolean = false,
        val isVerified: Boolean = false,
    ) : ContactItem(handle) {

        fun getTitle(): String =
            when {
                !alias.isNullOrBlank() -> alias
                !fullName.isNullOrBlank() -> fullName
                else -> email
            }

        override fun getSection(): String =
            getFirstCharacter()

        fun getFirstCharacter(): String =
            AvatarUtil.getFirstLetter(getTitle())

        fun matches(queryString: String): Boolean =
            fullName?.contains(queryString, true) == true ||
                    email.contains(queryString, true) ||
                    alias?.contains(queryString, true) == true
    }

    /**
     * View item that represents the item header at UI level.
     *
     * @property title  String to show as the header
     */
    data class Header constructor(val title: String) : ContactItem(title.hashCode().toLong()) {

        override fun getSection(): String =
            AvatarUtil.getFirstLetter(title)
    }

    class DiffCallback : DiffUtil.ItemCallback<ContactItem>() {
        override fun areItemsTheSame(oldItem: ContactItem, newItem: ContactItem): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: ContactItem, newItem: ContactItem): Boolean {
            val isSameDataItem = oldItem is Data && newItem is Data && oldItem == newItem
            val isSameHeaderItem = oldItem is Header && newItem is Header && oldItem == newItem
            return isSameDataItem || isSameHeaderItem
        }
    }
}
