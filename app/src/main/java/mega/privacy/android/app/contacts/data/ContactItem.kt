package mega.privacy.android.app.contacts.data

import android.net.Uri
import androidx.recyclerview.widget.DiffUtil

data class ContactItem constructor(
    val handle: Long,
    val email: String,
    val name: String,
    val status: Int,
    val lastSeen: String? = null,
    val imageColor: Int,
    var imageUri: Uri? = null,
    val isNew: Boolean = false
) {

    class DiffCallback : DiffUtil.ItemCallback<ContactItem>() {

        override fun areItemsTheSame(oldItem: ContactItem, newItem: ContactItem): Boolean =
            oldItem.handle == newItem.handle

        override fun areContentsTheSame(oldItem: ContactItem, newItem: ContactItem): Boolean =
            oldItem == newItem
    }
}
