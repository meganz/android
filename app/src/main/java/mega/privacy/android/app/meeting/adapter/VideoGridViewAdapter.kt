package mega.privacy.android.app.meeting.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import mega.privacy.android.app.components.CustomizedGridCallRecyclerView
import mega.privacy.android.app.databinding.ItemParticipantVideoBinding
import mega.privacy.android.app.meeting.fragments.InMeetingViewModel
import mega.privacy.android.app.meeting.listeners.GridViewListener
import mega.privacy.android.app.utils.Constants.TYPE_AUDIO
import mega.privacy.android.app.utils.Constants.TYPE_VIDEO
import mega.privacy.android.app.utils.LogUtil.logDebug

class VideoGridViewAdapter(
    private val inMeetingViewModel: InMeetingViewModel,
    private val gridView: CustomizedGridCallRecyclerView,
    private val screenWidth: Int,
    private val screenHeight: Int,
    private val pagePosition: Int,
    private val listener: GridViewListener,
    private val orientation: Int
) : ListAdapter<Participant, VideoGridViewHolder>(ParticipantDiffCallback()) {

    private fun getParticipantPosition(peerId: Long, clientId: Long) =
        currentList.indexOfFirst { it.peerId == peerId && it.clientId == clientId }

    override fun onBindViewHolder(gridHolder: VideoGridViewHolder, position: Int) {
        gridHolder.bind(inMeetingViewModel, getItem(position), itemCount, pagePosition == 0)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoGridViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return VideoGridViewHolder(
            ItemParticipantVideoBinding.inflate(inflater, parent, false),
            gridView,
            screenWidth,
            screenHeight,
            listener, orientation
        )
    }

    fun getHolder(position: Int): VideoGridViewHolder {
        return gridView.findViewHolderForAdapterPosition(position) as VideoGridViewHolder
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
     * Update participant name
     *
     * @param participant
     */
    fun updateParticipantName(participant: Participant) {
        val position = getParticipantPosition(participant.peerId, participant.clientId)
        getHolder(position).updateName(participant)
    }

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
            if(typeChange == TYPE_VIDEO){
                it.updateVideo(participant)
            }else if(typeChange ==  TYPE_AUDIO){
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

