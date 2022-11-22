package mega.privacy.android.app.meeting.list.adapter

import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.databinding.ItemMeetingHeaderBinding
import mega.privacy.android.app.meeting.list.MeetingItem

class MeetingHeaderViewHolder(
    private val binding: ItemMeetingHeaderBinding,
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: MeetingItem.Header) {
        binding.txtHeader.text = item.title
    }
}
