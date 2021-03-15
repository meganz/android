package mega.privacy.android.app.meeting

import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.databinding.ItemMeetingParticipantBinding

class ParticipantViewHolder(
    private val binding: ItemMeetingParticipantBinding,
    private val listener: BottomFloatingPanelListener
) :
    RecyclerView.ViewHolder(binding.root) {
    fun bind(participant: Participant) {
        binding.apply {
            this.participant = participant
            this.listener = this@ParticipantViewHolder.listener
        }
    }
}
