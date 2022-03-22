package mega.privacy.android.app.meeting.adapter

import androidx.recyclerview.widget.DiffUtil

class ParticipantDiffCallback : DiffUtil.ItemCallback<Participant>() {

    override fun areItemsTheSame(oldItem: Participant, newItem: Participant) = oldItem.peerId == newItem.peerId && oldItem.clientId == newItem.clientId

    override fun areContentsTheSame(oldItem: Participant, newItem: Participant): Boolean = oldItem == newItem
}
