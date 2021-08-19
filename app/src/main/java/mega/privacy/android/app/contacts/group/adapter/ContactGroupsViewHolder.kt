package mega.privacy.android.app.contacts.group.adapter

import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.facebook.imagepipeline.request.ImageRequest
import mega.privacy.android.app.contacts.group.data.ContactGroupItem
import mega.privacy.android.app.databinding.ItemContactGroupBinding

/**
 * RecyclerView's ViewHolder to show ContactGroupItem.
 *
 * @property binding    Item's view binding
 */
class ContactGroupsViewHolder(
    private val binding: ItemContactGroupBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: ContactGroupItem) {
        binding.txtTitle.text = item.title
        binding.imgPrivate.isVisible = !item.isPublic
        binding.imgThumbnailFirst.hierarchy.setPlaceholderImage(item.firstUser.placeholder)
        binding.imgThumbnailLast.hierarchy.setPlaceholderImage(item.lastUser.placeholder)
        binding.imgThumbnailFirst.setImageRequest(ImageRequest.fromUri(item.firstUser.avatar))
        binding.imgThumbnailLast.setImageRequest(ImageRequest.fromUri(item.lastUser.avatar))
    }
}
