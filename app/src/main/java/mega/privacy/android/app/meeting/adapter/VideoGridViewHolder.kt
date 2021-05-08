package mega.privacy.android.app.meeting.adapter

import android.content.res.Configuration
import android.view.SurfaceHolder
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.databinding.ItemParticipantVideoBinding
import mega.privacy.android.app.meeting.fragments.InMeetingViewModel
import mega.privacy.android.app.meeting.listeners.MeetingVideoListener
import mega.privacy.android.app.utils.Util.dp2px
import nz.mega.sdk.MegaApiAndroid
import org.jetbrains.anko.displayMetrics
import javax.inject.Inject

/**
 * When use DataBinding here, when user fling the RecyclerView, the bottom sheet will have
 * extra top offset. Not use DataBinding could avoid this bug.
 */
class VideoGridViewHolder(
    private val binding: ItemParticipantVideoBinding,
    private val screenWidth: Int,
    private val screenHeight: Int,
    private val orientation: Int
) : RecyclerView.ViewHolder(binding.root) {

    @Inject
    lateinit var megaApi: MegaApiAndroid

    lateinit var inMeetingViewModel: InMeetingViewModel

    lateinit var holder: SurfaceHolder

    private val SIZE_AVATAR = 88

    fun bind(
        inMeetingViewModel: InMeetingViewModel,
        participant: Participant,
        itemCount: Int,
        isFirstPage: Boolean
    ) {
        this.inMeetingViewModel = inMeetingViewModel

        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            portraitLayout(isFirstPage, itemCount)
        } else {
            landscapeLayout(isFirstPage, itemCount)
        }

        binding.name.text = participant.name

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
            dp2px(SIZE_AVATAR.toFloat(), MegaApplication.getInstance().displayMetrics)
        paramsAvatar.height =
            dp2px(SIZE_AVATAR.toFloat(), MegaApplication.getInstance().displayMetrics)
        binding.avatar.layoutParams = paramsAvatar

        val paramsOnHoldIcon = binding.onHoldIcon.layoutParams
        paramsOnHoldIcon.width =
            dp2px(SIZE_AVATAR.toFloat(), MegaApplication.getInstance().displayMetrics)
        paramsOnHoldIcon.height =
            dp2px(SIZE_AVATAR.toFloat(), MegaApplication.getInstance().displayMetrics)
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
        val session = inMeetingViewModel.getSession(participant.clientId)
        val call = inMeetingViewModel.getCall()
        call?.let {
            if (participant.isVideoOn && !it.isOnHold && (session == null || !session.isOnHold)) {
                activateVideo(participant)
            } else {
                showAvatar(participant)
                session?.let { session ->
                    checkOnHold(it.isOnHold, session.isOnHold)
                }
            }

            updateAudioIcon(participant)
            updatePrivilegeIcon(participant)
        }
    }
    private fun checkOnHold(isCallOnHold: Boolean, isSessionOnHold: Boolean){
        if(isSessionOnHold){
            binding.onHoldIcon.isVisible = true
            binding.avatar.alpha = 0.5f
        }else{
            binding.onHoldIcon.isVisible = false
            if(isCallOnHold){
                binding.avatar.alpha = 0.5f
            }else{
                binding.avatar.alpha = 1f
            }
        }

    }
    /**
     * Update resolution
     *
     * @param participant
     */
    fun updateRes(participant: Participant) {
        if (participant.isVideoOn) {
            inMeetingViewModel.onChangeResolution(participant)
        }
    }

    /**
     * Update name and avatar
     *
     * @param participant
     */
    fun updateName(participant: Participant) {
        binding.name.text = participant.name

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

    /**
     * Check when the session is on hold or not
     *
     * @param participant
     */
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

    private fun landscapeLayout(isFirstPage: Boolean, itemCount: Int) {
        var w = 0
        var h = 0

        val layoutParams: GridLayoutManager.LayoutParams =
            binding.root.layoutParams as GridLayoutManager.LayoutParams

        if (isFirstPage) {
            when (itemCount) {
                1 -> {
                    w = screenWidth / 2
                    h = screenHeight
                    layoutParams.setMargins(w / 2, 0, w / 2, 0)
                }
                2 -> {
                    w = screenWidth / 2
                    h = screenHeight
                    layoutParams.setMargins(0, 0, 0, 0)
                }
                3 -> {
                    w = (screenWidth / 3)
                    h = (screenHeight * 0.6).toInt()
                    layoutParams.setMargins(0, 0, 0, screenHeight - h)
                }
                5 -> {
                    w = screenWidth / 4
                    h = screenHeight / 2

                    when (adapterPosition) {
                        3, 4 -> layoutParams.setMargins(w / 2, 0, 0, 0)
                    }
                }
                4, 6 -> {
                    w = (screenWidth / 4)
                    h = (screenHeight / 2)
                }
            }
        } else {
            w = (screenWidth / 4)
            h = (screenHeight / 2)
        }

        layoutParams.width = w
        layoutParams.height = h
    }

    private fun portraitLayout(isFirstPage: Boolean, itemCount: Int) {
        var w = 0
        var h = 0

        val layoutParams: GridLayoutManager.LayoutParams =
            binding.root.layoutParams as GridLayoutManager.LayoutParams

        val verticalMargin = ((screenHeight - screenWidth / 2 * 3) / 2)

        if (isFirstPage) {
            when (itemCount) {
                1 -> {
                    w = screenWidth
                    h = screenHeight
                    layoutParams.setMargins(0, 0, 0, 0)
                }
                2 -> {
                    w = screenWidth
                    h = screenHeight / 2
                    layoutParams.setMargins(0, 0, 0, 0)
                }
                3 -> {
                    w = (screenWidth * 0.8).toInt()
                    h = screenHeight / 3
                    layoutParams.setMargins((screenWidth - w) / 2, 0, (screenWidth - w) / 2, 0)
                }
                5 -> {
                    w = screenWidth / 2
                    h = w

                    when (adapterPosition) {
                        0, 1 -> {
                            layoutParams.setMargins(0, verticalMargin, 0, 0)
                        }
                        4 -> {
                            layoutParams.setMargins(
                                (screenWidth - w) / 2,
                                0,
                                (screenWidth - w) / 2,
                                0
                            )
                        }
                        else -> {
                            layoutParams.setMargins(0, 0, 0, 0)
                        }
                    }
                }
                4, 6 -> {
                    val pair = layout46(layoutParams, verticalMargin)
                    h = pair.first
                    w = pair.second
                }
            }
        } else {
            val pair = layout46(layoutParams, verticalMargin)
            h = pair.first
            w = pair.second
        }

        layoutParams.width = w
        layoutParams.height = h
    }

    private fun layout46(
        layoutParams: GridLayoutManager.LayoutParams,
        verticalMargin: Int
    ): Pair<Int, Int> {
        val w = screenWidth / 2
        when (adapterPosition) {
            0, 1 -> {
                layoutParams.setMargins(0, verticalMargin, 0, 0)
            }
            else -> {
                layoutParams.setMargins(0, 0, 0, 0)
            }
        }
        return Pair(w, w)
    }
}
