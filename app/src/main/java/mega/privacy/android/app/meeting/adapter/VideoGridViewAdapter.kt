package mega.privacy.android.app.meeting.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import mega.privacy.android.app.components.CustomizedGridCallRecyclerView
import mega.privacy.android.app.databinding.ItemParticipantVideoBinding
import mega.privacy.android.app.meeting.fragments.InMeetingViewModel
import mega.privacy.android.app.utils.Constants.INVALID_POSITION
import mega.privacy.android.app.utils.Constants.TYPE_AUDIO
import mega.privacy.android.app.utils.Constants.TYPE_VIDEO
import timber.log.Timber

class VideoGridViewAdapter(
    private val inMeetingViewModel: InMeetingViewModel,
    private val gridView: CustomizedGridCallRecyclerView,
    private val screenWidth: Int,
    private val screenHeight: Int,
    private var pagePosition: Int,
    private val onPageClickedCallback: () -> Unit,
) : ListAdapter<Participant, VideoMeetingViewHolder>(ParticipantDiffCallback()) {

    private fun getParticipantPosition(peerId: Long, clientId: Long) =
        currentList.indexOfFirst { it.peerId == peerId && it.clientId == clientId }

    override fun onBindViewHolder(gridHolder: VideoMeetingViewHolder, position: Int) {
        Timber.d("Bind view holder position $position, pagePosition $pagePosition")
        gridHolder.bind(inMeetingViewModel, getItem(position), itemCount, pagePosition == 0)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoMeetingViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return VideoMeetingViewHolder(
            ItemParticipantVideoBinding.inflate(inflater, parent, false),
            screenWidth,
            screenHeight,
            true,
            null,
            onPageClickedCallback
        )
    }

    private fun getHolderAtPosition(position: Int): VideoMeetingViewHolder? {
        gridView.let { recyclerview ->
            recyclerview.findViewHolderForAdapterPosition(position)?.let {
                return it as VideoMeetingViewHolder
            }
        }

        return null
    }

    /**
     * Update participant name
     *
     * @param participant Participant to update
     * @param typeChange the type of change, name or avatar
     */
    fun updateParticipantNameOrAvatar(participant: Participant, typeChange: Int) {
        val position = getParticipantPosition(participant.peerId, participant.clientId)
        if (position == INVALID_POSITION)
            return

        getHolderAtPosition(position)?.let {
            it.updateNameOrAvatar(participant, typeChange)
            return
        }

        notifyItemChanged(position)
    }

    /**
     * Method to activate or stop a participant's video whether it is visible or not
     *
     * @param shouldActivate True, if video should be activated. False, otherwise.
     * @param participant Participant to update
     */
    fun updateVideoWhenScroll(shouldActivate: Boolean, participant: Participant) {
        val position = getParticipantPosition(participant.peerId, participant.clientId)
        if (position == INVALID_POSITION)
            return

        getHolderAtPosition(position)?.let {
            if (shouldActivate) {
                it.checkVideoOn(participant)
            } else {
                it.closeVideo(participant)
            }
            return
        }

        notifyItemChanged(position)
    }

    /**
     * Update participant audio or video flags
     *
     * @param typeChange TYPE_VIDEO or TYPE_AUDIO
     * @param participant Participant to update
     */
    fun updateParticipantAudioVideo(typeChange: Int, participant: Participant) {
        val position = getParticipantPosition(participant.peerId, participant.clientId)
        if (position == INVALID_POSITION)
            return

        getHolderAtPosition(position)?.let {
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
     * @param participant Participant to update
     * @param isOnHold True, if is on hold. False, otherwise.
     */
    fun updateSessionOnHold(participant: Participant, isOnHold: Boolean) {
        val position = getParticipantPosition(participant.peerId, participant.clientId)
        if (position == INVALID_POSITION)
            return

        getHolderAtPosition(position)?.let {
            it.updateSessionOnHold(participant, isOnHold)
            return
        }

        notifyItemChanged(position)
    }

    /**
     * Method to control when the video listener should be added or removed.
     *
     * @param participant The participant whose listener of the video is to be added or deleted
     * @param shouldAddListener True, should add the listener. False, should remove the listener
     * @param isHiRes True, if is High resolution. False, if is Low resolution
     */
    fun updateListener(participant: Participant, shouldAddListener: Boolean, isHiRes: Boolean) {
        val position = getParticipantPosition(participant.peerId, participant.clientId)
        if (position == INVALID_POSITION)
            return

        getHolderAtPosition(position)?.let {
            it.updateListener(participant, shouldAddListener, isHiRes)
            return
        }

        notifyItemChanged(position)
    }

    /**
     * Update participant when call is on hold
     *
     * @param participant Participant to update
     * @param isOnHold True, if is on hold. False, otherwise.
     */
    fun updateCallOnHold(participant: Participant, isOnHold: Boolean) {
        val position = getParticipantPosition(participant.peerId, participant.clientId)
        if (position == INVALID_POSITION)
            return

        getHolderAtPosition(position)?.let {
            it.updateCallOnHold(participant, isOnHold)
            return
        }

        notifyItemChanged(position)
    }

    /**
     * Resets the parameters of the participant video.
     *
     * @param participant Participant to update
     */
    fun removeTextureView(participant: Participant) {
        val position = getParticipantPosition(participant.peerId, participant.clientId)
        if (position == INVALID_POSITION)
            return

        getHolderAtPosition(position)?.let { holder ->
            holder.removeResolutionAndListener(participant)
            return
        }

        participant.videoListener?.let {
            inMeetingViewModel.removeResolutionAndListener(participant, it)
        }
        participant.videoListener = null
    }
}

