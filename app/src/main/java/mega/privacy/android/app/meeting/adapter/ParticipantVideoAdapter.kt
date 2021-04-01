package mega.privacy.android.app.meeting.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import mega.privacy.android.app.databinding.ItemCameraGroupCallBinding
import mega.privacy.android.app.databinding.ItemMeetingParticipantBinding
import mega.privacy.android.app.meeting.BottomFloatingPanelListener

class ParticipantVideoAdapter() :
    ListAdapter<Participant, ParticipantVideoViewHolder>(ParticipantDiffCallback()) {

    override fun onBindViewHolder(holder: ParticipantVideoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParticipantVideoViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ParticipantVideoViewHolder(
            ItemCameraGroupCallBinding.inflate(inflater, parent, false)
        )
    }
}
