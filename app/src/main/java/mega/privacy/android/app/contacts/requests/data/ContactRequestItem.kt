package mega.privacy.android.app.contacts.requests.data

import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.recyclerview.widget.DiffUtil

/**
 * View item that represents a Contact Request at UI level.
 *
 * @property handle         Request handle
 * @property email          User email
 * @property avatarUri      User avatar Uri
 * @property placeholder    User avatar placeholder
 * @property createdTime    Request creation time
 * @property isOutgoing     Flag to check whether it's outgoing or ingoing request
 */
data class ContactRequestItem constructor(
    val handle: Long,
    val email: String,
    var avatarUri: Uri? = null,
    val placeholder: Drawable,
    val createdTime: String? = null,
    val isOutgoing: Boolean = true,
) {

    class DiffCallback : DiffUtil.ItemCallback<ContactRequestItem>() {

        override fun areItemsTheSame(
            oldItem: ContactRequestItem,
            newItem: ContactRequestItem,
        ): Boolean =
            oldItem.handle == newItem.handle

        override fun areContentsTheSame(
            oldItem: ContactRequestItem,
            newItem: ContactRequestItem,
        ): Boolean =
            oldItem == newItem
    }
}
