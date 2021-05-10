package mega.privacy.android.app.meeting.fragments

import android.os.Bundle
import android.util.Pair
import android.view.LayoutInflater
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.jeremyliao.liveeventbus.LiveEventBus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.individual_call_fragment.view.avatar
import kotlinx.android.synthetic.main.individual_call_fragment.view.on_hold_icon
import kotlinx.android.synthetic.main.individual_call_fragment.view.video
import kotlinx.android.synthetic.main.self_feed_floating_window_fragment.view.*
import mega.privacy.android.app.components.RoundedImageView
import mega.privacy.android.app.databinding.IndividualCallFragmentBinding
import mega.privacy.android.app.databinding.SelfFeedFloatingWindowFragmentBinding
import mega.privacy.android.app.meeting.listeners.MeetingVideoListener
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.LogUtil.logError
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaChatCall
import nz.mega.sdk.MegaChatSession

@AndroidEntryPoint
class IndividualCallFragment : MeetingBaseFragment() {

    private var chatId: Long? = MEGACHAT_INVALID_HANDLE
    private var peerId: Long? = MEGACHAT_INVALID_HANDLE
    private var clientId: Long? = MEGACHAT_INVALID_HANDLE

    private var isFloatingWindow = false

    // Views
    private lateinit var vVideo: SurfaceView
    private var vAvatarLayout: RelativeLayout? = null
    private lateinit var vAvatar: RoundedImageView
    private lateinit var vOnHold: ImageView

    private val inMeetingViewModel by viewModels<InMeetingViewModel>()

    private var videoListener: MeetingVideoListener? = null

    var videoAlpha = 255

    private val remoteAVFlagsObserver =
        Observer<Pair<Long, MegaChatSession>> { callAndSession ->
            when {
                inMeetingViewModel.isOneToOneCall() && inMeetingViewModel.isSameCall(callAndSession.first) -> {
                    logDebug("Check changes in remote AVFlags")
                    when {
                        isFloatingWindow -> checkItIsOnlyAudio()
                        else -> {
                            when {
                                callAndSession.second.hasVideo() -> {
                                    logDebug("Check if video should be on")
                                    checkVideoOn(
                                        callAndSession.second.peerid,
                                        callAndSession.second.clientid
                                    )
                                }
                                else -> {
                                    logDebug("Video should be off")
                                    videoOffUI(this.peerId!!, this.clientId!!)
                                }
                            }
                        }
                    }
                }
            }
        }

    private val localAVFlagsObserver = Observer<MegaChatCall> {
        when {
            inMeetingViewModel.isSameCall(it.callId) -> {
                logDebug("Check changes in local AVFlags")
                checkItIsOnlyAudio()
            }
        }
    }

    private val callOnHoldObserver = Observer<MegaChatCall> {
        when {
            inMeetingViewModel.isSameCall(it.callId) -> {
                logDebug("Check changes in call on hold")
                checkChangesInOnHold(it.isOnHold)
            }
        }
    }

    private val sessionOnHoldObserver =
        Observer<Pair<Long, MegaChatSession>> { callAndSession ->
            when {
                inMeetingViewModel.isOneToOneCall() && inMeetingViewModel.isSameCall(callAndSession.first) -> {
                    logDebug("Check changes in session on hold")
                    checkChangesInOnHold(
                        callAndSession.second.isOnHold
                    )
                }
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

        when (chatId) {
            MEGACHAT_INVALID_HANDLE -> {
                logError("Error. Chat doesn't exist")
                return
            }
            else -> {
                chatId?.let { inMeetingViewModel.setChatId(it) }
                when {
                    inMeetingViewModel.getCall() == null || peerId == MEGACHAT_INVALID_HANDLE -> {
                        logError("Error. Call doesn't exist")
                        return
                    }

                    !inMeetingViewModel.isMe(peerId) && clientId == MEGACHAT_INVALID_HANDLE -> {
                        logError("Error. Client id invalid")
                        return
                    }
                    else -> initLiveEventBus()
                }
            }
        }
    }

    private fun initLiveEventBus() {
        LiveEventBus.get(Constants.EVENT_LOCAL_AVFLAGS_CHANGE, MegaChatCall::class.java)
            .observeSticky(this, localAVFlagsObserver)

        LiveEventBus.get(Constants.EVENT_CALL_ON_HOLD_CHANGE, MegaChatCall::class.java)
            .observeSticky(this, callOnHoldObserver)

        LiveEventBus.get(Constants.EVENT_REMOTE_AVFLAGS_CHANGE)
            .observeSticky(this, remoteAVFlagsObserver as Observer<Any>)

        LiveEventBus.get(Constants.EVENT_SESSION_ON_HOLD_CHANGE)
            .observeSticky(this, sessionOnHoldObserver as Observer<Any>)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        when {
            isFloatingWindow -> {
                val binding = SelfFeedFloatingWindowFragmentBinding.inflate(
                    inflater,
                    container,
                    false
                )

                binding.root.let {
                    vVideo = it.video
                    vAvatar = it.avatar
                    vAvatarLayout = it.avatar_layout
                    vOnHold = it.on_hold_icon
                }
                return binding.root
            }
            else -> {
                val binding = IndividualCallFragmentBinding.inflate(
                    inflater,
                    container,
                    false
                )

                binding.root.let {
                    vVideo = it.video
                    vAvatar = it.avatar
                    vAvatarLayout = null
                    vOnHold = it.on_hold_icon
                }
                return binding.root
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        inMeetingViewModel.getAvatarBitmap(peerId!!)?.let {
            vAvatar.setImageBitmap(it)
        }

        checkUI()

        when {
            isFloatingWindow -> {
                when (BottomSheetBehavior.STATE_EXPANDED) {
                    (parentFragment as InMeetingFragment).bottomFloatingPanelViewHolder.getState() -> {
                        view.alpha = 0f
                    }
                }

                (parentFragment as InMeetingFragment).bottomFloatingPanelViewHolder.propertyUpdaters.apply {
                    add {
                        view.alpha = 1 - it
                    }
                    add {
                        videoAlpha = ((1 - it) * 255).toInt()
                    }
                }
            }
        }
    }

    /**
     * Initialising the UI
     */
    private fun checkUI() {
        logDebug("Check the current UI status")
        inMeetingViewModel.getCall()?.let {
            when {
                inMeetingViewModel.isMe(this.peerId) -> {
                    when {
                        it.hasLocalVideo() -> {
                            logDebug("Check if video should be on")
                            checkVideoOn(
                                this.peerId!!,
                                this.clientId!!
                            )
                        }
                        else -> {
                            logDebug("Video should be off")
                            videoOffUI(this.peerId!!, this.clientId!!)
                        }
                    }

                }
                else -> {
                    val session = inMeetingViewModel.getSession(this.clientId!!)
                    session?.let {
                        when {
                            it.hasVideo() -> {
                                logDebug("Check if video should be on")
                                checkVideoOn(
                                    this.peerId!!,
                                    this.clientId!!
                                )
                            }
                            else -> {
                                logDebug("Video should be off")
                                videoOffUI(this.peerId!!, this.clientId!!)
                            }
                        }
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
        when {
            peerId != this.peerId || clientId != this.clientId -> return
            else -> {
                logDebug("UI video off")
                showAvatar(peerId, clientId)
                checkItIsOnlyAudio()
                closeVideo(peerId, clientId)
                showCallOnHoldIcon()
            }
        }
    }

    /**
     * Method to show the Avatar
     *
     * @param peerId
     * @param clientId
     */
    private fun showAvatar(peerId: Long, clientId: Long) {
        when {
            peerId != this.peerId || clientId != this.clientId -> return
            else -> {
                logDebug("Show avatar")
                vAvatar.isVisible = true
                vAvatarLayout?.let {
                    it.isVisible = true
                }
            }
        }
    }

    /**
     * Check if is an audio call
     */
    private fun checkItIsOnlyAudio() {
        when {
            !isFloatingWindow || !inMeetingViewModel.isOneToOneCall() -> return
            else -> when {
                inMeetingViewModel.isAudioCall() -> this.peerId?.let {
                    logDebug("Is only audio call, hide avatar")
                    this.clientId?.let { it1 ->
                        hideAvatar(
                            it,
                            it1
                        )
                    }
                }
                else -> {
                    inMeetingViewModel.getCall()?.let { call ->
                        when {
                            !call.hasLocalVideo() -> {
                                logDebug("Not only audio call, show avatar")
                                showAvatar(this.peerId!!, this.clientId!!)
                            }
                        }
                    }
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
        when {
            peerId != this.peerId || clientId != this.clientId -> return
        }

        logDebug("Close video")
        vVideo.isVisible = false
        removeChatVideoListener(peerId, clientId)
    }

    /**
     * Remove chat video listener
     *
     * @param peerId
     * @param clientId
     */
    private fun removeChatVideoListener(peerId: Long, clientId: Long) {
        when {
            peerId != this.peerId || clientId != this.clientId -> return
        }

        when (videoListener) {
            null -> return
            else -> {
                logDebug("Remove local video listener")
                when {
                    inMeetingViewModel.isMe(this.peerId) -> {
                        sharedModel.removeLocalVideo(chatId!!, videoListener!!)
                    }
                    else -> {
                        inMeetingViewModel.getSession(clientId)?.let {
                            inMeetingViewModel.removeHiRes(videoListener!!, it, chatId!!)
                        }
                    }
                }

                videoListener = null
            }
        }
    }

    /**
     * Method to control the Call on hold icon visibility
     */
    private fun showCallOnHoldIcon() {
        when {
            inMeetingViewModel.isOneToOneCall() && inMeetingViewModel.isCallOrSessionOnHoldOfOneToOneCall() && !isFloatingWindow ||
                    !inMeetingViewModel.isOneToOneCall() && inMeetingViewModel.isCallOnHold() -> {
                logDebug("Show on hold icon")
                vOnHold.isVisible = true
                vAvatar.alpha = 0.5f
            }
            else -> {
                logDebug("Hide on hold icon")
                vOnHold.isVisible = false
                vAvatar.alpha = 1f
            }
        }
    }

    /**
     * Show UI when video is on
     *
     * @param peerId
     * @param clientId
     */
    private fun videoOnUI(peerId: Long, clientId: Long) {
        when {
            peerId != this.peerId || clientId != this.clientId -> return
            else -> {
                logDebug("UI video on")
                hideAvatar(peerId, clientId)
                activateVideo(peerId, clientId)
            }
        }
    }

    /**
     * Control when a change is received in the video flag
     *
     * @param peerId
     * @param clientId
     */
    fun checkVideoOn(peerId: Long, clientId: Long) {
        when {
            peerId != this.peerId || clientId != this.clientId -> return
            else -> {
                when {
                    (!inMeetingViewModel.isMe(peerId) && inMeetingViewModel.isCallOrSessionOnHoldOfOneToOneCall()) || (inMeetingViewModel.isMe(
                        peerId
                    ) &&
                            ((inMeetingViewModel.isOneToOneCall() && inMeetingViewModel.isCallOrSessionOnHoldOfOneToOneCall()) ||
                                    (!inMeetingViewModel.isOneToOneCall() && inMeetingViewModel.isCallOnHold()))) -> {
                        logDebug("The video should not be turned on")
                        return
                    }
                    else -> {
                        logDebug("The video should be turned on")
                        videoOnUI(peerId, clientId)
                    }
                }

            }
        }
    }

    /**
     * Method to hide the Avatar
     *
     * @param peerId
     * @param clientId
     */
    private fun hideAvatar(peerId: Long, clientId: Long) {
        when {
            peerId != this.peerId || clientId != this.clientId -> return
            else -> {
                logDebug("Hide Avatar")
                vOnHold.isVisible = false
                vAvatar.alpha = 1f
                vAvatar.isVisible = false
                vAvatarLayout?.let {
                    it.isVisible = false
                }
            }
        }
    }

    /**
     * Method for activating the video
     *
     * @param peerId
     * @param clientId
     */
    private fun activateVideo(peerId: Long, clientId: Long) {
        when {
            peerId != this.peerId || clientId != this.clientId -> return
        }

        when (videoListener) {
            null -> {
                logDebug("Video Listener is null ")
                when {
                    inMeetingViewModel.isMe(peerId) -> {
                        videoListener = MeetingVideoListener(
                            vVideo,
                            outMetrics,
                            megaChatApi.myUserHandle,
                            MEGACHAT_INVALID_HANDLE,
                            isFloatingWindow
                        )

                        sharedModel.addLocalVideo(chatId!!, videoListener)
                    }
                    else -> {
                        videoListener = MeetingVideoListener(
                            vVideo,
                            outMetrics,
                            peerId,
                            clientId,
                            false
                        )

                        inMeetingViewModel.getSession(clientId)?.let {
                            inMeetingViewModel.addHiRes(videoListener!!, it, chatId!!)
                        }
                    }
                }
            }
            else -> {
                logDebug("Video Listener is not null ")
                videoListener!!.height = 0
                videoListener!!.width = 0
            }
        }

        vVideo.isVisible = true
    }

    /**
     * Method to check if there is a call or session on hold
     *
     * @param isOnHold
     */
    private fun checkChangesInOnHold(isOnHold: Boolean) {
        when {
            isOnHold -> {
                logDebug("Call or session on hold")
                //It's on hold
                when {
                    inMeetingViewModel.isOneToOneCall() && inMeetingViewModel.isMe(this.peerId) && isFloatingWindow -> {
                        closeVideo(this.peerId!!, this.clientId!!)
                        checkItIsOnlyAudio()
                    }
                    else -> {
                        videoOffUI(this.peerId!!, this.clientId!!)
                    }
                }
            }
            else -> {
                //It is not on hold
                logDebug("Call or session is not on hold")
                checkUI()
            }
        }
    }

    /**
     * Method to destroy the surfaceView.
     */
    private fun removeSurfaceView() {
        vVideo.let { surfaceView ->
            when {
                surfaceView.parent != null && surfaceView.parent.parent != null -> {
                    logDebug("Removing surface view")
                    (surfaceView.parent as ViewGroup).removeView(surfaceView)
                }
            }
            surfaceView.isVisible = false
        }

        removeChatVideoListener(this.peerId!!, this.clientId!!)
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

    override fun onResume() {
        super.onResume()

        videoListener?.let {
            it.height = 0
            it.width = 0
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        removeSurfaceView()
        vAvatar.setImageBitmap(null)
    }
}