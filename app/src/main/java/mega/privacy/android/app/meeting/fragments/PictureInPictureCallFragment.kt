package mega.privacy.android.app.meeting.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.components.RoundedImageView
import mega.privacy.android.app.databinding.PictureInPictureCallFragmentBinding
import mega.privacy.android.app.meeting.listeners.IndividualCallVideoListener
import mega.privacy.android.domain.entity.call.ChatCallStatus
import mega.privacy.android.domain.entity.call.ChatSessionStatus
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import timber.log.Timber

/**
 * Fragment to show the video of a participant in Picture in Picture mode
 */
@AndroidEntryPoint
class PictureInPictureCallFragment : MeetingBaseFragment() {

    private var chatId: Long = MEGACHAT_INVALID_HANDLE
    private var clientId: Long = MEGACHAT_INVALID_HANDLE

    // Views
    private lateinit var rootLayout: ConstraintLayout
    private lateinit var videoTextureView: TextureView
    private lateinit var avatarImageView: RoundedImageView

    private lateinit var inMeetingFragment: InMeetingFragment

    private lateinit var inMeetingViewModel: InMeetingViewModel

    private var videoListener: IndividualCallVideoListener? = null

    /**
     * Method to create the view of the fragment
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val binding = PictureInPictureCallFragmentBinding.inflate(inflater, container, false)
        rootLayout = binding.root
        videoTextureView = binding.video
        avatarImageView = binding.avatar

        return binding.root
    }

    /**
     * Method to initialise the fragment
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rootLayout.addOnLayoutChangeListener {
                view: View?, oldLeft: Int,
                oldTop: Int, oldRight: Int, oldBottom: Int, newLeft: Int,
                newTop:
                Int,
                newRight: Int, newBottom: Int,
            ->
            Timber.d("width ${view?.width}, ${view?.height}")
            Timber.d("Old layout $oldLeft $oldTop $oldRight $oldBottom")
            Timber.d("Layout changed $newLeft $newTop $newRight $newBottom")
        }
        inMeetingFragment = parentFragment as InMeetingFragment

        inMeetingViewModel = inMeetingFragment.inMeetingViewModel
        inMeetingViewModel.state.value.let {
            chatId = it.currentChatId
        }
        Timber.d("Chat ID $chatId")
        collectFlows()
        checkInitialUI()
    }

    private fun collectFlows() {
        viewLifecycleOwner.collectFlow(inMeetingViewModel.state.map { it.changesInAVFlagsInSession }
            .distinctUntilChanged()) {
            it?.let { chatSession ->
                Timber.d("Chat Session $chatSession")
                Timber.d("Check changes in remote AVFlags")
                when {
                    chatSession.hasVideo && chatSession.status == ChatSessionStatus.Progress -> {
                        Timber.d("Check if video should be on")
                        checkVideoOn()
                    }

                    else -> {
                        Timber.d("Video should be off")
                        videoOffUI()
                    }
                }
            }
        }

        viewLifecycleOwner.collectFlow(inMeetingViewModel.state.map { it.isCallOnHold }
            .distinctUntilChanged()) {
            it?.let { isOnHold ->
                Timber.d("Check changes in call on hold")
                checkChangesInOnHold(isOnHold)
            }
        }

        viewLifecycleOwner.collectFlow(inMeetingViewModel.state.map { it.changesInHiResInSession }
            .distinctUntilChanged()) { session ->
            Timber.d("Check changes in HiRes video $session")
            session?.apply {
                val participant =
                    inMeetingViewModel.getParticipant(session.peerId, session.clientId)
                Timber.d("Participant $participant")

                participant?.also { currentActiveParticipant ->
                    if (this@PictureInPictureCallFragment.clientId != currentActiveParticipant.clientId) {
                        removeChatVideoListener()
                    }
                    this@PictureInPictureCallFragment.clientId = currentActiveParticipant.clientId
                    currentActiveParticipant.avatar?.let {
                        avatarImageView.setImageBitmap(it)
                    }
                    checkUI()
                }
            }
        }
    }

    private fun checkInitialUI() {
        Timber.d("Call ${inMeetingViewModel.getCall()}")
        val session =
            inMeetingViewModel.state.value.changesInStatusInSession
        Timber.d("Current Session $session")
        session?.also { currentSession ->
            this@PictureInPictureCallFragment.clientId = currentSession.clientId
            inMeetingViewModel.getAvatarBitmap(currentSession.peerId)?.let {
                avatarImageView.setImageBitmap(it)
            }
            inMeetingViewModel.requestHiResVideo(currentSession, this.chatId)
            checkUI()
        }
    }

    /**
     * Method to add video listener
     *
     */
    private fun addListener() {
        Timber.d("Adding Listener for $clientId")
        if (videoListener == null) {
            videoListener = IndividualCallVideoListener(
                videoTextureView,
                resources.displayMetrics,
                clientId,
                isFloatingWindow = false
            )
            Timber.d("Participant $clientId video listener created")
        }
        Timber.d("Add remote video listener of client ID $clientId")

        inMeetingViewModel.addChatRemoteVideoListener(
            listener = videoListener!!,
            clientId = clientId,
            chatId = chatId,
            isHiRes = true
        )
        rootLayout.isVisible = true
        videoTextureView.isVisible = true
        rootLayout.background = null
    }

    /**
     * Initialising the UI
     */
    private fun checkUI() {
        Timber.d("Check the current UI status")
        inMeetingViewModel.getCall()?.let {
            inMeetingViewModel.getSessionByClientId(clientId)?.let { chatSession ->
                if (chatSession.status == ChatSessionStatus.Progress && chatSession.hasVideo) {
                    Timber.d("Check if remote video should be on")
                    checkVideoOn()
                } else {
                    Timber.d("Remote video should be off")
                    videoOffUI()
                }
            }
        }
    }

    /**
     * Show UI when video is off
     */
    private fun videoOffUI() {
        Timber.d("UI video off")
        showAvatar()
        closeVideo()
        showCallOnHoldIcon()
    }

    /**
     * Method to show the Avatar
     */
    private fun showAvatar() {

        Timber.d("Show avatar")
        avatarImageView.isVisible = true
    }

    /**
     * Method to close Video
     */
    private fun closeVideo() {
        Timber.d("Close video of $clientId")
        videoTextureView.isVisible = false
        removeChatVideoListener()
    }

    /**
     * Method to control the Call on hold icon visibility
     */
    private fun showCallOnHoldIcon() {
        val isCallOnHold = inMeetingViewModel.state.value.isCallOnHold == true
        val isSessionOnHold = inMeetingViewModel.state.value.isSessionOnHold == true

        if (isCallOnHold || isSessionOnHold) {
            avatarImageView.alpha = 0.5f
            return
        }

        Timber.d("Hide on hold icon")
        avatarImageView.alpha = 1f

    }

    /**
     * Show UI when video is on
     */
    private fun videoOnUI() {
        inMeetingViewModel.getCall()?.let {
            Timber.d("UI video on")
            hideAvatar()
            activateVideo()
        }
    }

    /**
     * Control when a change is received in the video flag
     */
    private fun checkVideoOn() {
        val currentCall = inMeetingViewModel.getCall()
        if ((currentCall != null && currentCall.status != ChatCallStatus.Joining && currentCall.status != ChatCallStatus.InProgress) ||
            inMeetingViewModel.isCallOnHold()
        ) {
            Timber.d("The video should be turned off")
            videoOffUI()
            return
        }

        Timber.d("The video should be turned on")
        videoOnUI()
    }

    /**
     * Method to hide the Avatar
     */
    private fun hideAvatar() {
        Timber.d("Hide Avatar")
        avatarImageView.alpha = 1f
        avatarImageView.isVisible = false
    }

    /**
     * Method for activating the video
     */
    private fun activateVideo() {
        inMeetingViewModel.getSessionByClientId(clientId)?.also {
            Timber.d("ChatSession $it of client id $clientId")
            if (!it.canReceiveVideoHiRes && it.isHiResVideo) {
                Timber.d("Asking for HiRes video of client ID $clientId")
                inMeetingViewModel.requestHiResVideo(it, this.chatId)
            } else {
                Timber.d("I am already receiving the HiRes video")
                if (inMeetingViewModel.sessionHasVideo(clientId)) {
                    Timber.d("Session has video")
                    addListener()
                }
            }
        }
        videoTextureView.isVisible = true
    }

    /**
     * Method to check if there is a call or session on hold
     *
     * @param isOnHold True, if the call or session is on hold. False, if not
     */
    private fun checkChangesInOnHold(isOnHold: Boolean) {
        if (isOnHold) {
            Timber.d("Call or session on hold")
            videoOffUI()
        } else {
            //It is not on hold
            Timber.d("Call or session is not on hold")
            checkUI()
        }
    }

    /**
     * Remove chat video listener
     */
    fun removeChatVideoListener() {
        Timber.d("Removing Video Listener")
        videoListener?.let {
            removeResolutionOrLocalListener(this.clientId)
            removeRemoteListener(this.clientId)
        }
    }

    /**
     * Remove chat video listener
     */
    private fun removeResolutionOrLocalListener(clientId: Long) {
        inMeetingViewModel.getSessionByClientId(clientId)?.let { session ->
            videoListener?.let {
                if (session.canReceiveVideoHiRes) {
                    Timber.d("Removing HiRes video of client ID $clientId")
                    inMeetingViewModel.stopHiResVideo(session, chatId)
                }
            }
        }
    }

    /**
     * Method to remove video listener
     *
     * @param clientId Client ID of a participant
     */
    private fun removeRemoteListener(clientId: Long) {
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

        /**
         * Tag to identify the fragment
         */
        const val TAG = "PictureInPictureCallFragment"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         * @return A new instance of fragment PictureInPictureCallFragment.
         */
        @JvmStatic
        fun newInstance() =
            PictureInPictureCallFragment()
    }

    /**
     * Method to destroy the view
     */
    override fun onDestroyView() {
        Timber.d("View destroyed")
        removeChatVideoListener()
        avatarImageView.setImageBitmap(null)
        super.onDestroyView()
    }
}
