package mega.privacy.android.app.meeting.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import mega.privacy.android.app.databinding.ItemMeetingParticipantBinding
import mega.privacy.android.app.meeting.BottomFloatingPanelListener
import mega.privacy.android.app.meeting.fragments.InMeetingViewModel

class ParticipantsAdapter(
    private val inMeetingViewModel: InMeetingViewModel,
    private val listener: BottomFloatingPanelListener
) : ListAdapter<Participant, ParticipantViewHolder>(ParticipantDiffCallback()) {

    override fun onBindViewHolder(holder: ParticipantViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParticipantViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ParticipantViewHolder(
            inMeetingViewModel,
            ItemMeetingParticipantBinding.inflate(inflater, parent, false)
        ) {
            listener.onParticipantOption(getItem(it))
        }
    }

    /**
     * Update the my mic and cam icon
     *
     * @param icon
     * @param state
     */
    fun updateIcon(icon: Int, state: Boolean) {
        val myParticipant = this.currentList.filterIndexed { _, participant -> participant.isMe }
        val index = myParticipant.lastIndex
        val me = myParticipant.last()
        if (index < 0 || me == null) {
            return
        }
        if (icon == MIC) {
            me.isAudioOn = state
        } else {
            me.isVideoOn = state
        }

        notifyItemChanged(index, me)
    }

    companion object {
        const val MIC = 0
        const val CAM = 1
    }
}
