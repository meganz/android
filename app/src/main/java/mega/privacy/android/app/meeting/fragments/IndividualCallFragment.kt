package mega.privacy.android.app.meeting.fragments

import android.content.res.Configuration
import android.os.Bundle
import android.util.Pair
import android.view.LayoutInflater
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.jeremyliao.liveeventbus.LiveEventBus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.individual_call_fragment.view.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mega.privacy.android.app.R
import mega.privacy.android.app.components.RoundedImageView
import mega.privacy.android.app.constants.EventConstants
import mega.privacy.android.app.constants.EventConstants.EVENT_CALL_ON_HOLD_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_LOCAL_AVFLAGS_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_REMOTE_AVFLAGS_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_SESSION_ON_HOLD_CHANGE
import mega.privacy.android.app.databinding.IndividualCallFragmentBinding
import mega.privacy.android.app.databinding.SelfFeedFloatingWindowFragmentBinding
import mega.privacy.android.app.meeting.listeners.MeetingVideoListener
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.LogUtil.logError
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.Util.getCurrentOrientation
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaChatCall
import nz.mega.sdk.MegaChatCall.CALL_STATUS_IN_PROGRESS
import nz.mega.sdk.MegaChatSession
import nz.mega.sdk.MegaChatSession.SESSION_STATUS_IN_PROGRESS

@AndroidEntryPoint
class IndividualCallFragment : MeetingBaseFragment() {

    private var chatId: Long = MEGACHAT_INVALID_HANDLE
    private var peerId: Long = MEGACHAT_INVALID_HANDLE
    private var clientId: Long = MEGACHAT_INVALID_HANDLE

    private var isFloatingWindow = false

    private var videoAlphaFloating = 255
    private var videoAlpha = 255

    // Views
    private lateinit var rootLayout: ConstraintLayout
    private lateinit var videoSurfaceView: SurfaceView
    private lateinit var avatarImageView: RoundedImageView
    private lateinit var onHoldImageView: ImageView

    @ExperimentalCoroutinesApi
    private lateinit var inMeetingFragment: InMeetingFragment

    private lateinit var inMeetingViewModel: InMeetingViewModel

    private var videoListener: MeetingVideoListener? = null

    private val remoteAVFlagsObserver = Observer<Pair<Long, MegaChatSession>> {
        val callId = it.first
        val session = it.second

        if (inMeetingViewModel.isOneToOneCall() && inMeetingViewModel.isSameCall(callId)) {
            logDebug("Check changes in remote AVFlags")
            when {
                isFloatingWindow -> checkItIsOnlyAudio()

                session.hasVideo() && session.status == SESSION_STATUS_IN_PROGRESS -> {
                    logDebug("Check if video should be on")
                    checkVideoOn(
                        session.peerid,
                        session.clientid
                    )
                }
                else -> {
                    logDebug("Video should be off")
                    videoOffUI(peerId, clientId)
                }
            }
        }
    }

    private val sessionHiResObserver =
        Observer<Pair<Long, MegaChatSession>> { callAndSession ->
            if (inMeetingViewModel.isSameCall(callAndSession.first) && !isFloatingWindow && inMeetingViewModel.isOneToOneCall()) {
                if (callAndSession.second.canRecvVideoHiRes()) {
                    logDebug("Can receive high-resolution video")

                    if (inMeetingViewModel.sessionHasVideo(callAndSession.second.clientid)) {
                        logDebug("Session has video")
                        addListener(callAndSession.second)
                    }
                } else {
                    logDebug("Can not receive high-resolution video")
                    removeListener(callAndSession.second)

                    //Ask for high resolution, if necessary
                    if (inMeetingViewModel.sessionHasVideo(callAndSession.second.clientid)) {
                        logDebug("Asking for HiRes video")
                        inMeetingViewModel.requestHiResVideo(callAndSession.second, chatId)
                    }
                }
            }
        }

    private val localAVFlagsObserver = Observer<MegaChatCall> {
        if (inMeetingViewModel.isSameCall(it.callId)) {
            logDebug("Check changes in local AVFlags")
            checkItIsOnlyAudio()
        }
    }

    private val callOnHoldObserver = Observer<MegaChatCall> {
        if (inMeetingViewModel.isSameCall(it.callId)) {
            logDebug("Check changes in call on hold")

            if (isAdded) {
                checkChangesInOnHold(it.isOnHold)
            }
        }
    }

    private val sessionOnHoldObserver = Observer<Pair<Long, MegaChatSession>> {
        val callId = it.first
        val session = it.second

        if (inMeetingViewModel.isOneToOneCall() && inMeetingViewModel.isSameCall(callId)) {
            logDebug("Check changes in session on hold")
            checkChangesInOnHold(
                session.isOnHold
            )
        }
    }

    @ExperimentalCoroutinesApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            chatId = it.getLong(Constants.CHAT_ID, MEGACHAT_INVALID_HANDLE)
            peerId = it.getLong(Constants.PEER_ID, MEGACHAT_INVALID_HANDLE)
            clientId = it.getLong(Constants.CLIENT_ID, MEGACHAT_INVALID_HANDLE)
            isFloatingWindow = it.getBoolean(Constants.IS_FLOATING_WINDOW, false)
        }

        if (chatId == MEGACHAT_INVALID_HANDLE) {
            logError("Error. Chat doesn't exist")
            return
        }

        inMeetingFragment = parentFragment as InMeetingFragment

        inMeetingViewModel = inMeetingFragment.inMeetingViewModel

        inMeetingViewModel.setChatId(chatId)

        if (inMeetingViewModel.getCall() == null || peerId == MEGACHAT_INVALID_HANDLE) {
            logError("Error. Call doesn't exist")
            return
        }

        if (!inMeetingViewModel.isMe(peerId) && clientId == MEGACHAT_INVALID_HANDLE) {
            logError("Error. Client id invalid")
            return
        }

        initLiveEventBus()
    }

    private fun initLiveEventBus() {
        LiveEventBus.get(EVENT_LOCAL_AVFLAGS_CHANGE, MegaChatCall::class.java)
            .observeSticky(this, localAVFlagsObserver)

        LiveEventBus.get(EVENT_CALL_ON_HOLD_CHANGE, MegaChatCall::class.java)
            .observeSticky(this, callOnHoldObserver)
        @Suppress("UNCHECKED_CAST")
        LiveEventBus.get(EVENT_REMOTE_AVFLAGS_CHANGE)
            .observeSticky(this, remoteAVFlagsObserver as Observer<Any>)
        @Suppress("UNCHECKED_CAST")
        LiveEventBus.get(EventConstants.EVENT_SESSION_ON_HIRES_CHANGE)
            .observe(this, sessionHiResObserver as Observer<Any>)
        @Suppress("UNCHECKED_CAST")
        LiveEventBus.get(EVENT_SESSION_ON_HOLD_CHANGE)
            .observeSticky(this, sessionOnHoldObserver as Observer<Any>)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = if (isFloatingWindow) {
            SelfFeedFloatingWindowFragmentBinding.inflate(
                inflater,
                container,
                false
            )
        } else {
            IndividualCallFragmentBinding.inflate(
                inflater,
                container,
                false
            )
        }

        binding.root.let {
            rootLayout = it as ConstraintLayout
            videoSurfaceView = it.video
            avatarImageView = it.avatar
            onHoldImageView = it.on_hold_icon
        }

        return binding.root
    }

    @ExperimentalCoroutinesApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        inMeetingViewModel.getAvatarBitmap(peerId)?.let {
            avatarImageView.setImageBitmap(it)
        }

        if (isFloatingWindow) {
            if (inMeetingFragment.bottomFloatingPanelViewHolder.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                view.alpha = 0f
            }

            inMeetingFragment.bottomFloatingPanelViewHolder.propertyUpdaters.apply {
                add {
                    view.alpha = 1 - it
                }
                add {
                    videoAlphaFloating = ((1 - it) * videoAlpha).toInt()
                    videoListener?.setAlpha(videoAlphaFloating)
                }
            }
        } else {
            videoListener?.setAlpha(videoAlpha)
        }

        checkUI()
    }

    /**
     * Method to add video listener
     *
     * @param session The session of paticipant whose listener of the video is to be added
     */
    fun addListener(session: MegaChatSession) {
        if (videoListener == null) {
            videoListener = MeetingVideoListener(
                videoSurfaceView,
                outMetrics,
                clientId,
                isFloatingWindow = false,
                isOneToOneCall = true
            )
            logDebug("Participant $clientId video listener created")
        }

        videoListener?.setAlpha(videoAlpha)

        inMeetingViewModel.addChatRemoteVideoListener(
            videoListener!!,
            session.clientid,
            chatId, true
        )
    }

    /**
     * Method to remove video listener
     *
     * @param session The session of paticipant whose listener of the video is to be removed
     */
    fun removeListener(session: MegaChatSession) {
        if (videoListener != null) {
            inMeetingViewModel.removeChatRemoteVideoListener(
                videoListener!!,
                session.clientid,
                chatId,
                true
            )

            videoListener = null

            logDebug("Participant $clientId video listener null")
        }
    }

    /**
     * Initialising the UI
     */
    private fun checkUI() {
        logDebug("Check the current UI status")
        inMeetingViewModel.getCall()?.let {
            if (inMeetingViewModel.isMe(peerId)) {
                if (it.status == CALL_STATUS_IN_PROGRESS && it.hasLocalVideo()) {
                    logDebug("Check if local video should be on")
                    checkVideoOn(peerId, clientId)
                } else {
                    logDebug("Local video should be off")
                    videoOffUI(peerId, clientId)
                }
            } else {
                val session = inMeetingViewModel.getSession(clientId)
                session?.let { participant ->
                    if (session.status == SESSION_STATUS_IN_PROGRESS && participant.hasVideo()) {
                        logDebug("Check if remote video should be on")
                        checkVideoOn(peerId, clientId)
                    } else {
                        logDebug("Remote video should be off")
                        videoOffUI(peerId, clientId)
                    }
                }
            }
        }
    }

    /**
     * Show UI when video is off
     *
     * @param peerId User handle of a participant
     * @param clientId Client ID of a participant
     */
    fun videoOffUI(peerId: Long, clientId: Long) {
        if (isInvalid(peerId, clientId)) return

        logDebug("UI video off")
        showAvatar(peerId, clientId)
        checkItIsOnlyAudio()
        closeVideo(peerId, clientId)
        showCallOnHoldIcon()
    }

    /**
     * Method to show the Avatar
     *
     * @param peerId User handle of a participant
     * @param clientId Client ID of a participant
     */
    private fun showAvatar(peerId: Long, clientId: Long) {
        if (isInvalid(peerId, clientId)) return

        logDebug("Show avatar")
        rootLayout.isVisible = true
        avatarImageView.isVisible = true
    }

    /**
     * Check if is an audio call
     */
    private fun checkItIsOnlyAudio() {
        if (!isFloatingWindow || !inMeetingViewModel.isOneToOneCall()) return

        if (inMeetingViewModel.isAudioCall()) {
            logDebug("Is only audio call, hide avatar")
            hideAvatar(peerId, clientId)
            rootLayout.isVisible = false
        } else {
            inMeetingViewModel.getCall()?.let {
                if (!it.hasLocalVideo()) {
                    logDebug("Not only audio call, show avatar")
                    showAvatar(peerId, clientId)
                }
            }
        }
    }

    /**
     * Method to close Video
     *
     * @param peerId User handle of a participant
     * @param clientId Client ID of a participant
     */
    private fun closeVideo(peerId: Long, clientId: Long) {
        if (isInvalid(peerId, clientId)) return

        logDebug("Close video of $clientId")
        videoSurfaceView.isVisible = false

        if (isFloatingWindow) {
            rootLayout.background = ContextCompat.getDrawable(
                requireContext(),
                R.drawable.self_feed_floating_window_background
            )
        }

        removeChatVideoListener(peerId, clientId)
    }

    /**
     * Remove chat video listener
     *
     * @param peerId User handle of a participant
     * @param clientId Client ID of a participant
     */
    private fun removeChatVideoListener(peerId: Long, clientId: Long) {
        if (isInvalid(peerId, clientId)) return

        if (inMeetingViewModel.isMe(this.peerId)) {
            videoListener?.let {
                logDebug("Remove local video listener")
                sharedModel.removeLocalVideo(chatId, videoListener!!)
            }

            this.videoListener = null

        } else {
            inMeetingViewModel.getSession(clientId)?.let { session ->
                videoListener?.let {
                    if (session.canRecvVideoHiRes()) {
                        logDebug("Removing HiRes video")
                        inMeetingViewModel.stopHiResVideo(session, chatId)
                    }
                }
            }
        }
    }

    /**
     * Method to control the Call on hold icon visibility
     */
    private fun showCallOnHoldIcon() {
        if (inMeetingViewModel.isOneToOneCall() && inMeetingViewModel.isCallOrSessionOnHoldOfOneToOneCall() && !isFloatingWindow ||
            !inMeetingViewModel.isOneToOneCall() && inMeetingViewModel.isCallOnHold()
        ) {
            logDebug("Show on hold icon")
            onHoldImageView.isVisible = true
            avatarImageView.alpha = 0.5f
            return
        }

        logDebug("Hide on hold icon")
        onHoldImageView.isVisible = false
        avatarImageView.alpha = 1f

    }

    /**
     * Show UI when video is on
     *
     * @param peerId User handle of a participant
     * @param clientId Client ID of a participant
     */
    private fun videoOnUI(peerId: Long, clientId: Long) {
        if (isInvalid(peerId, clientId)) return

        inMeetingViewModel.getCall()?.let {
            logDebug("UI video on")
            hideAvatar(peerId, clientId)
            activateVideo(peerId, clientId)
        }
    }

    /**
     * Control when a change is received in the video flag
     *
     * @param peerId User handle of a participant
     * @param clientId Client ID of a participant
     */
    fun checkVideoOn(peerId: Long, clientId: Long) {
        if (isInvalid(peerId, clientId)) return

        if ((!inMeetingViewModel.isMe(peerId) && inMeetingViewModel.isCallOrSessionOnHoldOfOneToOneCall()) || (inMeetingViewModel.isMe(
                peerId
            ) &&
                    ((inMeetingViewModel.isOneToOneCall() && inMeetingViewModel.isCallOrSessionOnHoldOfOneToOneCall()) ||
                            (!inMeetingViewModel.isOneToOneCall() && inMeetingViewModel.isCallOnHold())))
        ) {
            logDebug("The video should not be turned on")
            videoOffUI(peerId, clientId)
            return
        }

        logDebug("The video should be turned on")
        videoOnUI(peerId, clientId)
    }

    /**
     * Method to hide the Avatar
     *
     * @param peerId User handle of a participant
     * @param clientId Client ID of a participant
     */
    private fun hideAvatar(peerId: Long, clientId: Long) {
        if (isInvalid(peerId, clientId)) return

        logDebug("Hide Avatar")
        onHoldImageView.isVisible = false
        avatarImageView.alpha = 1f
        avatarImageView.isVisible = false
    }

    /**
     * Method for activating the video
     *
     * @param peerId User handle of a participant
     * @param clientId Client ID of a participant
     */
    private fun activateVideo(peerId: Long, clientId: Long) {
        if (isInvalid(peerId, clientId)) return

        when {
            videoListener != null -> {
                logDebug("Video listener not null")
                videoListener!!.height = 0
                videoListener!!.width = 0
            }
            inMeetingViewModel.isMe(peerId) -> {
                var isOneToOneChat = true
                if (isFloatingWindow && !inMeetingViewModel.isOneToOneCall()) {
                    isOneToOneChat = false
                }

                videoListener = MeetingVideoListener(
                    videoSurfaceView,
                    outMetrics,
                    MEGACHAT_INVALID_HANDLE,
                    isFloatingWindow,
                    isOneToOneChat
                )

                sharedModel.addLocalVideo(chatId, videoListener)

                // Check bottom panel's expanding state, if it's expanded, video should invisible.
                videoListener?.setAlpha(videoAlphaFloating)
            }
            else -> {
                logDebug("Video listener is null")
                videoListener = MeetingVideoListener(
                    videoSurfaceView,
                    outMetrics,
                    clientId,
                    isFloatingWindow = false,
                    isOneToOneCall = true
                )
                logDebug("Participant $clientId video listener created")

                inMeetingViewModel.getSession(clientId)?.let {
                    if (!it.canRecvVideoHiRes()) {
                        logDebug("Asking for HiRes video")
                        inMeetingViewModel.requestHiResVideo(it, this.chatId)
                    } else {
                        logDebug("I am already receiving the HiRes video")
                        if (inMeetingViewModel.sessionHasVideo(it.clientid)) {
                            logDebug("Session has video")
                            addListener(it)
                        }
                    }
                }
            }
        }

        rootLayout.isVisible = true
        videoSurfaceView.isVisible = true
        rootLayout.background = null
    }

    /**
     * Method that controls when we have lost the video in the resolution we were receiving it.
     * Need to close video and activate it again
     *
     * @param peerId User handle of a participant
     * @param clientId Client ID of a participant
     */
    fun updateResolution(peerId: Long, clientId: Long) {
        if (isInvalid(peerId, clientId)) return

        if (!isFloatingWindow && inMeetingViewModel.isOneToOneCall() && !inMeetingViewModel.isMe(
                peerId
            )
        ) {
            inMeetingViewModel.getSession(clientId)?.let {
                if (it.status == SESSION_STATUS_IN_PROGRESS && it.hasVideo()) {
                    logDebug("Update resolution")
                    closeVideo(peerId, clientId)
                    checkVideoOn(peerId, clientId)
                }
            }
        }
    }

    /**
     * Method compare with current participant
     *
     * @param peerId User handle of a participant
     * @param clientId Client ID of a participant
     */
    private fun isInvalid(peerId: Long, clientId: Long) =
        peerId != this.peerId || clientId != this.clientId

    /**
     * Method to check if there is a call or session on hold
     *
     * @param isOnHold True, if the call or session is on hold. False, if not
     */
    private fun checkChangesInOnHold(isOnHold: Boolean) {
        if (isOnHold) {
            logDebug("Call or session on hold")
            //It's on hold
            if (inMeetingViewModel.isOneToOneCall() && inMeetingViewModel.isMe(this.peerId) && isFloatingWindow) {
                closeVideo(peerId, this.clientId)
                checkItIsOnlyAudio()
                return
            }

            videoOffUI(peerId, clientId)
        } else {
            //It is not on hold
            logDebug("Call or session is not on hold")
            checkUI()
        }
    }

    /**
     * Change the layout when the orientation is changing
     */
    fun updateOrientation() {
        if (!isFloatingWindow) return

        logDebug("Orientation changed")
        val params = rootLayout.layoutParams
        if (getCurrentOrientation() == Configuration.ORIENTATION_PORTRAIT) {
            params.width = Util.dp2px(88f, outMetrics)
            params.height = Util.dp2px(120f, outMetrics)
        } else {
            params.width = Util.dp2px(120f, outMetrics)
            params.height = Util.dp2px(88f, outMetrics)
        }

        rootLayout.layoutParams = params
    }

    /**
     * Update my own avatar
     */
    fun updateMyAvatar() {
        inMeetingViewModel.getAvatarBitmap(peerId)?.let {
            avatarImageView.setImageBitmap(it)
        }
    }

    companion object {

        const val TAG = "IndividualCallFragment"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param chatId Chat ID
         * @param peerId peer ID
         * @param isFloatingWindow True, if it's floating window. False, otherwise.
         * @return A new instance of fragment MeetingFragment.
         */
        @JvmStatic
        fun newInstance(chatId: Long, peerId: Long, isFloatingWindow: Boolean) =
            IndividualCallFragment().apply {
                arguments = Bundle().apply {
                    putLong(Constants.CHAT_ID, chatId)
                    putLong(Constants.PEER_ID, peerId)
                    putBoolean(Constants.IS_FLOATING_WINDOW, isFloatingWindow)
                }
            }

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param chatId Chat ID
         * @param peerId User handle of a participant
         * @param clientId Client ID of a participant
         * @return A new instance of fragment MeetingFragment.
         */
        fun newInstance(chatId: Long, peerId: Long, clientId: Long) =
            IndividualCallFragment().apply {
                arguments = Bundle().apply {
                    putLong(Constants.CHAT_ID, chatId)
                    putLong(Constants.PEER_ID, peerId)
                    putLong(Constants.CLIENT_ID, clientId)
                }
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        logDebug("View destroyed")
        closeVideo(peerId, clientId)
        inMeetingViewModel.getSession(clientId)?.let {
            removeListener(it)
        }

        avatarImageView.setImageBitmap(null)
    }
}