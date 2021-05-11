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

    fun getHolder(position: Int): VideoMeetingViewHolder? {
        listView.let { recyclerview ->
            recyclerview.findViewHolderForAdapterPosition(position)?.let {
                return it as VideoMeetingViewHolder
            }
        }

        return null
    }

    /**
     * Update participant privileges
     *
     * @param participant
     */
    fun updateParticipantPrivileges(participant: Participant) {
        val position = getParticipantPosition(participant.peerId, participant.clientId)
        getHolder(position)?.let {
            it.updatePrivilegeIcon(participant)
            return
        }

        notifyItemChanged(position)
    }

    /**
     * Update participant when the name is changed
     *
     * @param participant
     */
    fun updateName(participant: Participant) {
        val position = getParticipantPosition(participant.peerId, participant.clientId)
        getHolder(position)?.let {
            it.updateName(participant)
            return
        }

        notifyItemChanged(position)
    }

    /**
     * Update participant video resolution
     *
     * @param participant
     */
    fun updateRemoteResolution(participant: Participant) {
        val position = getParticipantPosition(participant.peerId, participant.clientId)
        getHolder(position)?.let {
            it.updateResolution(participant)
            return
        }

        notifyItemChanged(position)
    }

    /**
     * Update participant that is speaking
     *
     * @param participant
     */
    fun updatePeerSelected(participant: Participant) {
        val position = getParticipantPosition(participant.peerId, participant.clientId)
        getHolder(position)?.let {
            it.updatePeerSelected(participant)
            return
        }

        notifyItemChanged(position)
    }

    /**
     * Update participant resolution
     *
     * @param participant
     */
    fun updateParticipantRes(participant: Participant) {
        val position = getParticipantPosition(participant.peerId, participant.clientId)
        getHolder(position)?.let {
            it.updateRes(participant)
            return
        }

        notifyItemChanged(position)
    }

    /**
     * Update participant audio or video flags
     *
     * @param participant
     */
    fun updateParticipantAudioVideo(typeChange: Int, participant: Participant) {
        val position = getParticipantPosition(participant.peerId, participant.clientId)
        getHolder(position)?.let {
            if (typeChange == Constants.TYPE_VIDEO) {
                it.checkVideOn(participant)
            }
            if (typeChange == Constants.TYPE_AUDIO) {
                it.updateAudioIcon(participant)
            }
            return
        }

        notifyItemChanged(position)
    }

    /**
     * Update participant on hold session
     *
     * @param participant
     * @param isOnHold True, it it's. False, otherwise.
     */
    fun updateSessionOnHold(participant: Participant, isOnHold: Boolean) {
        val position = getParticipantPosition(participant.peerId, participant.clientId)
        getHolder(position)?.let {
            it.updateSessionOnHold(participant, isOnHold)
            return
        }

        notifyItemChanged(position)
    }

    /**
     * Update participant when call is on hold
     *
     * @param participant
     * @param isOnHold True, it it's. False, otherwise.
     */
    fun updateCallOnHold(participant: Participant, isOnHold: Boolean) {
        val position = getParticipantPosition(participant.peerId, participant.clientId)
        getHolder(position)?.let {
            it.updateCallOnHold(participant, isOnHold)
            return
        }

        notifyItemChanged(position)
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    /**
     * Resets the parameters of the participant video.
     *
     * @param participant   Participant
     */
    fun removeSurfaceView(participant: Participant) {
        val position = getParticipantPosition(participant.peerId, participant.clientId)
        getHolder(position)?.let {
            it.removeSurfaceView(participant)
            return
        }

        val iterator = currentList.iterator()
        iterator.forEach { participant ->
            inMeetingViewModel.onCloseVideo(participant)
        }
    }
}
