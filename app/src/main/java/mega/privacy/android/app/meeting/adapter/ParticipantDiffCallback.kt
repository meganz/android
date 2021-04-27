package mega.privacy.android.app.meeting.adapter

import androidx.recyclerview.widget.DiffUtil

class ParticipantDiffCallback : DiffUtil.ItemCallback<Participant>() {

    override fun areItemsTheSame(oldItem: Participant, newItem: Participant) = oldItem == newItem

    override fun areContentsTheSame(oldItem: Participant, newItem: Participant) = oldItem.avatarBackground == newItem.avatarBackground
}
