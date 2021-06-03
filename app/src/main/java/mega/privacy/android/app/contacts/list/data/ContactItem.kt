package mega.privacy.android.app.contacts.list.data

import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.annotation.ColorRes
import androidx.recyclerview.widget.DiffUtil
import java.util.*

sealed class ContactItem(val id: Long) {

    data class Data constructor(
        val handle: Long,
        val email: String,
        val firstName: String? = null,
        val lastName: String? = null,
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
                firstName.isNullOrBlank() -> email
                lastName.isNullOrBlank() && !firstName.isNullOrBlank() -> firstName
                else -> "$firstName $lastName"
            }

        fun getFirstCharacter(): String =
            getTitle().first().toString().toUpperCase(Locale.getDefault())

        fun matches(queryString: String): Boolean =
            firstName?.contains(queryString, true) == true ||
                    lastName?.contains(queryString, true) == true ||
                    alias?.contains(queryString, true) == true ||
                    email.contains(queryString, true)
    }

    data class Header(val title: String) : ContactItem(title.hashCode().toLong())

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
