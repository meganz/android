package mega.privacy.android.app.meeting.pip

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.meeting.adapter.Participant
import mega.privacy.android.app.meeting.fragments.InMeetingFragment
import mega.privacy.android.app.meeting.fragments.InMeetingViewModel
import mega.privacy.android.app.meeting.fragments.MeetingBaseFragment
import mega.privacy.android.domain.entity.call.ChatCallStatus
import mega.privacy.android.domain.entity.call.ChatSessionStatus
import timber.log.Timber

/**
 * Fragment to show the video of a participant in Picture in Picture mode
 */
@AndroidEntryPoint
class PictureInPictureCallFragment : MeetingBaseFragment() {

    private lateinit var inMeetingFragment: InMeetingFragment

    private lateinit var inMeetingViewModel: InMeetingViewModel

    private val pictureInPictureCallViewModel: PictureInPictureCallViewModel by viewModels()

    /**
     * Method to create the view of the fragment
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                PictureInPictureCallScreen(pictureCallViewModel = pictureInPictureCallViewModel)
            }

            addOnLayoutChangeListener {
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
        }
    }

    /**
     * Method to initialise the fragment
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        inMeetingFragment = parentFragment as InMeetingFragment

        inMeetingViewModel = inMeetingFragment.inMeetingViewModel
        inMeetingViewModel.state.value.let {
            pictureInPictureCallViewModel.setChatId(chatId = it.currentChatId)
            Timber.d("Chat ID $it.chatId")
        }
        collectFlows()
        checkParticipantsUI()
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
                        pictureInPictureCallViewModel.cancelVideUpdates()
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
                checkParticipantsUI()
            }
        }

        inMeetingViewModel.participants.observe(viewLifecycleOwner) {
            checkParticipantsUI()
        }
    }

    private fun getCurrentParticipant(): Participant? {
        val participant =
            inMeetingViewModel.getCurrentSpeakerParticipant()
                ?.takeIf { it.clientId != -1L }
                ?: inMeetingViewModel.getFirstParticipant(-1, -1)
        Timber.d("Participant $participant")
        return participant
    }

    private fun checkParticipantsUI() {
        val participant = getCurrentParticipant()
        participant?.also { currentActiveParticipant ->
            Timber.d("previousClientId ${pictureInPictureCallViewModel.uiState.value.chatId} and new client Id ${currentActiveParticipant.clientId}")
            if (pictureInPictureCallViewModel.uiState.value.chatId != currentActiveParticipant.clientId) {
                pictureInPictureCallViewModel.setClientAndPeerId(
                    currentActiveParticipant.clientId,
                    currentActiveParticipant.peerId
                )
            }
            checkUI()
        } ?: run {
            inMeetingViewModel.getCall()?.run {
                Timber.d("Chat Session $sessionsClientId and number of participants $numParticipants hasLocalVideo $hasLocalVideo")
                // need to render only the local video or avatar
                pictureInPictureCallViewModel.setClientAndPeerId(
                    clientId = -1,
                    inMeetingViewModel.state.value.myUserHandle ?: -1
                )
                if (hasLocalVideo) {
                    pictureInPictureCallViewModel.showVideoUpdates()
                }
            }
        }
    }

    /**
     * Initialising the UI
     */
    private fun checkUI() {
        Timber.d("Check the current UI status")
        inMeetingViewModel.getCall()?.let {
            inMeetingViewModel.getSessionByClientId(pictureInPictureCallViewModel.uiState.value.clientId)
                ?.let { chatSession ->
                    if (chatSession.status == ChatSessionStatus.Progress && chatSession.hasVideo) {
                        Timber.d("Check if remote video should be on")
                        checkVideoOn()
                    } else {
                        Timber.d("Remote video should be off")
                        pictureInPictureCallViewModel.cancelVideUpdates()
                    }
                }
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
            pictureInPictureCallViewModel.cancelVideUpdates()
            return
        }

        Timber.d("The video should be turned on")
        activateVideo()
    }

    /**
     * Method for activating the video
     */
    private fun activateVideo() {
        val clientId = pictureInPictureCallViewModel.uiState.value.clientId
        val chatId = pictureInPictureCallViewModel.uiState.value.chatId
        Timber.d("Activate video of $clientId")
        inMeetingViewModel.getSessionByClientId(clientId)?.also {
            Timber.d("ChatSession $it of client id $clientId")
            if (!it.canReceiveVideoHiRes && it.isHiResVideo) {
                Timber.d("Asking for HiRes video of client ID $clientId")
                inMeetingViewModel.requestHiResVideo(it, chatId)
            } else {
                Timber.d("I am already receiving the HiRes video")
                if (inMeetingViewModel.sessionHasVideo(clientId)) {
                    Timber.d("Session has video")
                    pictureInPictureCallViewModel.showVideoUpdates()
                }
            }
        }
    }

    /**
     * Method to check if there is a call or session on hold
     *
     * @param isOnHold True, if the call or session is on hold. False, if not
     */
    private fun checkChangesInOnHold(isOnHold: Boolean) {
        if (isOnHold) {
            Timber.d("Call or session on hold")
            pictureInPictureCallViewModel.cancelVideUpdates()
        } else {
            //It is not on hold
            Timber.d("Call or session is not on hold")
            checkUI()
        }
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
        super.onDestroyView()
    }
}
