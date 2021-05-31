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
import mega.privacy.android.app.R
import mega.privacy.android.app.components.RoundedImageView
import mega.privacy.android.app.databinding.IndividualCallFragmentBinding
import mega.privacy.android.app.databinding.SelfFeedFloatingWindowFragmentBinding
import mega.privacy.android.app.meeting.listeners.MeetingVideoListener
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.LogUtil.logError
import mega.privacy.android.app.utils.Util
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaChatCall
import nz.mega.sdk.MegaChatSession

@AndroidEntryPoint
class IndividualCallFragment : MeetingBaseFragment() {

    private var chatId: Long = MEGACHAT_INVALID_HANDLE
    private var peerId: Long = MEGACHAT_INVALID_HANDLE
    private var clientId: Long = MEGACHAT_INVALID_HANDLE

    private var isFloatingWindow = false

    private var orientation = Configuration.ORIENTATION_PORTRAIT

    private var videoAlphaFloating = 255

    // Views
    private lateinit var rootLayout: ConstraintLayout
    private lateinit var videoSurfaceView: SurfaceView
    private lateinit var avatarImageView: RoundedImageView
    private lateinit var onHoldImageView: ImageView

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

                session.hasVideo() -> {
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

    private val localAVFlagsObserver = Observer<MegaChatCall> {
        if (inMeetingViewModel.isSameCall(it.callId)) {
            logDebug("Check changes in local AVFlags")
            checkItIsOnlyAudio()
        }
    }

    private val callOnHoldObserver = Observer<MegaChatCall> {
        if (inMeetingViewModel.isSameCall(it.callId)) {
            logDebug("Check changes in call on hold")
            checkChangesInOnHold(it.isOnHold)
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
        LiveEventBus.get(Constants.EVENT_LOCAL_AVFLAGS_CHANGE, MegaChatCall::class.java)
            .observeSticky(this, localAVFlagsObserver)

        LiveEventBus.get(Constants.EVENT_CALL_ON_HOLD_CHANGE, MegaChatCall::class.java)
            .observeSticky(this, callOnHoldObserver)
        @Suppress("UNCHECKED_CAST")
        LiveEventBus.get(Constants.EVENT_REMOTE_AVFLAGS_CHANGE)
            .observeSticky(this, remoteAVFlagsObserver as Observer<Any>)
        @Suppress("UNCHECKED_CAST")
        LiveEventBus.get(Constants.EVENT_SESSION_ON_HOLD_CHANGE)
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
                    videoAlphaFloating = ((1 - it) * 255).toInt()
                    videoListener?.setAlpha(videoAlphaFloating)
                }
            }
        }

        checkUI()
    }

    /**
     * Initialising the UI
     */
    private fun checkUI() {
        logDebug("Check the current UI status")

        inMeetingViewModel.getCall()?.let {
            if (inMeetingViewModel.isMe(peerId)) {
                if (it.hasLocalVideo()) {
                    logDebug("Check if local video should be on")
                    checkVideoOn(peerId, clientId)
                } else {
                    logDebug("Local video should be off")
                    videoOffUI(peerId, clientId)
                }
            } else {
                val session = inMeetingViewModel.getSession(clientId)
                session?.let { participant ->
                    if (participant.hasVideo()) {
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
     * @param peerId
     * @param clientId
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
     * @param peerId
     * @param clientId
     */
    private fun showAvatar(peerId: Long, clientId: Long) {
        if (isInvalid(peerId, clientId)) return

        logDebug("Show avatar")
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
     * @param peerId
     * @param clientId
     */
    private fun closeVideo(peerId: Long, clientId: Long) {
        if (isInvalid(peerId, clientId)) return

        logDebug("Close video")
        videoSurfaceView.isVisible = false
        rootLayout.background = ContextCompat.getDrawable(
            requireContext(),
            R.drawable.self_feed_floating_window_background
        )
        removeChatVideoListener(peerId, clientId)
    }

    /**
     * Remove chat video listener
     *
     * @param peerId
     * @param clientId
     */
    private fun removeChatVideoListener(peerId: Long, clientId: Long) {
        if (isInvalid(peerId, clientId) || videoListener == null) return

        if (inMeetingViewModel.isMe(this.peerId)) {
            logDebug("Remove local video listener")
            sharedModel.removeLocalVideo(chatId, videoListener!!)
        } else {
            inMeetingViewModel.getSession(clientId)?.let {
                logDebug("Remove remove video listener")
                inMeetingViewModel.removeHiResOneToOneCall(videoListener!!, it, chatId)
            }
        }

        videoListener = null
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
     * @param peerId
     * @param clientId
     */
    private fun videoOnUI(peerId: Long, clientId: Long) {
        if (isInvalid(peerId, clientId)) return

        logDebug("UI video on")
        hideAvatar(peerId, clientId)
        activateVideo(peerId, clientId)
    }

    /**
     * Control when a change is received in the video flag
     *
     * @param peerId
     * @param clientId
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
            return
        }

        logDebug("The video should be turned on")
        videoOnUI(peerId, clientId)
    }

    /**
     * Method to hide the Avatar
     *
     * @param peerId
     * @param clientId
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
     * @param peerId
     * @param clientId
     */
    private fun activateVideo(peerId: Long, clientId: Long) {
        if (isInvalid(peerId, clientId)) return

        if (videoListener != null) {
            logDebug("Video Listener is not null ")
            videoListener!!.height = 0
            videoListener!!.width = 0
        } else {
            logDebug("Video Listener is null ")
            if (inMeetingViewModel.isMe(peerId)) {
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
            } else {
                videoListener = MeetingVideoListener(
                    videoSurfaceView,
                    outMetrics,
                    clientId,
                    isFloatingWindow = false,
                    isOneToOneCall = true
                )

                inMeetingViewModel.getSession(clientId)?.let {
                    inMeetingViewModel.addHiResOneToOneCall(videoListener!!, it, this.chatId)
                }
            }
        }

        videoSurfaceView.isVisible = true
        // Check bottom panel's expanding state, if it's expanded, video should invisible.
        videoListener?.setAlpha(videoAlphaFloating)
        rootLayout.background = null
    }

    /**
     * Method that controls when we have lost the video in the resolution we were receiving it.
     * Need to close video and activate it again
     *
     * @param peerId
     * @param clientId
     */
    fun updateResolution(peerId: Long, clientId: Long) {
        if (isInvalid(peerId, clientId)) return

        if (!isFloatingWindow && inMeetingViewModel.isOneToOneCall() && !inMeetingViewModel.isMe(
                peerId
            )
        ) {
            inMeetingViewModel.getSession(clientId)?.let {
                if (it.hasVideo()) {
                    logDebug("Update resolution")
                    closeVideo(peerId, clientId)
                    checkVideoOn(peerId, clientId)
                }
            }
        }
    }

    private fun isInvalid(peerId: Long, clientId: Long) =
        (peerId != this.peerId || clientId != this.clientId)

    /**
     * Method to check if there is a call or session on hold
     *
     * @param isOnHold
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
     *
     * @param newOrientation the new orientation
     */
    fun updateOrientation(newOrientation: Int) {
        if (!isFloatingWindow) return

        logDebug("Orientation changed")
        orientation = newOrientation

        val params = rootLayout.layoutParams
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            params.width = Util.dp2px(88f, outMetrics)
            params.height = Util.dp2px(120f, outMetrics)
        } else {
            params.width = Util.dp2px(120f, outMetrics)
            params.height = Util.dp2px(88f, outMetrics)
        }

        rootLayout.layoutParams = params
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
         * @param peerId peer ID
         * @param clientId client ID
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

    override fun onDestroyView() {
        logDebug("View destroyed")
        closeVideo(peerId, clientId)
        avatarImageView.setImageBitmap(null)
        super.onDestroyView()
    }
}