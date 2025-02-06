package mega.privacy.android.app.contacts.group.adapter

import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import mega.privacy.android.app.contacts.group.data.ContactGroupItem
import mega.privacy.android.app.databinding.ItemContactGroupBinding

/**
 * RecyclerView's ViewHolder to show ContactGroupItem.
 *
 * @property binding    Item's view binding
 */
class ContactGroupsViewHolder(
    private val binding: ItemContactGroupBinding,
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: ContactGroupItem) {
        binding.txtTitle.text = item.title
        binding.imgPrivate.isVisible = !item.isPublic
        binding.imgThumbnailFirst.load(item.firstUser.avatar) {
            transformations(CircleCropTransformation())
            placeholder(item.firstUser.getImagePlaceholder(itemView.context))
            listener(onError = { _, _ ->
                binding.imgThumbnailFirst.setImageDrawable(
                    item.firstUser.getImagePlaceholder(itemView.context)
                )
            })
        }
        binding.imgThumbnailLast.load(item.lastUser.avatar) {
            transformations(CircleCropTransformation())
            placeholder(item.lastUser.getImagePlaceholder(itemView.context))
            listener(onError = { _, _ ->
                binding.imgThumbnailLast.setImageDrawable(
                    item.lastUser.getImagePlaceholder(itemView.context)
                )
            })
        }
    }
}
