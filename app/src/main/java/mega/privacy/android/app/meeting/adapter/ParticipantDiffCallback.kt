package mega.privacy.android.app.meeting.adapter

import androidx.recyclerview.widget.DiffUtil

class ParticipantDiffCallback : DiffUtil.ItemCallback<Participant>() {

    override fun areItemsTheSame(oldItem: Participant, newItem: Participant) = oldItem.peerId == newItem.peerId && oldItem.clientId == newItem.clientId

    override fun areContentsTheSame(oldItem: Participant, newItem: Participant) =
            oldItem.isModerator == newItem.isModerator &&
            oldItem.name == newItem.name &&
            oldItem.isAudioOn == newItem.isAudioOn &&
            oldItem.isVideoOn == newItem.isVideoOn &&
            oldItem.isContact == newItem.isContact &&
            oldItem.hasHiRes == newItem.hasHiRes
}
