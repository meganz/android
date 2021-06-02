package mega.privacy.android.app.meeting.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import mega.privacy.android.app.components.CustomizedGridCallRecyclerView
import mega.privacy.android.app.databinding.ItemParticipantVideoBinding
import mega.privacy.android.app.meeting.fragments.InMeetingViewModel
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.LogUtil.logDebug

class VideoGridViewAdapter(
    private val inMeetingViewModel: InMeetingViewModel,
    private val gridView: CustomizedGridCallRecyclerView,
    private val screenWidth: Int,
    private val screenHeight: Int,
    private var pagePosition: Int,
    private val orientation: Int,
) : ListAdapter<Participant, VideoMeetingViewHolder>(ParticipantDiffCallback()) {

    private fun getParticipantPosition(peerId: Long, clientId: Long) =
        currentList.indexOfFirst { it.peerId == peerId && it.clientId == clientId }

    override fun onBindViewHolder(gridHolder: VideoMeetingViewHolder, position: Int) {
        logDebug("Bind view holder position $position")
        gridHolder.bind(inMeetingViewModel, getItem(position), itemCount, pagePosition == 0)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoMeetingViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return VideoMeetingViewHolder(
            ItemParticipantVideoBinding.inflate(inflater, parent, false),
            screenWidth,
            screenHeight,
            orientation,
            true,
            null
        )
    }

    fun getHolder(position: Int): VideoMeetingViewHolder? {
        gridView.let { recyclerview ->
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
        if (position == INVALID_POSITION)
            return

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
        if (position == INVALID_POSITION)
            return

        getHolder(position)?.let {
            it.updateName(participant)
            return
        }

        notifyItemChanged(position)
    }

    /**
     * Method to activate or stop a participant's video whether it is visible or not
     *
     * @param shouldActivate True, if video should be activated. False, otherwise.
     * @param participant
     */
    fun updateVideoWhenScroll(shouldActivate: Boolean, participant: Participant) {
        val position = getParticipantPosition(participant.peerId, participant.clientId)
        if (position == INVALID_POSITION)
            return

        getHolder(position)?.let {
            when {
                shouldActivate -> {
                    it.checkVideoOn(participant)
                }
                else -> {
                    it.closeVideo(participant)
                }
            }

            return
        }

        notifyItemChanged(position)
    }

    /**
     * Update participant audio or video flags
     *
     * @param typeChange TYPE_VIDEO or TYPE_AUDIO
     * @param participant
     */
    fun updateParticipantAudioVideo(typeChange: Int, participant: Participant) {
        val position = getParticipantPosition(participant.peerId, participant.clientId)
        if (position == INVALID_POSITION)
            return

        getHolder(position)?.let {
            when (typeChange) {
                TYPE_VIDEO -> {
                    it.checkVideoOn(participant)
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
        if (position == INVALID_POSITION)
            return

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
        if (position == INVALID_POSITION)
            return

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
    fun removeTextureView(participant: Participant) {
        val position = getParticipantPosition(participant.peerId, participant.clientId)
        if (position == INVALID_POSITION)
            return

        getHolder(position)?.let { holder ->
            holder.removeTextureView(participant)
            return
        }
    }

    companion object {
        private var INVALID_POSITION = -1
    }
}

