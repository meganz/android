package mega.privacy.android.app.contacts.group.adapter

import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.facebook.imagepipeline.request.ImageRequest
import mega.privacy.android.app.R
import mega.privacy.android.app.contacts.group.data.GroupItem
import mega.privacy.android.app.databinding.ItemGroupBinding
import mega.privacy.android.app.utils.view.TextDrawable

class ContactGroupsViewHolder(
    private val binding: ItemGroupBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: GroupItem) {
        binding.txtTitle.text = item.title

        val placeHolder = TextDrawable.builder()
            .beginConfig()
            .fontSize(itemView.resources.getDimensionPixelSize(R.dimen.placeholder_contact_text_size))
            .bold()
            .toUpperCase()
            .endConfig()
            .buildRound(item.title.first().toString(), item.firstImageColor)
        binding.imgThumbnail.hierarchy.setPlaceholderImage(placeHolder)
        binding.imgThumbnail.setImageRequest(ImageRequest.fromUri(item.firstImage))
        binding.imgPrivate.isVisible = !item.isPublic
    }
}
