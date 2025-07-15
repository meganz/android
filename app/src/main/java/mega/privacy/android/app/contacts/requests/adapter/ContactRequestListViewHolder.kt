package mega.privacy.android.app.contacts.requests.adapter

import androidx.recyclerview.widget.RecyclerView
import coil3.asImage
import coil3.load
import coil3.request.transformations
import coil3.transform.CircleCropTransformation
import mega.privacy.android.app.contacts.requests.data.ContactRequestItem
import mega.privacy.android.app.databinding.ItemContactRequestBinding
import mega.privacy.android.domain.entity.user.ContactAvatar
import mega.privacy.android.domain.entity.user.UserId

/**
 * RecyclerView's ViewHolder to show ContactRequestItem.
 *
 * @property binding    Item's view binding
 */
class ContactRequestListViewHolder(
    private val binding: ItemContactRequestBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: ContactRequestItem) {
        println("CR Timing - ViewHolder Bind")
        binding.txtTitle.text = item.email
        binding.txtSubtitle.text = item.createdTime
        binding.imgThumbnail.load(
            data = ContactAvatar(email = item.email, id = UserId(item.handle))
        ) {
            transformations(CircleCropTransformation())
            placeholder(item.placeholder.asImage())
        }
    }
}
