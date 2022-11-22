package mega.privacy.android.app.meeting.fragments

import android.content.res.Configuration
import android.os.Bundle
import android.util.Pair
import android.view.LayoutInflater
import android.view.TextureView
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
import mega.privacy.android.app.R
import mega.privacy.android.app.components.RoundedImageView
import mega.privacy.android.app.constants.EventConstants
import mega.privacy.android.app.constants.EventConstants.EVENT_CALL_ON_HOLD_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_LOCAL_AVFLAGS_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_REMOTE_AVFLAGS_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_SESSION_ON_HOLD_CHANGE
import mega.privacy.android.app.databinding.IndividualCallFragmentBinding
import mega.privacy.android.app.databinding.SelfFeedFloatingWindowFragmentBinding
import mega.privacy.android.app.meeting.listeners.IndividualCallVideoListener
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.Util.getCurrentOrientation
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaChatCall
import nz.mega.sdk.MegaChatCall.CALL_STATUS_IN_PROGRESS
import nz.mega.sdk.MegaChatSession
import nz.mega.sdk.MegaChatSession.SESSION_STATUS_IN_PROGRESS
import timber.log.Timber

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
    private lateinit var videoTextureView: TextureView
    private lateinit var avatarImageView: RoundedImageView
    private lateinit var onHoldImageView: ImageView
    private var microOffImageView: ImageView? = null

    private lateinit var inMeetingFragment: InMeetingFragment

    private lateinit var inMeetingViewModel: InMeetingViewModel

    private var videoListener: IndividualCallVideoListener? = null

    private val remoteAVFlagsObserver = Observer<Pair<Long, MegaChatSession>> {
        val callId = it.first
        val session = it.second

        if (inMeetingViewModel.isOneToOneCall() && inMeetingViewModel.isSameCall(callId) && isAdded) {
            Timber.d("Check changes in remote AVFlags")
            when {
                isFloatingWindow -> checkItIsOnlyAudio()

                session.hasVideo() && session.status == SESSION_STATUS_IN_PROGRESS -> {
                    Timber.d("Check if video should be on")
                    checkVideoOn(
                        session.peerid,
                        session.clientid
                    )
                }
                else -> {
                    Timber.d("Video should be off")
                    videoOffUI(peerId, clientId)
                }
            }
        }
    }

    private val sessionHiResObserver =
        Observer<Pair<Long, MegaChatSession>> { callAndSession ->
            if (inMeetingViewModel.isSameCall(callAndSession.first) && !isFloatingWindow && inMeetingViewModel.isOneToOneCall() && isAdded) {
                if (callAndSession.second.canRecvVideoHiRes() && callAndSession.second.isHiResVideo) {
                    Timber.d("Can receive high-resolution video")

                    if (inMeetingViewModel.sessionHasVideo(callAndSession.second.clientid)) {
                        Timber.d("Session has video of client ID ${callAndSession.second.clientid}")
                        addListener(callAndSession.second.clientid)
                    }
                } else {
                    Timber.d("Can not receive high-resolution video of client ID ${callAndSession.second.clientid}")
                    removeRemoteListener(
                        callAndSession.second.peerid,
                        callAndSession.second.clientid
                    )

                    //Ask for high resolution, if necessary
                    if (inMeetingViewModel.sessionHasVideo(callAndSession.second.clientid)) {
                        Timber.d("Asking for HiRes video of client ID ${callAndSession.second.clientid}")
                        inMeetingViewModel.requestHiResVideo(callAndSession.second, chatId)
                    }
                }
            }
        }

    private val localAVFlagsObserver = Observer<MegaChatCall> {
        if (inMeetingViewModel.isSameCall(it.callId) && isAdded) {
            Timber.d("Check changes in local AVFlags")
            checkItIsOnlyAudio()
        }
    }

    private val callOnHoldObserver = Observer<MegaChatCall> {
        if (inMeetingViewModel.isSameCall(it.callId) && isAdded) {
            Timber.d("Check changes in call on hold")
            checkChangesInOnHold(it.isOnHold)
        }
    }

    private val sessionOnHoldObserver = Observer<Pair<Long, MegaChatSession>> {
        val callId = it.first
        val session = it.second

        if (inMeetingViewModel.isOneToOneCall() && inMeetingViewModel.isSameCall(callId) && isAdded) {
            Timber.d("Check changes in session on hold")
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
            Timber.e("Error. Chat doesn't exist")
            return
        }

        inMeetingFragment = parentFragment as InMeetingFragment

        inMeetingViewModel = inMeetingFragment.inMeetingViewModel

        inMeetingViewModel.setChatId(chatId)

        if (inMeetingViewModel.getCall() == null || peerId == MEGACHAT_INVALID_HANDLE) {
            Timber.e("Error. Call doesn't exist")
            return
        }

        if (!inMeetingViewModel.isMe(peerId) && clientId == MEGACHAT_INVALID_HANDLE) {
            Timber.e("Error. Client id invalid")
            return
        }

        initLiveEventBus()
    }

    private fun initLiveEventBus() {
        LiveEventBus.get(EVENT_LOCAL_AVFLAGS_CHANGE, MegaChatCall::class.java)
            .observeSticky(this, localAVFlagsObserver)
        @Suppress("UNCHECKED_CAST")
        LiveEventBus.get(EVENT_REMOTE_AVFLAGS_CHANGE)
            .observeSticky(this, remoteAVFlagsObserver as Observer<Any>)

        @Suppress("UNCHECKED_CAST")
        LiveEventBus.get(EventConstants.EVENT_SESSION_ON_HIRES_CHANGE)
            .observe(this, sessionHiResObserver as Observer<Any>)

        LiveEventBus.get(EVENT_CALL_ON_HOLD_CHANGE, MegaChatCall::class.java)
            .observe(this, callOnHoldObserver)
        @Suppress("UNCHECKED_CAST")
        LiveEventBus.get(EVENT_SESSION_ON_HOLD_CHANGE)
            .observe(this, sessionOnHoldObserver as Observer<Any>)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
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

        // The cast is essential here not repeated code.
        if (binding is SelfFeedFloatingWindowFragmentBinding) {
            rootLayout = binding.root
            videoTextureView = binding.video
            avatarImageView = binding.avatar
            onHoldImageView = binding.onHoldIcon
            microOffImageView = binding.microOffIcon
        }

        if (binding is IndividualCallFragmentBinding) {
            rootLayout = binding.root
            videoTextureView = binding.video
            avatarImageView = binding.avatar
            onHoldImageView = binding.onHoldIcon
        }

        videoTextureView.isOpaque = false

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
                    videoAlphaFloating = ((1 - it) * videoAlpha).toInt()
                    videoListener?.setAlpha(videoAlphaFloating)
                }
            }

            inMeetingViewModel.getCall()?.hasLocalAudio()
                ?.let { showLocalFloatingViewMicroMutedIcon(!it) }
        } else {
            videoListener?.setAlpha(videoAlpha)
        }

        checkUI()
    }

    /**
     * Method to add video listener
     *
     * @param clientId Client ID of participant
     */
    private fun addListener(clientId: Long) {
        if (videoListener == null) {
            videoListener = IndividualCallVideoListener(
                videoTextureView,
                resources.displayMetrics,
                clientId,
                isFloatingWindow = false
            )
            Timber.d("Participant $clientId video listener created")
        }

        videoListener?.setAlpha(videoAlpha)
        Timber.d("Add remote video listener of client ID $clientId")
        inMeetingViewModel.addChatRemoteVideoListener(
            videoListener!!,
            clientId,
            chatId, true
        )
    }

    /**
     * Initialising the UI
     */
    private fun checkUI() {
        Timber.d("Check the current UI status")
        inMeetingViewModel.getCall()?.let {
            if (inMeetingViewModel.isMe(peerId)) {
                if (it.status == CALL_STATUS_IN_PROGRESS && it.hasLocalVideo()) {
                    Timber.d("Check if local video should be on")
                    checkVideoOn(peerId, clientId)
                } else {
                    Timber.d("Local video should be off")
                    videoOffUI(peerId, clientId)
                }
            } else {
                val session = inMeetingViewModel.getSession(clientId)
                session?.let { participant ->
                    if (session.status == SESSION_STATUS_IN_PROGRESS && participant.hasVideo()) {
                        Timber.d("Check if remote video should be on")
                        checkVideoOn(peerId, clientId)
                    } else {
                        Timber.d("Remote video should be off")
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

        Timber.d("UI video off")
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

        Timber.d("Show avatar")
        rootLayout.isVisible = true
        avatarImageView.isVisible = true
    }

    /**
     * Check if is an audio call
     */
    private fun checkItIsOnlyAudio() {
        if (!isFloatingWindow || !inMeetingViewModel.isOneToOneCall()) return

        if (inMeetingViewModel.isAudioCall()) {
            Timber.d("Is only audio call, hide avatar")
            hideAvatar(peerId, clientId)
            rootLayout.isVisible = false
        } else {
            inMeetingViewModel.getCall()?.let {
                if (!it.hasLocalVideo()) {
                    Timber.d("Not only audio call, show avatar")
                    showAvatar(peerId, clientId)
                }
            }
        }
    }

    /**
     * Method to show or hide the local micro muted icon of the floating view
     *
     * @param show Indicates if the call has local audio or not
     */
    fun showLocalFloatingViewMicroMutedIcon(show: Boolean) {
        microOffImageView?.isVisible = show
    }

    /**
     * Method to close Video
     *
     * @param peerId User handle of a participant
     * @param clientId Client ID of a participant
     */
    private fun closeVideo(peerId: Long, clientId: Long) {
        if (isInvalid(peerId, clientId)) return

        Timber.d("Close video of $clientId")
        videoTextureView.isVisible = false

        if (isFloatingWindow) {
            rootLayout.background = ContextCompat.getDrawable(
                requireContext(),
                R.drawable.self_feed_floating_window_background
            )
        }

        removeChatVideoListener()
    }

    /**
     * Method to control the Call on hold icon visibility
     */
    private fun showCallOnHoldIcon() {
        if (inMeetingViewModel.isOneToOneCall() && inMeetingViewModel.isCallOrSessionOnHoldOfOneToOneCall() && !isFloatingWindow ||
            !inMeetingViewModel.isOneToOneCall() && inMeetingViewModel.isCallOnHold()
        ) {
            Timber.d("Show on hold icon")
            onHoldImageView.isVisible = true
            avatarImageView.alpha = 0.5f
            return
        }

        Timber.d("Hide on hold icon")
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
            Timber.d("UI video on")
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
        val currentCall = inMeetingViewModel.getCall()
        if ((currentCall != null && currentCall.status != MegaChatCall.CALL_STATUS_JOINING && currentCall.status != CALL_STATUS_IN_PROGRESS) ||
            (!inMeetingViewModel.isMe(peerId) && inMeetingViewModel.isCallOrSessionOnHoldOfOneToOneCall()) ||
            (inMeetingViewModel.isMe(peerId) &&
                    ((inMeetingViewModel.isOneToOneCall() && inMeetingViewModel.isCallOrSessionOnHoldOfOneToOneCall()) ||
                            (!inMeetingViewModel.isOneToOneCall() && inMeetingViewModel.isCallOnHold())))
        ) {
            Timber.d("The video should be turned off")
            videoOffUI(peerId, clientId)
            return
        }

        Timber.d("The video should be turned on")
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

        Timber.d("Hide Avatar")
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
                Timber.d("Video listener not null")
                videoListener!!.height = 0
                videoListener!!.width = 0
            }
            inMeetingViewModel.isMe(peerId) -> {
                videoListener = IndividualCallVideoListener(
                    videoTextureView,
                    resources.displayMetrics,
                    MEGACHAT_INVALID_HANDLE,
                    isFloatingWindow
                )
                // Check bottom panel's expanding state, if it's expanded, video should invisible.
                videoListener?.setAlpha(videoAlphaFloating)
                Timber.d("Add local video listener")
                sharedModel.addLocalVideo(chatId, videoListener)
            }
            else -> {
                Timber.d("Video listener is null")
                videoListener = IndividualCallVideoListener(
                    videoTextureView,
                    resources.displayMetrics,
                    clientId,
                    isFloatingWindow = false
                )
                Timber.d("Participant $clientId video listener created")

                inMeetingViewModel.getSession(clientId)?.let {
                    if (!it.canRecvVideoHiRes() && it.isHiResVideo) {
                        Timber.d("Asking for HiRes video of client ID $clientId")
                        inMeetingViewModel.requestHiResVideo(it, this.chatId)
                    } else {
                        Timber.d("I am already receiving the HiRes video")
                        if (inMeetingViewModel.sessionHasVideo(it.clientid)) {
                            Timber.d("Session has video")
                            addListener(it.clientid)
                        }
                    }
                }
            }
        }

        rootLayout.isVisible = true
        videoTextureView.isVisible = true
        rootLayout.background = null
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
            Timber.d("Call or session on hold")
            //It's on hold
            if (inMeetingViewModel.isOneToOneCall() && inMeetingViewModel.isMe(this.peerId) && isFloatingWindow) {
                closeVideo(peerId, this.clientId)
                checkItIsOnlyAudio()
                return
            }

            videoOffUI(peerId, clientId)
        } else {
            //It is not on hold
            Timber.d("Call or session is not on hold")
            checkUI()
        }
    }

    /**
     * Change the layout when the orientation is changing
     */
    fun updateOrientation() {
        Timber.d("Orientation changed. Is floating window $isFloatingWindow")

        if (!isFloatingWindow) {
            videoListener?.let {
                it.height = 0
                it.width = 0
            }
            return
        }

        val params = rootLayout.layoutParams
        if (getCurrentOrientation() == Configuration.ORIENTATION_PORTRAIT) {
            params.width = Util.dp2px(88f, resources.displayMetrics)
            params.height = Util.dp2px(120f, resources.displayMetrics)
        } else {
            params.width = Util.dp2px(120f, resources.displayMetrics)
            params.height = Util.dp2px(88f, resources.displayMetrics)
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

    /**
     * Remove chat video listener
     */
    fun removeChatVideoListener() {
        videoListener?.let {
            removeResolutionOrLocalListener(this.peerId, this.clientId)
            removeRemoteListener(this.peerId, this.clientId)
        }
    }

    /**
     * Remove chat video listener
     *
     * @param peerId User handle of a participant
     * @param clientId Client ID of a participant
     */
    private fun removeResolutionOrLocalListener(peerId: Long, clientId: Long) {
        if (isInvalid(peerId, clientId)) return

        if (inMeetingViewModel.isMe(this.peerId)) {
            videoListener?.let {
                Timber.d("Remove local video listener")
                sharedModel.removeLocalVideo(chatId, videoListener!!)
            }

            videoListener = null
        } else {
            inMeetingViewModel.getSession(clientId)?.let { session ->
                videoListener?.let {
                    if (session.canRecvVideoHiRes()) {
                        Timber.d("Removing HiRes video of client ID $clientId")
                        inMeetingViewModel.stopHiResVideo(session, chatId)
                    }
                }
            }
        }
    }

    /**
     * Method to remove video listener
     *
     * @param peerId User handle of a participant
     * @param clientId Client ID of a participant
     */
    private fun removeRemoteListener(peerId: Long, clientId: Long) {
        if (isInvalid(
                peerId,
                clientId
            ) || videoListener == null || inMeetingViewModel.isMe(this.peerId)
        ) return

        Timber.d("Remove remove video listener of client ID $clientId")
        inMeetingViewModel.removeChatRemoteVideoListener(
            videoListener!!,
            clientId,
            chatId,
            true
        )

        videoListener = null
        Timber.d("Participant $clientId video listener null")
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

    override fun onDestroyView() {
        Timber.d("View destroyed")
        removeChatVideoListener()
        avatarImageView.setImageBitmap(null)
        super.onDestroyView()
    }
}