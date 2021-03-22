package mega.privacy.android.app.meeting.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import mega.privacy.android.app.databinding.ItemMeetingParticipantBinding
import mega.privacy.android.app.meeting.BottomFloatingPanelListener

class ParticipantsAdapter(
    private val listener: BottomFloatingPanelListener
) : ListAdapter<Participant, ParticipantViewHolder>(ParticipantDiffCallback()) {

    override fun onBindViewHolder(holder: ParticipantViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParticipantViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ParticipantViewHolder(
            ItemMeetingParticipantBinding.inflate(inflater, parent, false)
        ) {
            listener.onParticipantOption(getItem(it))
        }
    }
}
