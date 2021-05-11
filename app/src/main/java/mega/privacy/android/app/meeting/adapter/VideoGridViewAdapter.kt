package mega.privacy.android.app.meeting.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import mega.privacy.android.app.components.CustomizedGridCallRecyclerView
import mega.privacy.android.app.databinding.ItemParticipantVideoBinding
import mega.privacy.android.app.meeting.MegaSurfaceRenderer
import mega.privacy.android.app.meeting.fragments.InMeetingViewModel
import mega.privacy.android.app.utils.Constants.TYPE_AUDIO
import mega.privacy.android.app.utils.Constants.TYPE_VIDEO
import mega.privacy.android.app.utils.LogUtil

class VideoGridViewAdapter(
    private val inMeetingViewModel: InMeetingViewModel,
    private val gridView: CustomizedGridCallRecyclerView,
    private val screenWidth: Int,
    private val screenHeight: Int,
    private val pagePosition: Int,
    private val orientation: Int
) : ListAdapter<Participant, VideoMeetingViewHolder>(ParticipantDiffCallback()){

    private fun getParticipantPosition(peerId: Long, clientId: Long) =
        currentList.indexOfFirst { it.peerId == peerId && it.clientId == clientId }

    override fun onBindViewHolder(gridHolder: VideoMeetingViewHolder, position: Int) {
        gridHolder.bind(inMeetingViewModel, getItem(position), itemCount, pagePosition == 0)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoMeetingViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return VideoMeetingViewHolder(
            ItemParticipantVideoBinding.inflate(inflater, parent, false),
            screenWidth,
            screenHeight,
            orientation,
            true
        )
    }

    fun getHolder(position: Int): VideoMeetingViewHolder? {
        gridView?.let { recyclerview ->
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
     * Update participant name
     *
     * @param participant
     */
    fun updateParticipantName(participant: Participant) {
        val position = getParticipantPosition(participant.peerId, participant.clientId)
        getHolder(position)?.let {
            it.updateName(participant)
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
     * Update participant audio or video flags
     *
     * @param participant
     */
    fun updateParticipantAudioVideo(typeChange: Int, participant: Participant) {
        val position = getParticipantPosition(participant.peerId, participant.clientId)
        getHolder(position)?.let {
            when (typeChange) {
                TYPE_VIDEO -> {
                    it.checkVideOn(participant)
                }
                TYPE_AUDIO -> {
                    it.updateAudioIcon(participant)
                }
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

    /**
     * Resets the parameters of the participant video.
     *
     * @param participant
     */
    fun closeAllVideos(participant: Participant) {
        val position = getParticipantPosition(participant.peerId, participant.clientId)
        getHolder(position)?.let {
            it.closeAllVideos(participant)
            return
        }

        val iterator = currentList.iterator()
        iterator.forEach { participant ->
            inMeetingViewModel.onCloseVideo(participant)
        }
    }
}

