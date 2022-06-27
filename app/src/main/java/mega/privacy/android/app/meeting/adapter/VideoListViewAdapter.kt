package mega.privacy.android.app.meeting.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.databinding.ItemParticipantVideoBinding
import mega.privacy.android.app.meeting.MegaSurfaceRenderer
import mega.privacy.android.app.meeting.fragments.InMeetingViewModel
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.INVALID_POSITION
import timber.log.Timber

class VideoListViewAdapter(
    private val inMeetingViewModel: InMeetingViewModel,
    private val listView: RecyclerView,
    private val listenerRenderer: MegaSurfaceRenderer.MegaSurfaceRendererListener?,
) : ListAdapter<Participant, VideoMeetingViewHolder>(ParticipantDiffCallback()) {

    override fun onViewRecycled(holder: VideoMeetingViewHolder) {
        super.onViewRecycled(holder)

        if (holder.bindingAdapterPosition == INVALID_POSITION)
            return

        val manager: RecyclerView.LayoutManager? = listView.layoutManager
        if (manager is LinearLayoutManager) {
            val firstPositionVisible = manager.findFirstVisibleItemPosition()
            val lastPositionVisible = manager.findLastVisibleItemPosition() + 1
            if (!currentList.isNullOrEmpty()) {
                val iterator = currentList.iterator()
                iterator.forEach { participant ->
                    val position = getParticipantPosition(participant.peerId, participant.clientId)
                    if (position == holder.bindingAdapterPosition) {
                        if (position < firstPositionVisible || position > lastPositionVisible) {
                            holder.onRecycle()
                            return@forEach
                        }
                    }
                }
            }
        }
    }

    private fun getParticipantPosition(peerId: Long, clientId: Long) =
        currentList.indexOfFirst { it.peerId == peerId && it.clientId == clientId }

    override fun onBindViewHolder(holder: VideoMeetingViewHolder, position: Int) {
        Timber.d("Bind view holder position $position")
        holder.bind(inMeetingViewModel, getItem(position), itemCount, true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoMeetingViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return VideoMeetingViewHolder(
            ItemParticipantVideoBinding.inflate(inflater, parent, false),
            0,
            0,
            false,
            listenerRenderer,
            null
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
     * @param participant Participant to update
     */
    fun updateParticipantPrivileges(participant: Participant) {
        val position = getParticipantPosition(participant.peerId, participant.clientId)
        if (position == INVALID_POSITION)
            return

        getHolder(position)?.let {
            it.updatePrivilegeIcon(participant)
            return
        }

        listView.recycledViewPool.clear()
        notifyItemChanged(position)
    }

    /**
     * Update participant when the name is changed
     *
     * @param participant Participant to update
     * @param typeChange the type of change, name or avatar
     */
    fun updateNameOrAvatar(participant: Participant, typeChange: Int) {
        val position = getParticipantPosition(participant.peerId, participant.clientId)
        if (position == INVALID_POSITION)
            return

        getHolder(position)?.let {
            it.updateNameOrAvatar(participant, typeChange)
            return
        }

        listView.recycledViewPool.clear()
        notifyItemChanged(position)
    }

    /**
     * Update participant that is speaking
     *
     * @param participant Participant to update
     */
    fun updatePeerSelected(participant: Participant) {
        val position = getParticipantPosition(participant.peerId, participant.clientId)
        if (position == INVALID_POSITION)
            return

        getHolder(position)?.let {
            it.updatePeerSelected(participant)
            return
        }

        listView.recycledViewPool.clear()
        notifyItemChanged(position)
    }

    /**
     * Update participant audio or video flags
     *
     * @param participant Participant to update
     */
    fun updateParticipantAudioVideo(typeChange: Int, participant: Participant) {
        val position = getParticipantPosition(participant.peerId, participant.clientId)
        if (position == INVALID_POSITION)
            return

        getHolder(position)?.let {
            when (typeChange) {
                Constants.TYPE_VIDEO -> it.checkVideoOn(participant)
                Constants.TYPE_AUDIO -> it.updateAudioIcon(participant)
            }
            return
        }

        listView.recycledViewPool.clear()
        notifyItemChanged(position)
    }

    /**
     * Update participant on hold session
     *
     * @param participant Participant to update
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

        listView.recycledViewPool.clear()
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

        getHolder(position)?.let {
            it.updateListener(participant, shouldAddListener, isHiRes)
            return
        }

        listView.recycledViewPool.clear()
        notifyItemChanged(position)
    }

    /**
     * Update participant when call is on hold
     *
     * @param participant Participant to update
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

        listView.recycledViewPool.clear()
        notifyItemChanged(position)
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    /**
     * Remove the texture view of a participant
     *
     * @param participant Participant to update
     */
    fun removeTextureView(participant: Participant) {
        val position = getParticipantPosition(participant.peerId, participant.clientId)
        if (position == INVALID_POSITION)
            return

        getHolder(position)?.removeResolutionAndListener(participant)
    }
}