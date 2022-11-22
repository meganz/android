package mega.privacy.android.app.meeting.list

import androidx.recyclerview.widget.DiffUtil

class MeetingItemDiffCallback : DiffUtil.ItemCallback<MeetingItem>() {
    override fun areItemsTheSame(oldItem: MeetingItem, newItem: MeetingItem): Boolean =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: MeetingItem, newItem: MeetingItem): Boolean =
        oldItem == newItem
}
