package mega.privacy.android.app.meeting.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import mega.privacy.android.app.databinding.ItemMeetingParticipantBinding
import mega.privacy.android.app.meeting.listeners.BottomFloatingPanelListener
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
     * @param icon the icon flag for mic or cam
     * @param state the new state of mic or cam
     */
    fun updateIcon(icon: Int, state: Boolean) {
        val localList = this.currentList
        if (localList.isNullOrEmpty()) {
            return
        }

        val myParticipant = this.currentList.filter { it.isMe }
        if (myParticipant.isNullOrEmpty()) {
            return
        }

        val me = myParticipant.last()
        val index = localList.indexOf(me)
        if (index < 0 || me == null) {
            return
        }

        when (icon) {
            MIC -> me.isAudioOn = state
            CAM -> me.isVideoOn = state
            else -> me.isModerator = state
        }

        notifyItemChanged(index, me)
    }

    /**
     * Update the participant's avatar
     *
     * @param peerId User handle of the participant
     */
    fun updateAvatar(peerId: Long){
        val localList = this.currentList
        if (localList.isNullOrEmpty()) {
            return
        }

        val changeParticipants = this.currentList.filter { it.peerId == peerId }
        if (changeParticipants.isNullOrEmpty()) {
            return
        }

        val target = changeParticipants.last()
        val index = localList.indexOf(target)
        if (index < 0 || target == null) {
            return
        }

        target.avatar = null
        notifyItemChanged(index)
    }


    /**
     * Update the icon when the state of other participant's mic or cam changing
     *
     * @param peerId   User handle of the participant
     * @param clientId Client identifier of the participant
     */
    fun updateParticipantAudioVideo(peerId: Long, clientId: Long) {
        val localList = this.currentList
        if (localList.isNullOrEmpty()) {
            return
        }

        val participants =
            this.currentList.filter { participant -> participant.peerId == peerId && participant.clientId == clientId }
        if (participants.isNullOrEmpty()) {
            return
        }

        val participant = participants.last()
        val index = localList.indexOf(participant)

        notifyItemChanged(index, participant)
    }

    /**
     * Update the permission for participant when the permission is changing
     *
     * @param peerId   User handle of the participant
     * @param clientId Client identifier of the participant
     */
    fun updateParticipantPermission(peerId: Long, clientId: Long){
        val localList = this.currentList
        if (localList.isNullOrEmpty()) {
            return
        }

        val participants =
            this.currentList.filter { participant -> participant.peerId == peerId && participant.clientId == clientId }
        if (participants.isNullOrEmpty()) {
            return
        }

        val participant = participants.last()
        participant.isModerator = inMeetingViewModel.isParticipantModerator(peerId)
        val index = localList.indexOf(participant)
        notifyItemChanged(index, participant)
    }

    /**
     * Update participant's name
     *
     * @param listParticipants List of participants with changes
     */
    fun updateName(listParticipants: MutableSet<Participant>) {
        val localList = this.currentList
        if (localList.isNullOrEmpty()) {
            return
        }

        listParticipants.forEach { newParticipant ->
            val participants =
                this.currentList.filter { participant -> participant.peerId == newParticipant.peerId && participant.clientId == newParticipant.clientId }

            if (participants.isNullOrEmpty()) {
                return@forEach
            }

            participants.forEach {
                it.name = newParticipant.name
                val index = localList.indexOf(it)
                notifyItemChanged(index, it)
            }
        }
    }

    companion object {
        const val MIC = 0
        const val CAM = 1
        const val MODERATOR = 2
    }
}
