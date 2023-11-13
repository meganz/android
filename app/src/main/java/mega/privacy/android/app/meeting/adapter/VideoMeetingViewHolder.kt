package mega.privacy.android.app.meeting.adapter

import android.content.res.Configuration
import android.graphics.Outline
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.ItemParticipantVideoBinding
import mega.privacy.android.app.meeting.MegaSurfaceRenderer
import mega.privacy.android.app.meeting.fragments.InMeetingViewModel
import mega.privacy.android.app.utils.Constants.AVATAR_CHANGE
import mega.privacy.android.app.utils.Constants.NAME_CHANGE
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.Util.dp2px
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import timber.log.Timber
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
    private val onPageClickedCallback: (() -> Unit)?,
) : RecyclerView.ViewHolder(binding.root) {

    @Inject
    lateinit var megaApi: MegaApiAndroid

    lateinit var inMeetingViewModel: InMeetingViewModel

    private var avatarSize = BIG_AVATAR
    private var peerId = MEGACHAT_INVALID_HANDLE
    private var clientId = MEGACHAT_INVALID_HANDLE

    private var isDrawing = true

    private var itemCount = 0

    fun bind(
        inMeetingViewModel: InMeetingViewModel,
        participant: Participant,
        itemCount: Int,
        isFirstPage: Boolean,
    ) {
        this.inMeetingViewModel = inMeetingViewModel
        this.itemCount = itemCount

        if (participant.peerId == MEGACHAT_INVALID_HANDLE || participant.clientId == MEGACHAT_INVALID_HANDLE) {
            Timber.e("Error. Peer id or client id invalid")
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

            binding.root.background = ContextCompat.getDrawable(
                binding.root.context,
                R.drawable.self_feed_floating_window_background
            )

            (binding.muteIcon.layoutParams as LinearLayout.LayoutParams).apply {
                bottomMargin = 0
            }

            roundMuteIconBackground()
            binding.name.isVisible = false
        }

        if (!isGrid) {
            inMeetingViewModel.addParticipantVisible(participant)
        }

        initAvatar(participant)

        if (isGrid || isDrawing) {
            inMeetingViewModel.getSession(participant.clientId)?.let {
                participant.videoListener?.let { listener ->
                    Timber.d("Removing listener, clientID ${participant.clientId}")
                    inMeetingViewModel.removeChatRemoteVideoListener(
                        listener,
                        participant.clientId,
                        inMeetingViewModel.getChatId(),
                        participant.hasHiRes
                    )
                    removeListener(participant)
                }
            }
        }

        checkUI(participant)
    }

    /**
     * Make the mute icon background rounded
     */
    private fun roundMuteIconBackground() {
        binding.participantInfoLayout.background = ContextCompat.getDrawable(
            binding.root.context,
            R.drawable.participant_mic_off_background
        )
        binding.muteIcon.cornerRadius = dp2px(16f)
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
        Timber.d("Check the current UI status")
        inMeetingViewModel.getSession(participant.clientId)?.let {
            if (it.hasVideo()) {
                Timber.d("Check if video should be on")
                checkVideoOn(participant)
            } else {
                Timber.d("Video should be off")
                videoOffUI(participant)
            }

            updateAudioIcon(participant)
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

        Timber.d("UI video on for clientId ${participant.clientId}")
        hideAvatar(participant)
        activateVideo(participant)
    }

    /**
     * Method to hide the Avatar
     *
     * @param participant Participant whose avatar is to be hidden
     */
    private fun hideAvatar(participant: Participant) {
        if (isInvalid(participant)) return

        Timber.d("Hide Avatar")
        binding.onHoldIcon.isVisible = false
        binding.avatar.alpha = AVATAR_VIDEO_VISIBLE
        binding.avatar.isVisible = false
    }

    /**
     * Method for activating the video
     *
     * @param participant Participant whose video is to be activated
     */
    private fun activateVideo(participant: Participant) {
        if (isInvalid(participant)) return

        if (!inMeetingViewModel.isParticipantVisible(participant)) {
            Timber.d("No activate video, the participant with clientId ${participant.clientId} is not visible")
            return
        }

        Timber.d("Activate video, the participant with clientId ${participant.clientId} is visible")
        if (participant.videoListener == null) {
            binding.parentTextureView.removeAllViews()
            createListener(participant)

            inMeetingViewModel.getSession(participant.clientId)?.let {
                if (participant.hasHiRes && !it.canRecvVideoHiRes() && it.isHiResVideo) {
                    Timber.d("Asking for HiRes video, clientId ${participant.clientId}")
                    inMeetingViewModel.requestHiResVideo(it, inMeetingViewModel.currentChatId)
                } else if (!participant.hasHiRes && !it.canRecvVideoLowRes() && it.isLowResVideo) {
                    Timber.d("Asking for LowRes video, clientId ${participant.clientId}")
                    inMeetingViewModel.requestLowResVideo(it, inMeetingViewModel.currentChatId)
                } else {
                    Timber.d("Already have LowRes/HiRes video, clientId ${participant.clientId}")
                    updateListener(participant, true, participant.hasHiRes)
                }
            }
        } else {
            Timber.d("Listener is not null ${participant.clientId}")
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

        if (!isGrid || itemCount != SLOT_NUM_1) {
            participant.videoListener?.textureView?.let { view ->
                setRoundedCorners(view)
            }
        }

        binding.parentTextureView.isVisible = true
    }

    /**
     * Close Video of participant in a meeting. Removing resolution and listener.
     *
     * @param participant The participant from whom the video is to be closed
     */
    fun removeResolutionAndListener(participant: Participant) {
        if (isInvalid(participant) || participant.videoListener == null) return

        Timber.d("Close video of ${participant.clientId}")
        participant.videoListener?.let {
            inMeetingViewModel.removeResolutionAndListener(participant, it)

            if (binding.parentTextureView.childCount > 0) {
                binding.parentTextureView.removeAllViews()
                binding.parentTextureView.removeAllViewsInLayout()
            }

            removeListener(participant)
        }

        binding.parentTextureView.isVisible = false
    }

    /**
     * Show UI when video is off
     *
     * @param participant Participant from whom the video should be deactivated
     */
    private fun videoOffUI(participant: Participant) {
        if (isInvalid(participant)) return

        Timber.d("UI video off")
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

        Timber.d("Show avatar")
        binding.avatar.isVisible = true
    }

    /**
     * Method to close Video
     *
     * @param participant Participant from whom the video should be closed
     */
    fun closeVideo(participant: Participant) {
        if (isInvalid(participant)) return

        Timber.d("Close video of ${participant.clientId}")
        inMeetingViewModel.removeRemoteVideoResolution(participant)
        binding.parentTextureView.isVisible = false
    }

    /**
     * Method for creating the video listener
     *
     * @param participant The participant whose video listener is going to be created
     */
    private fun createListener(participant: Participant) {
        if (isInvalid(participant)) return

        participant.videoListener =
            inMeetingViewModel.createVideoListener(participant, AVATAR_VIDEO_VISIBLE, ROTATION)
        participant.videoListener?.let { listener ->
            binding.parentTextureView.addView(listener.textureView)
            if (!isGrid || itemCount != SLOT_NUM_1) {
                listener.textureView?.let { view -> setRoundedCorners(view) }
            }
            listener.localRenderer?.addListener(listenerRenderer)
        }
    }

    /**
     * Method for removing the video listener
     *
     * @param participant The participant whose video listener is going to be removed
     */
    private fun removeListener(participant: Participant) {
        if (isInvalid(participant)) return

        participant.videoListener?.let { listener ->
            listener.localRenderer?.addListener(null)

            Timber.d("Removing texture view of ${participant.clientId}")
            if (binding.parentTextureView.childCount > 0) {
                binding.parentTextureView.removeAllViews()
            }

            listener.textureView?.let { view ->
                view.parent?.let { surfaceParent ->
                    (surfaceParent as ViewGroup).removeView(view)
                }
            }

            Timber.d("Participant ${participant.clientId} video listener null")
            participant.videoListener = null
        }
    }

    /**
     * Method to add or remove video listener
     *
     * @param participant The participant whose listener of the video is to be added or removed
     * @param shouldAddListener True, if the listener is to be added. False, if the listener should be removed
     * @param isHiRes True, if it has high resolution. False, otherwise
     */
    fun updateListener(participant: Participant, shouldAddListener: Boolean, isHiRes: Boolean) {
        if (isInvalid(participant)) return

        if (shouldAddListener) {
            if (inMeetingViewModel.getSession(participant.clientId)
                    ?.hasVideo() == true && !inMeetingViewModel.isCallOrSessionOnHold(participant.clientId)
            ) {
                if (participant.videoListener == null) {
                    createListener(participant)
                }

                participant.videoListener?.let { listener ->
                    Timber.d("Adding listener, clientID ${participant.clientId}")
                    inMeetingViewModel.addChatRemoteVideoListener(
                        listener,
                        participant.clientId,
                        inMeetingViewModel.getChatId(), isHiRes
                    )
                }
            }
        } else {
            participant.videoListener?.let { listener ->
                Timber.d("Removing listener, clientID ${participant.clientId}")
                inMeetingViewModel.removeRemoteVideoListener(participant, listener)
                removeListener(participant)
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
            Timber.d("Show on hold icon participant")
            binding.onHoldIcon.isVisible = true
            binding.avatar.alpha = AVATAR_WITH_TRANSPARENCY
        } else {
            Timber.d("Hide on hold icon")
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

        Timber.d("Update audio icon")
        inMeetingViewModel.getSession(participant.clientId)?.let { session ->
            session.hasAudio().not().let { value ->
                binding.muteIcon.isVisible = value
                binding.speakingIcon.isVisible = false
                binding.selectedForeground.isVisible = false
                if (!isGrid) {
                    binding.participantInfoLayout.isVisible = value
                }
            }

            if (session.hasAudio()) {
                session.isAudioDetected.let { value ->
                    binding.speakingIcon.isVisible = value
                    binding.selectedForeground.isVisible = value
                    binding.muteIcon.isVisible = false
                    if (!isGrid) {
                        binding.participantInfoLayout.isVisible = value
                    }
                }
            }
        }
    }

    /**
     * Control when a change is received in the video flag
     *
     * @param participant Participant to be checked whether the video should be activated or not
     */
    fun checkVideoOn(participant: Participant) {
        if (isInvalid(participant)) return

        if (inMeetingViewModel.getSession(participant.clientId)
                ?.hasVideo() == true && !inMeetingViewModel.isCallOrSessionOnHold(participant.clientId)
        ) {
            Timber.d("Video should be on")
            videoOnUI(participant)
            return
        }

        Timber.d("Video should be off")
        videoOffUI(participant)
    }

    /**
     * Update name or avatar
     *
     * @param participant Participant whose name needs to be updated
     * @param type the type of change, name or avatar
     */
    fun updateNameOrAvatar(participant: Participant, type: Int) {
        if (isInvalid(participant)) return

        Timber.d("Update name")
        when (type) {
            NAME_CHANGE -> binding.name.text = participant.name
            AVATAR_CHANGE -> binding.avatar.setImageBitmap(participant.avatar)
        }
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
            Timber.d("Call is on hold")
            videoOffUI(participant)
        } else {
            Timber.d("Call is not on hold")
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
            Timber.d("Session is on hold")
            videoOffUI(participant)
        } else {
            Timber.d("Session is not on hold")
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

        participant.videoListener?.textureView?.let { view ->
            setRoundedCorners(view)
        }

        if (participant.isSpeaker) {
            Timber.d("Participant is speaker")
            setRoundedCorners(binding.selectedForeground)
            binding.selectedForeground.isVisible = true
        } else {
            Timber.d("Participant is not selected")
            binding.selectedForeground.isVisible = false
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

        var marginLeft = if (isFirstPage && itemCount == SLOT_NUM_1) 0 else dp2px(DEFAULT_MARGIN)
        val marginTop = if (isFirstPage && itemCount == SLOT_NUM_1) 0 else dp2px(DEFAULT_MARGIN)
        var marginRight = if (isFirstPage && itemCount == SLOT_NUM_1) 0 else dp2px(DEFAULT_MARGIN)
        var marginBottom = if (isFirstPage && itemCount == SLOT_NUM_1) 0 else dp2px(DEFAULT_MARGIN)

        if (isFirstPage) {
            when (itemCount) {
                SLOT_NUM_1 -> {
                    w = screenWidth / TWO_COLUMNS
                    h = screenHeight

                    marginLeft = w / 2
                    marginRight = marginLeft
                }

                SLOT_NUM_2 -> {
                    w = (screenWidth - 3 * dp2px(DEFAULT_MARGIN)) / TWO_COLUMNS
                    h = screenHeight - 2 * dp2px(DEFAULT_MARGIN)
                }

                SLOT_NUM_3 -> {
                    w = (screenWidth - 4 * dp2px(DEFAULT_MARGIN)) / THREE_COLUMNS
                    h = screenHeight - 2 * dp2px(DEFAULT_MARGIN)
                }

                SLOT_NUM_4 -> {
                    w = (screenWidth - 3 * dp2px(DEFAULT_MARGIN)) / FOUR_COLUMNS
                    h = (screenHeight - 3 * dp2px(DEFAULT_MARGIN)) / TWO_ROWS
                    when (bindingAdapterPosition) {
                        POSITION_0, POSITION_1 -> marginBottom = 0
                    }
                }

                SLOT_NUM_5 -> {
                    w = (screenWidth - 4 * dp2px(DEFAULT_MARGIN)) / FOUR_COLUMNS
                    h = (screenHeight - 3 * dp2px(DEFAULT_MARGIN)) / TWO_ROWS

                    when (bindingAdapterPosition) {
                        POSITION_0, POSITION_1, POSITION_2 -> marginBottom = 0
                        POSITION_3, POSITION_4 -> marginLeft = w / 2
                    }
                }

                SLOT_NUM_6 -> {
                    w = (screenWidth - 4 * dp2px(DEFAULT_MARGIN)) / FOUR_COLUMNS
                    h = (screenHeight - 3 * dp2px(DEFAULT_MARGIN)) / TWO_ROWS
                    when (bindingAdapterPosition) {
                        POSITION_0, POSITION_1, POSITION_2 -> marginBottom = 0
                    }
                }
            }
        } else {
            w = (screenWidth - 4 * dp2px(DEFAULT_MARGIN)) / FOUR_COLUMNS
            h = (screenHeight - 3 * dp2px(DEFAULT_MARGIN)) / TWO_ROWS
            when (bindingAdapterPosition) {
                POSITION_0, POSITION_1, POSITION_2 -> marginBottom = 0
            }
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

        // Margin top for the first and second items when total item count >= 4.
        val nonZeroMarginTop = (screenHeight - screenWidth / 2 * 3) / 2

        var w = 0
        var h = 0

        val layoutParams: GridLayoutManager.LayoutParams =
            binding.root.layoutParams as GridLayoutManager.LayoutParams

        var marginLeft = if (isFirstPage && itemCount == SLOT_NUM_1) 0 else dp2px(DEFAULT_MARGIN)
        var marginTop = if (isFirstPage && itemCount == SLOT_NUM_1) 0 else dp2px(DEFAULT_MARGIN)
        var marginRight = if (isFirstPage && itemCount == SLOT_NUM_1) 0 else dp2px(DEFAULT_MARGIN)
        val marginBottom = if (isFirstPage && itemCount == SLOT_NUM_1) 0 else dp2px(DEFAULT_MARGIN)

        if (isFirstPage) {
            when (itemCount) {
                SLOT_NUM_1 -> {
                    w = screenWidth
                    h = screenHeight
                }

                SLOT_NUM_2 -> {
                    w = screenWidth - 2 * dp2px(DEFAULT_MARGIN)
                    h = (screenHeight - 3 * dp2px(DEFAULT_MARGIN)) / TWO_ROWS

                    when (bindingAdapterPosition) {
                        POSITION_1 -> marginTop = 0
                    }
                }

                SLOT_NUM_3 -> {
                    w = screenWidth - 2 * dp2px(DEFAULT_MARGIN)
                    h = (screenHeight - 4 * dp2px(DEFAULT_MARGIN)) / THREE_ROWS

                    when (bindingAdapterPosition) {
                        POSITION_1, POSITION_2 -> marginTop = 0
                    }
                }

                SLOT_NUM_4 -> {
                    w = (screenWidth - 3 * dp2px(DEFAULT_MARGIN)) / TWO_COLUMNS
                    h = w

                    when (bindingAdapterPosition) {
                        POSITION_0, POSITION_1 -> marginTop = nonZeroMarginTop
                        POSITION_2, POSITION_3 -> marginTop = 0
                    }

                    when (bindingAdapterPosition) {
                        POSITION_1, POSITION_3 -> marginLeft = dp2px(DEFAULT_HALF_MARGIN)
                    }
                }

                SLOT_NUM_5 -> {
                    w = (screenWidth - 3 * dp2px(DEFAULT_MARGIN)) / TWO_COLUMNS
                    h = w

                    when (bindingAdapterPosition) {
                        POSITION_0, POSITION_1 -> marginTop = nonZeroMarginTop
                        POSITION_2, POSITION_3 -> marginTop = 0
                        POSITION_4 -> {
                            marginTop = 0
                            marginLeft = (screenWidth - w) / 2
                            marginRight = marginLeft
                        }
                    }

                    when (bindingAdapterPosition) {
                        POSITION_1, POSITION_3 -> marginLeft = dp2px(DEFAULT_HALF_MARGIN)
                    }
                }

                SLOT_NUM_6 -> {
                    w = (screenWidth - 3 * dp2px(DEFAULT_MARGIN)) / TWO_COLUMNS
                    h = w

                    when (bindingAdapterPosition) {
                        POSITION_0, POSITION_1 -> marginTop = nonZeroMarginTop
                        POSITION_2, POSITION_3, POSITION_4, POSITION_5 -> marginTop = 0
                    }

                    when (bindingAdapterPosition) {
                        POSITION_1, POSITION_3, POSITION_5 -> marginLeft =
                            dp2px(DEFAULT_HALF_MARGIN)
                    }
                }
            }
        } else {
            w = (screenWidth - 3 * dp2px(DEFAULT_MARGIN)) / TWO_COLUMNS
            h = w

            when (bindingAdapterPosition) {
                POSITION_0, POSITION_1 -> marginTop = nonZeroMarginTop
                POSITION_2, POSITION_3, POSITION_4, POSITION_5 -> marginTop = 0
            }

            when (bindingAdapterPosition) {
                POSITION_1, POSITION_3, POSITION_5 -> marginLeft = dp2px(DEFAULT_HALF_MARGIN)
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
        inMeetingViewModel.getParticipant(peerId, clientId)?.let { participant ->
            inMeetingViewModel.removeParticipantVisible(participant)

            if (inMeetingViewModel.getSession(participant.clientId)?.hasVideo() == true) {
                Timber.d("Recycle participant in the list, participant clientId is ${participant.clientId}")
                participant.videoListener?.let {
                    removeResolutionAndListener(participant)
                }
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

    /**
     * Set rounded corners for a view
     *
     * @param view  [View] to set the rounded corners
     */
    private fun setRoundedCorners(view: View) {
        val mOutlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                val cornerRadius = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    ROUNDED_CORNERS_RADIUS,
                    MegaApplication.getInstance().resources.displayMetrics
                ).toInt()

                outline.setRoundRect(0, 0, view.width, view.height, cornerRadius.toFloat())
            }
        }

        view.apply {
            outlineProvider = mOutlineProvider
            clipToOutline = true
        }
    }

    companion object {
        const val ITEM_WIDTH = 90f
        const val ITEM_HEIGHT = 90f
        const val BIG_AVATAR = 88
        const val SMALL_AVATAR = 60
        const val AVATAR_VIDEO_VISIBLE = 1f
        const val AVATAR_WITH_TRANSPARENCY = 0.5f
        const val ROTATION = 0f
        const val DEFAULT_MARGIN = 4f
        const val DEFAULT_HALF_MARGIN = 2f
        const val ROUNDED_CORNERS_RADIUS = 8f

        const val TWO_COLUMNS = 2
        const val TWO_ROWS = 2
        const val THREE_COLUMNS = 3
        const val THREE_ROWS = 3
        const val FOUR_COLUMNS = 4
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