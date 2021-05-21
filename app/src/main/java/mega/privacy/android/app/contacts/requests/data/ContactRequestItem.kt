package mega.privacy.android.app.contacts.requests.data

import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.recyclerview.widget.DiffUtil

data class ContactRequestItem constructor(
    val handle: Long,
    val email: String? = null,
    val name: String? = null,
    var avatarUri: Uri? = null,
    val placeholder: Drawable,
    val createdTime: String? = null,
    val isOutgoing: Boolean = true
) {

    class DiffCallback : DiffUtil.ItemCallback<ContactRequestItem>() {

        override fun areItemsTheSame(oldItem: ContactRequestItem, newItem: ContactRequestItem): Boolean =
            oldItem.handle == newItem.handle

        override fun areContentsTheSame(oldItem: ContactRequestItem, newItem: ContactRequestItem): Boolean =
            oldItem == newItem
    }
}
