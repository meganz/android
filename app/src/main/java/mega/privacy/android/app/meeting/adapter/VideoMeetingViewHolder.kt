package mega.privacy.android.app.meeting.adapter

import android.content.res.Configuration
import android.view.SurfaceHolder
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.ItemParticipantVideoBinding
import mega.privacy.android.app.meeting.MegaSurfaceRenderer
import mega.privacy.android.app.meeting.fragments.InMeetingViewModel
import mega.privacy.android.app.meeting.listeners.MeetingVideoListener
import mega.privacy.android.app.utils.LogUtil
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.LogUtil.logError
import mega.privacy.android.app.utils.Util.dp2px
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import org.jetbrains.anko.displayMetrics
import javax.inject.Inject

/**
 * When use DataBinding here, when user fling the RecyclerView, the bottom sheet will have
 * extra top offset. Not use DataBinding could avoid this bug.
 */
class VideoMeetingViewHolder(
    private val listener: MegaSurfaceRenderer.MegaSurfaceRendererGroupListener,
    private val binding: ItemParticipantVideoBinding,
    private val screenWidth: Int,
    private val screenHeight: Int,
    private val orientation: Int,
    private val isTypeGridViewHolder: Boolean
) : RecyclerView.ViewHolder(binding.root) {

    @Inject
    lateinit var megaApi: MegaApiAndroid

    lateinit var inMeetingViewModel: InMeetingViewModel

    private var isGrid: Boolean = true

    lateinit var holder: SurfaceHolder

    private var SIZE_AVATAR = 88
    private var peerId: Long? = MEGACHAT_INVALID_HANDLE
    private var clientId: Long? = MEGACHAT_INVALID_HANDLE

    var isDrawing = true

    fun bind(
        inMeetingViewModel: InMeetingViewModel,
        participant: Participant,
        itemCount: Int,
        isFirstPage: Boolean
    ) {
        this.isGrid = isTypeGridViewHolder
        this.inMeetingViewModel = inMeetingViewModel

        when {
            participant.peerId == MEGACHAT_INVALID_HANDLE || participant.clientId == MEGACHAT_INVALID_HANDLE -> {
                logError("Error. Peer id or client id invalid")
                return
            }
            else -> {
                this.peerId = participant.peerId
                this.clientId = participant.clientId
                if (isGrid) {
                    SIZE_AVATAR = 88
                    if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                        portraitLayout(isFirstPage, itemCount)
                    } else {
                        landscapeLayout(isFirstPage, itemCount)
                    }

                    binding.name.text = participant.name
                } else {
                    SIZE_AVATAR = 60
                    val layoutParams = binding.root.layoutParams
                    layoutParams.width = dp2px(ITEM_WIDTH)
                    layoutParams.height = dp2px(ITEM_HEIGHT)
                    binding.root.setOnClickListener {
                        inMeetingViewModel.onItemClick(participant)
                    }

                    binding.name.isVisible = false
                }

                initAvatar(participant)
                checkUI(participant)
                holder = binding.video.holder
            }
        }

    }

    /**
     * Initialising the avatar
     *
     * @param participant
     */
    private fun initAvatar(participant: Participant) {
        logDebug("initAvatar")

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

        logDebug("Init avatar")
        binding.avatar.setImageBitmap(participant.avatar)
    }

    /**
     * Initialising the UI
     */
    private fun checkUI(participant: Participant) {
        logDebug("Check the current UI status")
        inMeetingViewModel.getSession(participant.clientId)?.let {
            when {
                it.hasVideo() -> {
                    logDebug("Check if video should be on")
                    checkVideOn(participant)
                }
                else -> {
                    logDebug("Video should be off")
                    videoOffUI(participant)
                }
            }

            updateAudioIcon(participant)
            updatePrivilegeIcon(participant)
            updatePeerSelected(participant)
        }
    }


    /**
     * Show UI when video is on
     *
     * @param participant
     */
    private fun videoOnUI(participant: Participant) {
        when {
            participant.peerId != this.peerId || participant.clientId != this.clientId -> return
            else -> {
                logDebug("UI video on")
                hideAvatar(participant)
                activateVideo(participant)
            }
        }
    }

    /**
     * Method to hide the Avatar
     *
     * @param participant
     */
    private fun hideAvatar(participant: Participant) {
        when {
            participant.peerId != this.peerId || participant.clientId != this.clientId -> return
            else -> {
                logDebug("Hide Avatar")
                binding.onHoldIcon.isVisible = false
                binding.avatar.alpha = 1f
                binding.avatar.isVisible = false
            }
        }
    }

    /**
     * Method for activating the video
     *
     * @param participant
     */
    private fun activateVideo(participant: Participant) {
        when {
            participant.peerId != this.peerId || participant.clientId != this.clientId -> return
            else -> {
                //closeVideo(participant)
                when (participant.videoListener) {
                    null -> {
                        val vListener = MeetingVideoListener(
                            binding.video,
                            MegaApplication.getInstance().applicationContext.displayMetrics,
                            participant.peerId,
                            participant.clientId,
                            false
                        )

                        participant.videoListener = vListener

                        inMeetingViewModel.onActivateVideo(participant)

                            participant.videoListener!!.getLocalRenderer()?.let {
                                it.addListener(listener)
                            }

                    }
                    else -> {
                        logDebug("Video Listener is not null ")
                        participant.videoListener!!.height = 0
                        participant.videoListener!!.width = 0
                    }
                }

                binding.video.isVisible = true
            }
        }
    }

    /**
     * Show UI when video is off
     *
     * @param participant
     */
    private fun videoOffUI(participant: Participant) {
        when {
            participant.peerId != this.peerId || participant.clientId != this.clientId -> return
            else -> {
                logDebug("UI video off")
                showAvatar(participant)
                closeVideo(participant)
                checkOnHold(participant)
            }
        }
    }

    /**
     * Show avatar
     *
     * @param participant
     */
    private fun showAvatar(participant: Participant) {
        when {
            participant.peerId != this.peerId || participant.clientId != this.clientId -> return
            else -> {
                logDebug("Show avatar")
                binding.avatar.isVisible = true
            }
        }
    }

    /**
     * Method to close Video
     *
     * @param participant
     */
    private fun closeVideo(participant: Participant) {
        when {
            participant.peerId != this.peerId || participant.clientId != this.clientId -> return
            else -> {
                logDebug("Close video")
                binding.video.isVisible = false
                inMeetingViewModel.onCloseVideo(participant)
            }
        }
    }

    /**
     * Method to control the Call/Session on hold icon visibility
     *
     * @param participant
     */
    private fun checkOnHold(participant: Participant) {
        when {
            participant.peerId != this.peerId || participant.clientId != this.clientId -> return
            else -> {
                val isCallOnHold = inMeetingViewModel.isCallOnHold()
                val isSessionOnHold = inMeetingViewModel.isSessionOnHold(participant.clientId)
                when {
                    isSessionOnHold -> {
                        logDebug("Show on hold icon participant Name = " + participant.name)
                        binding.onHoldIcon.isVisible = true
                        binding.avatar.alpha = 0.5f
                    }
                    else -> {
                        logDebug("Hide on hold icon")
                        binding.onHoldIcon.isVisible = false
                        when {
                            isCallOnHold -> {
                                binding.avatar.alpha = 0.5f
                            }
                            else -> {
                                binding.avatar.alpha = 1f
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Check if mute icon should be visible
     *
     * @param participant
     */
    fun updateAudioIcon(participant: Participant) {
        when {
            participant.peerId != this.peerId || participant.clientId != this.clientId -> return
            else -> {
                logDebug("Update audio icon")
                binding.muteIcon.isVisible = !participant.isAudioOn
            }
        }
    }

    /**
     * Control when a change is received in the video flag
     *
     * @param participant
     */
    fun checkVideOn(participant: Participant) {
        when {
            participant.peerId != this.peerId || participant.clientId != this.clientId -> return
            else -> {
                when {
                    participant.isVideoOn && !inMeetingViewModel.isCallOrSessionOnHold(participant.clientId) -> {
                        logDebug("Video should be on")
                        videoOnUI(participant)
                    }
                    else -> {
                        logDebug("Video should be off")
                        videoOffUI(participant)
                    }
                }
            }
        }
    }

    /**
     * Update privileges
     *
     * @param participant
     */
    fun updatePrivilegeIcon(participant: Participant) {
        when {
            participant.peerId != this.peerId || participant.clientId != this.clientId -> return
            else -> {
                logDebug("Update privilege icon")
                binding.moderatorIcon.isVisible = participant.isModerator
            }
        }
    }

    /**
     * Update resolution
     *
     * @param participant
     */
    fun updateRes(participant: Participant) {
        when {
            participant.peerId != this.peerId || participant.clientId != this.clientId -> return
            else -> {
                logDebug("Update resolution")
                when {
                    participant.isVideoOn -> {
                        inMeetingViewModel.onChangeResolution(participant)
                    }
                }
            }
        }
    }

    /**
     * Update name and avatar
     *
     * @param participant
     */
    fun updateName(participant: Participant) {
        when {
            participant.peerId != this.peerId || participant.clientId != this.clientId -> return
            else -> {
                logDebug("Update name")
                binding.avatar.setImageBitmap(participant.avatar)

            }
        }
    }

    /**
     * Check changes in call on hold
     *
     * @param participant
     * @param isCallOnHold
     */
    fun updateCallOnHold(participant: Participant, isCallOnHold: Boolean) {
        when {
            participant.peerId != this.peerId || participant.clientId != this.clientId -> return
            else -> {
                when {
                    isCallOnHold -> {
                        logDebug("Call is on hold")
                        videoOffUI(participant)
                    }
                    else -> {
                        logDebug("Call is not on hold")
                        checkVideOn(participant)
                    }
                }
            }
        }
    }

    /**
     * Check changes in session on hold
     *
     * @param participant
     * @param isSessionOnHold
     */
    fun updateSessionOnHold(participant: Participant, isSessionOnHold: Boolean) {
        when {
            participant.peerId != this.peerId || participant.clientId != this.clientId -> return
            else -> {
                if (isSessionOnHold) {
                    logDebug("Session is on hold")
                    videoOffUI(participant)
                } else {
                    logDebug("Session is not on hold")
                    checkVideOn(participant)
                }
            }
        }
    }

    /**
     * Update the peer selected
     *
     * @param participant
     */
    fun updatePeerSelected(participant: Participant) {
        when {
            isGrid || participant.peerId != this.peerId || participant.clientId != this.clientId -> return
            else -> {
                when {
                    participant.isSelected -> {
                        logDebug("Participant is selected")
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
                        logDebug("Participant is not selected")
                        binding.selectedForeground.isVisible = false
                    }
                }
            }
        }
    }

    private fun landscapeLayout(isFirstPage: Boolean, itemCount: Int) {
        if (!isGrid)
            return

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
        if (!isGrid)
            return

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

    fun onRecycle() {
        if (isGrid)
            return

        isDrawing = false
    }

    companion object {

        const val ITEM_WIDTH = 90f
        const val ITEM_HEIGHT = 90f
    }
}
