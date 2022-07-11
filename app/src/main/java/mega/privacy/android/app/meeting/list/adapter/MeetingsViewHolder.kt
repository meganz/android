package mega.privacy.android.app.meeting.list.adapter

import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.databinding.ItemMeetingBinding
import mega.privacy.android.app.meeting.list.MeetingItem
import mega.privacy.android.app.utils.setImageRequestFromUri

class MeetingsViewHolder(
    private val binding: ItemMeetingBinding,
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: MeetingItem) {
        binding.txtTitle.text = item.title

        val firstUserPlaceholder = item.firstUser.getImagePlaceholder(itemView.context)
        val lastUserPlaceholder = item.lastUser.getImagePlaceholder(itemView.context)
        binding.imgThumbnailGroupFirst.hierarchy.setPlaceholderImage(firstUserPlaceholder)
        binding.imgThumbnailGroupLast.hierarchy.setPlaceholderImage(lastUserPlaceholder)
        binding.imgThumbnailGroupFirst.setImageRequestFromUri(item.firstUser.avatar)
        binding.imgThumbnailGroupLast.setImageRequestFromUri(item.lastUser.avatar)
    }
}
