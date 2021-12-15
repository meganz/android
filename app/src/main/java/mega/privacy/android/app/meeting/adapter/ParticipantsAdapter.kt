package mega.privacy.android.app.meeting.adapter

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import mega.privacy.android.app.databinding.ItemParticipantChatListBinding
import mega.privacy.android.app.meeting.listeners.BottomFloatingPanelListener
import mega.privacy.android.app.utils.Constants.AVATAR_CHANGE
import mega.privacy.android.app.utils.Constants.NAME_CHANGE

class ParticipantsAdapter(
    private val listener: BottomFloatingPanelListener
) : ListAdapter<Participant, ParticipantViewHolder>(ParticipantDiffCallback()) {

    override fun onBindViewHolder(holder: ParticipantViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParticipantViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ParticipantViewHolder(
            ItemParticipantChatListBinding.inflate(inflater, parent, false)
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
     * Update the participant's name or avatar
     *
     * @param peerId User handle of the participant
     * @param type the type of change, name or avatar
     * @param newName new name
     * @param newAvatar new avatar
     */
    fun updateParticipantInfo(peerId: Long, type: Int, newName: String, newAvatar: Bitmap? = null) {
        val localList = this.currentList
        if (localList.isNullOrEmpty()) {
            return
        }

        val changeParticipants = this.currentList.filter { it.peerId == peerId }
        if (changeParticipants.isNullOrEmpty()) {
            return
        }
        changeParticipants.forEach { participant ->
            val index = localList.indexOf(participant)
            if (index < 0 || participant == null) {
                return
            }

            when(type){
                NAME_CHANGE -> participant.name = newName
                AVATAR_CHANGE -> participant.avatar = newAvatar
            }

            notifyItemChanged(index)
        }
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
     * @param participantModerator new permission
     */
    fun updateParticipantPermission(peerId: Long, clientId: Long, participantModerator: Boolean){
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
        participant.isModerator = participantModerator
        val index = localList.indexOf(participant)
        notifyItemChanged(index, participant)
    }

    companion object {
        const val MIC = 0
        const val CAM = 1
        const val MODERATOR = 2
    }
}
