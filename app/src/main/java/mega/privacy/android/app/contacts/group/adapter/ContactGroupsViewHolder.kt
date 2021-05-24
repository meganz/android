package mega.privacy.android.app.contacts.group.adapter

import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.facebook.imagepipeline.request.ImageRequest
import mega.privacy.android.app.contacts.group.data.ContactGroupItem
import mega.privacy.android.app.databinding.ItemGroupBinding

class ContactGroupsViewHolder(
    private val binding: ItemGroupBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: ContactGroupItem) {
        binding.txtTitle.text = item.title
        binding.imgPrivate.isVisible = !item.isPublic
        binding.imgThumbnailFirst.hierarchy.setPlaceholderImage(item.firstUserPlaceholder)
        binding.imgThumbnailLast.hierarchy.setPlaceholderImage(item.lastUserPlaceholder)
        binding.imgThumbnailFirst.setImageRequest(ImageRequest.fromUri(item.firstUserAvatar))
        binding.imgThumbnailLast.setImageRequest(ImageRequest.fromUri(item.lastUserAvatar))
    }
}
