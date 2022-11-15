package mega.privacy.android.app.meeting.list.adapter

import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.databinding.ItemMeetingHeaderBinding

class MeetingHeaderViewHolder(
    private val binding: ItemMeetingHeaderBinding,
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(headerText: String) {
        binding.txtHeader.text = headerText
    }
}
