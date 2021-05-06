package mega.privacy.android.app.meeting.adapter

import android.graphics.Rect
import android.view.SurfaceHolder
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.databinding.ItemParticipantVideoBinding
import mega.privacy.android.app.meeting.TestTool.showHide
import mega.privacy.android.app.meeting.fragments.InMeetingViewModel
import mega.privacy.android.app.meeting.listeners.MeetingVideoListener
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.Util
import nz.mega.sdk.MegaApiAndroid
import org.jetbrains.anko.displayMetrics
import javax.inject.Inject

class VideoListViewHolder(
    private val binding: ItemParticipantVideoBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

    @Inject
    lateinit var megaApi: MegaApiAndroid

    lateinit var inMeetingViewModel: InMeetingViewModel

    lateinit var holder: SurfaceHolder

    var isDrawing = true

    private val srcRect = Rect()
    private val dstRect = Rect()

    fun bind(
        inMeetingViewModel: InMeetingViewModel,
        participant: Participant
    ) {
        this.inMeetingViewModel = inMeetingViewModel

        val layoutParams = binding.root.layoutParams
        layoutParams.width = Util.dp2px(ITEM_WIDTH)
        layoutParams.height = Util.dp2px(ITEM_HEIGHT)

        binding.name.isVisible = false

        binding.root.setOnClickListener {
            inMeetingViewModel.onItemClick(participant)
            binding.selectedForeground.showHide()
        }

        initAvatar(participant)
        initStatus(participant)

        holder = binding.video.holder
    }

    fun updateRes(participant: Participant) {
        if (participant.isVideoOn) {
            inMeetingViewModel.onChangeResolution(participant)
        }
    }

    fun updatePrivilegeIcon(participant: Participant) {
        binding.moderatorIcon.isVisible = participant.isModerator
    }

    fun updateAudioIcon(participant: Participant) {
        binding.muteIcon.isVisible = !participant.isAudioOn
    }

    fun updateCallOnHold(participant: Participant, isCallOnHold: Boolean) {
        if (isCallOnHold) {
            binding.avatar.alpha = 0.5f
            showAvatar(participant)
        } else {
            binding.avatar.alpha = 1f
            if (participant.isVideoOn) {
                activateVideo(participant)
            } else {
                showAvatar(participant)
            }
        }
    }

    fun updateSessionOnHold(participant: Participant, isSessionOnHold: Boolean) {
        if (isSessionOnHold) {
            binding.onHoldIcon.isVisible = true
            binding.avatar.alpha = 0.5f
            showAvatar(participant)
        } else {
            binding.onHoldIcon.isVisible = false
            binding.avatar.alpha = 1f
            if (participant.isVideoOn) {
                activateVideo(participant)
            } else {
                showAvatar(participant)
            }
        }
    }

    fun updateVideo(participant: Participant) {
        when {
            participant.isVideoOn -> {
                activateVideo(participant)
            }
            else -> {
                showAvatar(participant)
            }
        }
    }

    private fun initAvatar(participant: Participant) {
        inMeetingViewModel.getChat()?.let {
            var avatar = CallUtil.getImageAvatarCall(it, participant.peerId)
            if (avatar == null) {
                avatar = CallUtil.getDefaultAvatarCall(
                    MegaApplication.getInstance().applicationContext,
                    participant.peerId
                )
            }

            binding.avatar.setImageBitmap(avatar)
        }
    }

    private fun initStatus(participant: Participant) {
        val session = inMeetingViewModel.getSession(participant.peerId)
        val call = inMeetingViewModel.getCall()
        call?.let {
            if (participant.isVideoOn && !it.isOnHold && (session == null || !session.isOnHold)) {
                activateVideo(participant)
            } else {
                showAvatar(participant)
            }

            updateAudioIcon(participant)
            updatePrivilegeIcon(participant)
        }
    }

    private fun showAvatar(participant: Participant) {
        binding.avatar.isVisible = true
        closeVideo(participant)
    }

    /**
     * Method for activating the video.
     */
    private fun activateVideo(participant: Participant) {
        binding.avatar.isVisible = false

        if (participant.videoListener == null) {
            var vListener = MeetingVideoListener(
                binding.video,
                MegaApplication.getInstance().applicationContext.displayMetrics,
                participant.clientId,
                false
            )

            participant.videoListener = vListener

            inMeetingViewModel.onActivateVideo(participant)
        }

        binding.video.isVisible = true
    }

    /**
     * Method to close Video
     */
    private fun closeVideo(participant: Participant) {
        binding.video.isVisible = false
        inMeetingViewModel.onCloseVideo(participant)
    }

    fun onRecycle() {
        isDrawing = false
    }

    companion object {

        const val ITEM_WIDTH = 90f
        const val ITEM_HEIGHT = 90f
    }
}
