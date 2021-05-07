package mega.privacy.android.app.meeting.adapter

import android.graphics.Rect
import android.view.SurfaceHolder
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.ItemParticipantVideoBinding
import mega.privacy.android.app.meeting.fragments.InMeetingViewModel
import mega.privacy.android.app.meeting.listeners.MeetingVideoListener
import mega.privacy.android.app.utils.LogUtil.logDebug
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

    private val SIZE_AVATAR = 60

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
        }

        initAvatar(participant)
        initStatus(participant)

        holder = binding.video.holder
    }

    /**
     * Initialising the avatar
     *
     * @param participant
     */
    private fun initAvatar(participant: Participant) {
        val paramsAvatar = binding.avatar.layoutParams
        paramsAvatar.width =
            Util.dp2px(SIZE_AVATAR.toFloat(), MegaApplication.getInstance().displayMetrics)
        paramsAvatar.height =
            Util.dp2px(SIZE_AVATAR.toFloat(), MegaApplication.getInstance().displayMetrics)
        binding.avatar.layoutParams = paramsAvatar

        val paramsOnHoldIcon = binding.onHoldIcon.layoutParams
        paramsOnHoldIcon.width =
            Util.dp2px(SIZE_AVATAR.toFloat(), MegaApplication.getInstance().displayMetrics)
        paramsOnHoldIcon.height =
            Util.dp2px(SIZE_AVATAR.toFloat(), MegaApplication.getInstance().displayMetrics)
        binding.onHoldIcon.layoutParams = paramsOnHoldIcon

        inMeetingViewModel.getAvatarBitmap(participant.peerId)?.let {
            binding.avatar.setImageBitmap(it)
        }
    }

    /**
     * Check the initial UI
     *
     * @param participant
     */
    private fun initStatus(participant: Participant) {
        val session = inMeetingViewModel.getSession(participant.peerId)
        val call = inMeetingViewModel.getCall()
        call?.let {
            when {
                participant.isVideoOn && !it.isOnHold && (session == null || !session.isOnHold) -> {
                    activateVideo(participant)
                }
                else -> {
                    showAvatar(participant)
                }
            }

            updateAudioIcon(participant)
            updatePrivilegeIcon(participant)
        }
    }

    /**
     * Update resolution
     *
     * @param participant
     */
    fun updateRes(participant: Participant) {
        when {
            participant.isVideoOn -> {
                inMeetingViewModel.onChangeResolution(participant)
            }
        }
    }

    /**
     * Update avatar
     *
     * @param participant
     */
    fun updateAvatar(participant: Participant) {
        inMeetingViewModel.getAvatarBitmap(participant.peerId)?.let {
            binding.avatar.setImageBitmap(it)
        }
    }

    /**
     * Update privileges
     *
     * @param participant
     */
    fun updatePrivilegeIcon(participant: Participant) {
        binding.moderatorIcon.isVisible = participant.isModerator
    }

    /**
     * Check if mute icon should be visible
     *
     * @param participant
     */
    fun updateAudioIcon(participant: Participant) {
        binding.muteIcon.isVisible = !participant.isAudioOn
    }

    /**
     * Check when the call is on hold or not
     *
     * @param participant
     */
    fun updateCallOnHold(participant: Participant, isCallOnHold: Boolean) {
        when {
            isCallOnHold -> {
                binding.avatar.alpha = 0.5f
                showAvatar(participant)
            }
            else -> {
                binding.avatar.alpha = 1f
                when {
                    participant.isVideoOn -> {
                        activateVideo(participant)
                    }
                    else -> {
                        showAvatar(participant)
                    }
                }
            }
        }
    }

    /**
     * Check when the session is on hold or not
     *
     * @param participant
     */
    fun updateSessionOnHold(participant: Participant, isSessionOnHold: Boolean) {
        when {
            isSessionOnHold -> {
                binding.onHoldIcon.isVisible = true
                binding.avatar.alpha = 0.5f
                showAvatar(participant)
            }
            else -> {
                binding.onHoldIcon.isVisible = false
                binding.avatar.alpha = 1f
                when {
                    participant.isVideoOn -> {
                        activateVideo(participant)
                    }
                    else -> {
                        showAvatar(participant)
                    }
                }
            }
        }
    }

    /**
     * Check whether video should be activated or closed.
     *
     * @param participant
     */
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

    /**
     * Show avatar and close video
     *
     * @param participant
     */
    private fun showAvatar(participant: Participant) {
        binding.avatar.isVisible = true
        closeVideo(participant)
    }

    /**
     * Update the peer selected
     *
     * @param participant
     */
    fun updatePeerSelected(participant: Participant) {
        when {
            participant.isSelected -> {
                binding.selectedForeground.background = ContextCompat.getDrawable(
                    binding.root.context,
                    when {
                        inMeetingViewModel.isSpeakerSelectionAutomatic -> R.drawable.border_green_layer
                        else -> R.drawable.border_green_layer_selected
                    }
                )
                binding.selectedForeground.isVisible = true
            }
            else -> {
                binding.selectedForeground.isVisible = false

            }
        }
    }

    /**
     * Method for activating the video.
     */
    private fun activateVideo(participant: Participant) {
        binding.avatar.isVisible = false

        when (participant.videoListener) {
            null -> {
                val vListener = MeetingVideoListener(
                    binding.video,
                    MegaApplication.getInstance().applicationContext.displayMetrics,
                    participant.clientId,
                    false
                )

                participant.videoListener = vListener

                inMeetingViewModel.onActivateVideo(participant)
            }
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
