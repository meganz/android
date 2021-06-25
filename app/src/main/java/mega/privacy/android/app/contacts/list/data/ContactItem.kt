package mega.privacy.android.app.contacts.list.data

import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.annotation.ColorRes
import androidx.recyclerview.widget.DiffUtil
import mega.privacy.android.app.utils.AvatarUtil

sealed class ContactItem(val id: Long) {

    abstract fun getSection(): String

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
        val isNew: Boolean = false
    ) : ContactItem(handle) {

        fun getTitle(): String =
            when {
                !alias.isNullOrBlank() -> alias
                !fullName.isNullOrBlank() -> fullName
                else -> email
            }

        override fun getSection(): String =
            AvatarUtil.getFirstLetter(getTitle())

        fun getFirstCharacter(): String =
            AvatarUtil.getFirstLetter(getTitle())

        fun matches(queryString: String): Boolean =
            fullName?.contains(queryString, true) == true ||
                    email.contains(queryString, true) ||
                    alias?.contains(queryString, true) == true
    }

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
