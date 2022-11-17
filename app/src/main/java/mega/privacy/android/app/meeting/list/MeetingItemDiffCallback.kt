package mega.privacy.android.app.meeting.list

import androidx.recyclerview.widget.DiffUtil

class MeetingItemDiffCallback : DiffUtil.ItemCallback<MeetingItem>() {
    override fun areItemsTheSame(oldItem: MeetingItem, newItem: MeetingItem): Boolean =
        oldItem.chatId == newItem.chatId

    override fun areContentsTheSame(oldItem: MeetingItem, newItem: MeetingItem): Boolean =
        oldItem == newItem
}
