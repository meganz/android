package mega.privacy.android.app.meeting.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.databinding.ItemParticipantVideoBinding
import mega.privacy.android.app.meeting.fragments.InMeetingViewModel
import mega.privacy.android.app.utils.Constants

class VideoListViewAdapter(
    private val inMeetingViewModel: InMeetingViewModel,
    private val listView: RecyclerView
) : ListAdapter<Participant, VideoMeetingViewHolder>(ParticipantDiffCallback()) {

    override fun onViewRecycled(holder: VideoMeetingViewHolder) {
        super.onViewRecycled(holder)
        holder.onRecycle()
    }

    private fun getParticipantPosition(peerId: Long, clientId: Long) =
        currentList.indexOfFirst { it.peerId == peerId && it.clientId == clientId }

    override fun onBindViewHolder(listHolder: VideoMeetingViewHolder, position: Int) {
        listHolder.bind(inMeetingViewModel, getItem(position), itemCount, true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoMeetingViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return VideoMeetingViewHolder(
            ItemParticipantVideoBinding.inflate(inflater, parent, false),
            0,
            0,
            1,
            false
        )
    }

    fun getHolder(position: Int): VideoMeetingViewHolder {
        return listView.findViewHolderForAdapterPosition(position) as VideoMeetingViewHolder
    }

    /**
     * Update participant privileges
     *
     * @param participant
     */
    fun updateParticipantPrivileges(participant: Participant) {
        val position = getParticipantPosition(participant.peerId, participant.clientId)
        getHolder(position).updatePrivilegeIcon(participant)
    }

    /**
     * Update participant when the name is changed
     *
     * @param participant
     */
    fun updateName(participant: Participant) {
        val position = getParticipantPosition(participant.peerId, participant.clientId)
        getHolder(position).updateName(participant)
    }

    /**
     * Update participant that is speaking
     *
     * @param participant
     */
    fun updatePeerSelected(participant: Participant) {
        val position = getParticipantPosition(participant.peerId, participant.clientId)
        getHolder(position).updatePeerSelected(participant)
    }

    /**
     * Update participant resolution
     *
     * @param participant
     */
    fun updateParticipantRes(participant: Participant) {
        val position = getParticipantPosition(participant.peerId, participant.clientId)
        getHolder(position).updateRes(participant)
    }

    /**
     * Update participant audio or video flags
     *
     * @param participant
     */
    fun updateParticipantAudioVideo(typeChange: Int, participant: Participant) {
        val position = getParticipantPosition(participant.peerId, participant.clientId)
        getHolder(position).let {
            if (typeChange == Constants.TYPE_VIDEO) {
                it.checkVideOn(participant)
            }
            if (typeChange == Constants.TYPE_AUDIO) {
                it.updateAudioIcon(participant)
            }
        }
    }

    /**
     * Update participant on hold session
     *
     * @param participant
     * @param isOnHold True, it it's. False, otherwise.
     */
    fun updateSessionOnHold(participant: Participant, isOnHold: Boolean) {
        val position = getParticipantPosition(participant.peerId, participant.clientId)
        getHolder(position).updateSessionOnHold(participant, isOnHold)
    }

    /**
     * Update participant when call is on hold
     *
     * @param participant
     * @param isOnHold True, it it's. False, otherwise.
     */
    fun updateCallOnHold(participant: Participant, isOnHold: Boolean) {
        val position = getParticipantPosition(participant.peerId, participant.clientId)
        getHolder(position).updateCallOnHold(participant, isOnHold)
    }
}
