package mega.privacy.android.app.meeting.list

import androidx.recyclerview.widget.DiffUtil
import mega.privacy.android.app.meeting.list.adapter.MeetingAdapterItem

class MeetingAdapterItemDiffCallback : DiffUtil.ItemCallback<MeetingAdapterItem>() {

    override fun areItemsTheSame(
        oldItem: MeetingAdapterItem,
        newItem: MeetingAdapterItem,
    ): Boolean =
        oldItem.id == newItem.id

    override fun areContentsTheSame(
        oldItem: MeetingAdapterItem,
        newItem: MeetingAdapterItem,
    ): Boolean =
        if (oldItem is MeetingAdapterItem.Data && newItem is MeetingAdapterItem.Data) {
            oldItem.room == newItem.room
        } else {
            oldItem == newItem
        }
}
