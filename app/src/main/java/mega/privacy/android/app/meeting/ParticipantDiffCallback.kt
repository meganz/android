package mega.privacy.android.app.meeting

import androidx.recyclerview.widget.DiffUtil

class ParticipantDiffCallback : DiffUtil.ItemCallback<Participant>() {

    override fun areItemsTheSame(oldItem: Participant, newItem: Participant) = false

    override fun areContentsTheSame(oldItem: Participant, newItem: Participant) = false
}
