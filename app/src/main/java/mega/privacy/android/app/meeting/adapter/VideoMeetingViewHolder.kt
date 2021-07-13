package mega.privacy.android.app.meeting.adapter

import android.content.res.Configuration
import android.view.TextureView
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.ItemParticipantVideoBinding
import mega.privacy.android.app.meeting.MegaSurfaceRenderer
import mega.privacy.android.app.meeting.fragments.InMeetingViewModel
import mega.privacy.android.app.meeting.listeners.GroupVideoListener
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.LogUtil.logError
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.Util.dp2px
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import javax.inject.Inject

/**
 * When use DataBinding here, when user fling the RecyclerView, the bottom sheet will have
 * extra top offset. Not use DataBinding could avoid this bug.
 */
class VideoMeetingViewHolder(
    private val binding: ItemParticipantVideoBinding,
    private val screenWidth: Int,
    private val screenHeight: Int,
    private val isGrid: Boolean,
    private val listenerRenderer: MegaSurfaceRenderer.MegaSurfaceRendererListener?,
    private val onPageClickedCallback: (() -> Unit)?
) : RecyclerView.ViewHolder(binding.root) {

    @Inject
    lateinit var megaApi: MegaApiAndroid

    lateinit var inMeetingViewModel: InMeetingViewModel

    private var avatarSize = BIG_AVATAR
    private var peerId = MEGACHAT_INVALID_HANDLE
    private var clientId = MEGACHAT_INVALID_HANDLE

    private var isDrawing = true

    fun bind(
        inMeetingViewModel: InMeetingViewModel,
        participant: Participant,
        itemCount: Int,
        isFirstPage: Boolean
    ) {
        this.inMeetingViewModel = inMeetingViewModel

        if (participant.peerId == MEGACHAT_INVALID_HANDLE || participant.clientId == MEGACHAT_INVALID_HANDLE) {
            logError("Error. Peer id or client id invalid")
            return
        }

        peerId = participant.peerId
        clientId = participant.clientId

        if (isGrid) {
            avatarSize = BIG_AVATAR

            if (Util.getCurrentOrientation() == Configuration.ORIENTATION_PORTRAIT) {
                portraitLayout(isFirstPage, itemCount)
            } else {
                landscapeLayout(isFirstPage, itemCount)
            }
            binding.root.setOnClickListener {
                onPageClickedCallback?.invoke()
            }

            binding.name.text = participant.name
        } else {
            avatarSize = SMALL_AVATAR
            val layoutParams = binding.root.layoutParams
            layoutParams.width = dp2px(ITEM_WIDTH)
            layoutParams.height = dp2px(ITEM_HEIGHT)
            binding.root.setOnClickListener {
                inMeetingViewModel.onItemClick(participant)
            }

            binding.name.isVisible = false
        }

        initAvatar(participant)

        if (!isGrid) {
            inMeetingViewModel.addParticipantVisible(participant)
        }

        if (isGrid || isDrawing) {
            logDebug("Remove the video initially")
            inMeetingViewModel.onCloseVideo(participant)
            removeTextureView(participant)
        }

        checkUI(participant)
    }

    /**
     * Initialising the avatar of the participant
     *
     * @param participant Participant of whom the avatar has to be initialized.
     */
    private fun initAvatar(participant: Participant) {
        val paramsAvatar = binding.avatar.layoutParams
        paramsAvatar.width = dp2px(avatarSize.toFloat())
        paramsAvatar.height = dp2px(avatarSize.toFloat())
        binding.avatar.layoutParams = paramsAvatar

        val paramsOnHoldIcon = binding.onHoldIcon.layoutParams
        paramsOnHoldIcon.width = dp2px(avatarSize.toFloat())
        paramsOnHoldIcon.height = dp2px(avatarSize.toFloat())
        binding.onHoldIcon.layoutParams = paramsOnHoldIcon
        binding.avatar.setImageBitmap(participant.avatar)
    }

    /**
     * Initialising the UI of the participant
     *
     * @param participant Participant of whom the UI has to be initialized.
     */
    private fun checkUI(participant: Participant) {
        logDebug("Check the current UI status")
        inMeetingViewModel.getSession(participant.clientId)?.let {
            if (it.hasVideo()) {
                logDebug("Check if video should be on")
                checkVideoOn(participant)
            } else {
                logDebug("Video should be off")
                videoOffUI(participant)
            }

            updateAudioIcon(participant)
            updatePrivilegeIcon(participant)
            updatePeerSelected(participant)
        }
    }

    /**
     * Show UI when video is on
     *
     * @param participant Participant of whom the UI has to be shown.
     */
    private fun videoOnUI(participant: Participant) {
        if (isInvalid(participant)) return

        logDebug("UI video on")
        hideAvatar(participant)
        activateVideo(participant, !isGrid)
    }

    /**
     * Method to hide the Avatar
     *
     * @param participant Participant whose avatar is to be hidden
     */
    private fun hideAvatar(participant: Participant) {
        if (isInvalid(participant)) return

        logDebug("Hide Avatar")
        binding.onHoldIcon.isVisible = false
        binding.avatar.alpha = AVATAR_VIDEO_VISIBLE
        binding.avatar.isVisible = false
    }

    /**
     * Method for activating the video
     *
     * @param participant Participant whose video is to be activated
     */
    private fun activateVideo(participant: Participant, isSpeaker: Boolean) {
        if (isInvalid(participant)) return

        if (participant.videoListener == null) {
            logDebug("Active video when listener is null")
            binding.parentTextureView.removeAllViews()
            val myTexture = TextureView(MegaApplication.getInstance().applicationContext)
            myTexture.layoutParams = RelativeLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            myTexture.alpha = AVATAR_VIDEO_VISIBLE
            myTexture.rotation = ROTATION

            val vListener = GroupVideoListener(
                myTexture,
                participant.peerId,
                participant.clientId,
                false
            )

            participant.videoListener = vListener
            binding.parentTextureView.addView(participant.videoListener!!.textureView)
            participant.videoListener!!.localRenderer?.addListener(listenerRenderer)
        } else {
            logDebug("Active video when listener is not null")
            if (binding.parentTextureView.childCount > 0) {
                binding.parentTextureView.removeAllViews()
            }

            participant.videoListener?.textureView?.let { textureView ->
                textureView.parent?.let { textureViewParent ->
                    (textureViewParent as ViewGroup).removeView(textureView)
                }
            }

            binding.parentTextureView.addView(participant.videoListener?.textureView)

            participant.videoListener?.height = 0
            participant.videoListener?.width = 0
        }

        inMeetingViewModel.onActivateVideo(participant, isSpeaker)
        binding.parentTextureView.isVisible = true
    }

    /**
     * Show UI when video is off
     *
     * @param participant Participant from whom the video should be deactivated
     */
    private fun videoOffUI(participant: Participant) {
        if (isInvalid(participant)) return

        logDebug("UI video off")
        showAvatar(participant)
        closeVideo(participant)
        checkOnHold(participant)
    }

    /**
     * Show avatar
     *
     * @param participant Participant whose avatar is to be displayed
     */
    private fun showAvatar(participant: Participant) {
        if (isInvalid(participant)) return

        logDebug("Show avatar")
        binding.avatar.isVisible = true
    }

    /**
     * Method to close Video
     *
     * @param participant Participant from whom the video should be closed
     */
    fun closeVideo(participant: Participant) {
        if (isInvalid(participant)) return

        logDebug("Close video of ${participant.clientId}")
        binding.parentTextureView.isVisible = false

        inMeetingViewModel.onCloseVideo(participant)

        participant.videoListener?.let { listener ->
            listener.localRenderer?.addListener(null)

            logDebug("Removing texture view of ${participant.clientId}")
            if (binding.parentTextureView.childCount > 0) {
                binding.parentTextureView.removeAllViews()
            }

            listener.textureView?.let { view ->
                view.parent?.let { surfaceParent ->
                    (surfaceParent as ViewGroup).removeView(view)
                }
            }

            if (participant.videoListener != null) {
                logDebug("Participant ${participant.clientId} video listener null")
                participant.videoListener = null
            }
        }
    }

    /**
     * Method to control the Call/Session on hold icon visibility
     *
     * @param participant Participant to be checked if it is in on hold status
     */
    private fun checkOnHold(participant: Participant) {
        if (isInvalid(participant)) return

        val isCallOnHold = inMeetingViewModel.isCallOnHold()
        val isSessionOnHold = inMeetingViewModel.isSessionOnHold(participant.clientId)
        if (isSessionOnHold) {
            logDebug("Show on hold icon participant")
            binding.onHoldIcon.isVisible = true
            binding.avatar.alpha = AVATAR_WITH_TRANSPARENCY
        } else {
            logDebug("Hide on hold icon")
            binding.onHoldIcon.isVisible = false
            binding.avatar.alpha =
                if (isCallOnHold) AVATAR_WITH_TRANSPARENCY else AVATAR_VIDEO_VISIBLE
        }
    }

    /**
     * Check if mute icon should be visible
     *
     * @param participant Participant whose audio icon is to be updated
     */
    fun updateAudioIcon(participant: Participant) {
        if (isInvalid(participant)) return

        logDebug("Update audio icon")
        binding.muteIcon.isVisible = !participant.isAudioOn
    }

    /**
     * Control when a change is received in the video flag
     *
     * @param participant Participant to be checked whether the video should be activated or not
     */
    fun checkVideoOn(participant: Participant) {
        if (isInvalid(participant)) return

        if (participant.isVideoOn && !inMeetingViewModel.isCallOrSessionOnHold(participant.clientId)) {
            logDebug("Video should be on")
            videoOnUI(participant)
            return
        }

        logDebug("Video should be off")
        videoOffUI(participant)
    }

    /**
     * Update privileges
     *
     * @param participant Participant whose privileges are to be updated
     */
    fun updatePrivilegeIcon(participant: Participant) {
        if (isInvalid(participant)) return

        logDebug("Update privilege icon participant")
        binding.moderatorIcon.isVisible = participant.isModerator
    }

    /**
     * Update name and avatar
     *
     * @param participant Participant whose name needs to be updated
     */
    fun updateName(participant: Participant) {
        if (isInvalid(participant)) return

        logDebug("Update name")
        binding.avatar.setImageBitmap(participant.avatar)
    }

    /**
     * Check changes in call on hold
     *
     * @param participant Participant to update
     * @param isCallOnHold True, if the call is in the on hold state. False, if not.
     */
    fun updateCallOnHold(participant: Participant, isCallOnHold: Boolean) {
        if (isInvalid(participant)) return

        if (isCallOnHold) {
            logDebug("Call is on hold")
            videoOffUI(participant)
        } else {
            logDebug("Call is not on hold")
            checkVideoOn(participant)
        }
    }

    /**
     * Check changes in session on hold
     *
     * @param participant Participant to update
     * @param isSessionOnHold True, if the session is in the on hold state. False, if not.
     */
    fun updateSessionOnHold(participant: Participant, isSessionOnHold: Boolean) {
        if (isInvalid(participant)) return

        if (isSessionOnHold) {
            logDebug("Session is on hold")
            videoOffUI(participant)
        } else {
            logDebug("Session is not on hold")
            checkVideoOn(participant)
        }
    }

    /**
     * Update the peer selected
     *
     * @param participant Participant to be updated as the selected participant has been updated
     */
    fun updatePeerSelected(participant: Participant) {
        if (isGrid || isInvalid(participant)) return

        if (participant.isSpeaker) {
            logDebug("Participant is speaker")
            binding.selectedForeground.background = ContextCompat.getDrawable(
                binding.root.context,
                if (inMeetingViewModel.isSpeakerSelectionAutomatic) R.drawable.border_green_layer
                else R.drawable.border_green_layer_selected
            )
            binding.selectedForeground.isVisible = true
        } else {
            logDebug("Participant is not selected")
            binding.selectedForeground.isVisible = false
        }
    }

    /**
     * Remove Texture view when fragment is destroyed
     *
     * @param participant Participant from whom the texture view is to be deleted
     */
    fun removeTextureView(participant: Participant) {
        if (isInvalid(participant)) return

        logDebug("Removing texture view of ${participant.clientId}")
        if (binding.parentTextureView.childCount > 0) {
            binding.parentTextureView.removeAllViews()
            binding.parentTextureView.removeAllViewsInLayout()
        }

        participant.videoListener?.let { listener ->
            listener.localRenderer?.addListener(null)
            listener.textureView?.let { view ->
                view.parent?.let { surfaceParent ->
                    (surfaceParent as ViewGroup).removeView(view)
                }
            }
        }

        if (participant.videoListener != null) {
            logDebug("Participant ${participant.clientId} video listener null")
            participant.videoListener = null
        }
    }

    /**
     * Method for controlling the UI in landscape mode
     *
     * @param isFirstPage True, if it's page 0. False, otherwise
     * @param itemCount num of participants
     */
    private fun landscapeLayout(isFirstPage: Boolean, itemCount: Int) {
        if (!isGrid) return

        var w = 0
        var h = 0

        val layoutParams: GridLayoutManager.LayoutParams =
            binding.root.layoutParams as GridLayoutManager.LayoutParams

        var marginLeft = 0
        val marginTop = 0
        var marginRight = 0
        var marginBottom = 0

        if (isFirstPage) {
            when (itemCount) {
                SLOT_NUM_1 -> {
                    w = screenWidth / TWO_COLUMNS
                    h = screenHeight

                    marginLeft = w / 2
                    marginRight = marginLeft
                }
                SLOT_NUM_2 -> {
                    w = screenWidth / TWO_COLUMNS
                    h = screenHeight
                }
                SLOT_NUM_3 -> {
                    w = (screenWidth / THREE_COLUMNS)
                    h = (screenHeight * 0.6).toInt()
                    marginBottom = screenHeight - h
                }
                SLOT_NUM_4 -> {
                    w = (screenWidth / FOUR_COLUMNS)
                    h = (screenHeight / TWO_FILES)
                }
                SLOT_NUM_5 -> {
                    w = screenWidth / FOUR_COLUMNS
                    h = screenHeight / TWO_FILES

                    when (adapterPosition) {
                        POSITION_3, POSITION_4 -> marginLeft = w / 2
                    }
                }
                SLOT_NUM_6 -> {
                    w = (screenWidth / FOUR_COLUMNS)
                    h = (screenHeight / TWO_FILES)
                }
            }
        } else {
            w = (screenWidth / FOUR_COLUMNS)
            h = (screenHeight / TWO_FILES)
        }

        layoutParams.setMargins(marginLeft, marginTop, marginRight, marginBottom)
        layoutParams.width = w
        layoutParams.height = h
    }

    /**
     * Method for controlling the UI in portrait mode
     *
     * @param isFirstPage True, if it's page 0. False, otherwise
     * @param itemCount num of participants
     */
    private fun portraitLayout(isFirstPage: Boolean, itemCount: Int) {
        if (!isGrid) return

        val borderWidth = dp2px(BORDER_WIDTH)

        var w = 0
        var h = 0

        val layoutParams: GridLayoutManager.LayoutParams =
            binding.root.layoutParams as GridLayoutManager.LayoutParams

        var marginLeft = 0
        var marginTop = 0
        var marginRight = 0
        var marginBottom = 0

        if (isFirstPage) {
            when (itemCount) {
                SLOT_NUM_1 -> {
                    w = screenWidth
                    h = screenHeight
                }
                SLOT_NUM_2 -> {
                    w = screenWidth
                    h = screenHeight / TWO_FILES

                    // The 0 item has a bottom border.
                    when (adapterPosition) {
                        POSITION_0 -> marginBottom = borderWidth
                    }
                }
                SLOT_NUM_3 -> {
                    w = (screenWidth * 0.8).toInt()
                    h = screenHeight / THREE_FILES
                    marginLeft = (screenWidth - w) / 2
                    marginRight = marginLeft

                    // The 0, 1 item have a bottom border.
                    when (adapterPosition) {
                        POSITION_0, POSITION_1 -> marginBottom = borderWidth
                    }
                }
                SLOT_NUM_4 -> {
                    w = screenWidth / TWO_COLUMNS
                    h = w

                    when (adapterPosition) {
                        POSITION_0, POSITION_1 -> {
                            marginTop = ((screenHeight - screenWidth / 2 * 3) / 2)
                            marginBottom = borderWidth
                        }
                    }

                    when (adapterPosition) {
                        POSITION_1, POSITION_3 -> marginLeft = borderWidth
                    }
                }
                SLOT_NUM_5 -> {
                    w = screenWidth / TWO_COLUMNS
                    h = w

                    when (adapterPosition) {
                        POSITION_0, POSITION_1 -> {
                            marginTop = ((screenHeight - screenWidth / 2 * 3) / 2)
                            marginBottom = borderWidth
                        }
                        POSITION_2, POSITION_3 -> {
                            marginTop = 0
                        }
                        POSITION_4 -> {
                            marginTop = borderWidth
                            marginLeft = (screenWidth - w) / 2
                            marginRight = marginLeft
                        }
                    }

                    when (adapterPosition) {
                        POSITION_1, POSITION_3 -> marginLeft = borderWidth
                    }
                }
                SLOT_NUM_6 -> {
                    w = screenWidth / TWO_COLUMNS
                    h = w

                    when (adapterPosition) {
                        POSITION_0, POSITION_1 -> {
                            marginTop = ((screenHeight - screenWidth / 2 * 3) / 2)
                        }
                        POSITION_2, POSITION_3, POSITION_4, POSITION_5 -> {
                            marginTop = borderWidth
                        }
                    }

                    when (adapterPosition) {
                        POSITION_1, POSITION_3, POSITION_5 -> marginLeft = borderWidth
                    }
                }
            }
        } else {
            w = screenWidth / TWO_COLUMNS
            h = w

            when (adapterPosition) {
                POSITION_0, POSITION_1 -> marginTop = ((screenHeight - screenWidth / 2 * 3) / 2)
                POSITION_2, POSITION_3, POSITION_4, POSITION_5 -> {
                    marginTop = borderWidth
                }
            }

            when (adapterPosition) {
                POSITION_1, POSITION_3, POSITION_5 -> marginLeft = borderWidth
            }
        }

        layoutParams.apply {
            setMargins(marginLeft, marginTop, marginRight, marginBottom)
            width = w
            height = h
        }
    }

    /**
     * Method to control UI when scrolling in speaker view.
     * The participant being visible is checked.
     */
    fun onRecycle() {
        if (isGrid) return

        isDrawing = false
        inMeetingViewModel.getParticipant(peerId, clientId)?.let {
            if (it.isVideoOn) {
                logDebug("Recycle participant in the list, participant clientId is ${it.clientId}")
                inMeetingViewModel.removeParticipantVisible(it)
                inMeetingViewModel.onCloseVideo(it)
                removeTextureView(it)
            }
        }
    }

    /**
     * Method indicating whether the participant received is the same as the current participant
     *
     * @param participant The participant to be compared
     * @return If the compared participants are different. False, if they are the same
     */
    private fun isInvalid(participant: Participant) =
        (participant.peerId != peerId || participant.clientId != clientId)

    companion object {
        const val ITEM_WIDTH = 90f
        const val ITEM_HEIGHT = 90f
        const val BIG_AVATAR = 88
        const val SMALL_AVATAR = 60
        const val AVATAR_VIDEO_VISIBLE = 1f
        const val AVATAR_WITH_TRANSPARENCY = 0.5f
        const val BORDER_WIDTH = 2f
        const val ROTATION = 0f

        const val TWO_COLUMNS = 2
        const val TWO_FILES = 2
        const val THREE_COLUMNS = 3
        const val THREE_FILES = 3
        const val FOUR_COLUMNS = 3
        const val SLOT_NUM_1 = 1
        const val SLOT_NUM_2 = 2
        const val SLOT_NUM_3 = 3
        const val SLOT_NUM_4 = 4
        const val SLOT_NUM_5 = 5
        const val SLOT_NUM_6 = 6
        const val POSITION_0 = 0
        const val POSITION_1 = 1
        const val POSITION_2 = 2
        const val POSITION_3 = 3
        const val POSITION_4 = 4
        const val POSITION_5 = 5
    }
}
