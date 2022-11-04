package mega.privacy.android.app.meeting.fragments

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.SystemClock
import android.util.DisplayMetrics
import android.util.Pair
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Chronometer
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jeremyliao.liveeventbus.LiveEventBus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.components.ChatManagement
import mega.privacy.android.app.components.twemoji.EmojiTextView
import mega.privacy.android.app.constants.EventConstants.EVENT_CALL_COMPOSITION_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_CALL_ON_HOLD_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_CALL_STATUS_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_CHAT_CONNECTION_STATUS
import mega.privacy.android.app.constants.EventConstants.EVENT_CONTACT_NAME_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_ENABLE_OR_DISABLE_LOCAL_VIDEO_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_ERROR_STARTING_CALL
import mega.privacy.android.app.constants.EventConstants.EVENT_MEETING_AVATAR_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_MEETING_GET_AVATAR
import mega.privacy.android.app.constants.EventConstants.EVENT_NOT_OUTGOING_CALL
import mega.privacy.android.app.constants.EventConstants.EVENT_PRIVILEGES_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_REMOTE_AVFLAGS_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_SESSION_ON_HIRES_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_SESSION_ON_HOLD_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_SESSION_ON_LOWRES_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_SESSION_STATUS_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_USER_VISIBILITY_CHANGE
import mega.privacy.android.app.databinding.InMeetingFragmentBinding
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.listeners.AutoJoinPublicChatListener
import mega.privacy.android.app.listeners.ChatChangeVideoStreamListener
import mega.privacy.android.app.listeners.SimpleChatRequestListener
import mega.privacy.android.app.listeners.SimpleMegaRequestListener
import mega.privacy.android.app.main.AddContactActivity
import mega.privacy.android.app.main.megachat.AppRTCAudioManager
import mega.privacy.android.app.mediaplayer.service.MediaPlayerService.Companion.pauseAudioPlayer
import mega.privacy.android.app.mediaplayer.service.MediaPlayerService.Companion.resumeAudioPlayerIfNotInCall
import mega.privacy.android.app.meeting.AnimationTool.fadeInOut
import mega.privacy.android.app.meeting.AnimationTool.moveX
import mega.privacy.android.app.meeting.AnimationTool.moveY
import mega.privacy.android.app.meeting.OnDragTouchListener
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_ACTION_CREATE
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_ACTION_GUEST
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_ACTION_JOIN
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_ACTION_REJOIN
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_ACTION_RINGING_VIDEO_OFF
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_ACTION_RINGING_VIDEO_ON
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_ACTION_START
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_IS_GUEST
import mega.privacy.android.app.meeting.adapter.Participant
import mega.privacy.android.app.meeting.gateway.RTCAudioManagerGateway
import mega.privacy.android.app.meeting.listeners.BottomFloatingPanelListener
import mega.privacy.android.app.objects.PasscodeManagement
import mega.privacy.android.app.presentation.chat.dialog.AddParticipantsNoContactsDialogFragment
import mega.privacy.android.app.presentation.chat.dialog.AddParticipantsNoContactsLeftToAddDialogFragment
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.ChatUtil
import mega.privacy.android.app.utils.Constants.AVATAR_CHANGE
import mega.privacy.android.app.utils.Constants.CONTACT_TYPE_MEGA
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_IS_FROM_MEETING
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_CHAT
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_CHAT_ID
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_CONTACT_TYPE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_TOOL_BAR_TITLE
import mega.privacy.android.app.utils.Constants.INVALID_CALL_STATUS
import mega.privacy.android.app.utils.Constants.INVALID_POSITION
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import mega.privacy.android.app.utils.Constants.NAME_CHANGE
import mega.privacy.android.app.utils.Constants.PERMISSIONS_TYPE
import mega.privacy.android.app.utils.Constants.REQUEST_ADD_PARTICIPANTS
import mega.privacy.android.app.utils.Constants.SECONDS_TO_WAIT_ALONE_ON_THE_CALL
import mega.privacy.android.app.utils.Constants.SNACKBAR_TYPE
import mega.privacy.android.app.utils.Constants.TYPE_AUDIO
import mega.privacy.android.app.utils.Constants.TYPE_LEFT
import mega.privacy.android.app.utils.Constants.TYPE_VIDEO
import mega.privacy.android.app.utils.RunOnUIThreadUtils
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.Util.isOnline
import mega.privacy.android.app.utils.VideoCaptureUtils
import mega.privacy.android.app.utils.permission.PermissionUtils
import mega.privacy.android.app.utils.permission.permissionsBuilder
import mega.privacy.android.data.qualifier.MegaApi
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaChatCall
import nz.mega.sdk.MegaChatListItem
import nz.mega.sdk.MegaChatRequest
import nz.mega.sdk.MegaChatRoom
import nz.mega.sdk.MegaChatSession
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaUser.VISIBILITY_VISIBLE
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class InMeetingFragment : MeetingBaseFragment(), BottomFloatingPanelListener, SnackbarShower,
    AutoJoinPublicChatListener.OnJoinedChatCallback {

    @Inject
    @MegaApi
    lateinit var megaApi: MegaApiAndroid

    @Inject
    lateinit var passcodeManagement: PasscodeManagement

    @Inject
    lateinit var rtcAudioManagerGateway: RTCAudioManagerGateway

    @Inject
    lateinit var chatManagement: ChatManagement

    val args: InMeetingFragmentArgs by navArgs()

    // Views
    lateinit var toolbar: MaterialToolbar
    lateinit var blink: Animation

    private var toolbarTitle: EmojiTextView? = null
    private var toolbarSubtitle: TextView? = null
    private var meetingChrono: Chronometer? = null

    private lateinit var bannerAnotherCallLayout: View
    private var bannerAnotherCallTitle: EmojiTextView? = null
    private var bannerAnotherCallSubtitle: TextView? = null

    private lateinit var bannerMuteLayout: View
    private var bannerMuteText: EmojiTextView? = null
    private var bannerMuteIcon: ImageView? = null

    private var participantsChangesBanner: EmojiTextView? = null
    private var callBanner: TextView? = null
    private var bannerInfo: TextView? = null

    private lateinit var floatingWindowContainer: View
    private lateinit var floatingBottomSheet: View

    lateinit var bottomFloatingPanelViewHolder: BottomFloatingPanelViewHolder

    private var swapCameraMenuItem: MenuItem? = null
    private var gridViewMenuItem: MenuItem? = null
    private var speakerViewMenuItem: MenuItem? = null

    private var micIsEnable = false
    private var camIsEnable = false
    private var speakerIsEnable = false
    private var meetingLink: String = ""
    private var isManualModeView = false
    private var isWaitingForAnswerCall = false
    private var isWaitingForMakeModerator = false

    // Children fragments
    private var individualCallFragment: IndividualCallFragment? = null
    private var floatingWindowFragment: IndividualCallFragment? = null
    private var gridViewCallFragment: GridViewCallFragment? = null
    private var speakerViewCallFragment: SpeakerViewCallFragment? = null

    // For internal UI/UX use
    private var previousY = -1f
    private var lastTouch: Long = 0
    private lateinit var dragTouchListener: OnDragTouchListener
    private var bannerShouldBeShown = false

    private lateinit var binding: InMeetingFragmentBinding

    // Leave Meeting Dialog
    private var leaveDialog: Dialog? = null

    // Meeting failed Dialog
    private var failedDialog: Dialog? = null

    private var assignModeratorDialog:AssignModeratorBottomSheetDialogFragment? = null
    private var endMeetingAsModeratorDialog:EndMeetingAsModeratorBottomSheetDialogFragment? = null

    // Only me in the call Dialog
    private var onlyMeDialog: Dialog? = null

    private var countDownTimerToEndCall: CountDownTimer? = null

    val inMeetingViewModel: InMeetingViewModel by activityViewModels()

    private val enableOrDisableLocalVideoObserver = Observer<Boolean> { shouldBeEnabled ->
        val chatId = inMeetingViewModel.getChatId()
        if (chatId != MEGACHAT_INVALID_HANDLE && inMeetingViewModel.getCall() != null && shouldBeEnabled != camIsEnable) {
            MegaApplication.getChatManagement().isDisablingLocalVideo = true
            sharedModel.clickCamera(shouldBeEnabled)
        }
    }

    private val errorStatingCallObserver = Observer<Long> {
        if (inMeetingViewModel.isSameChatRoom(it)) {
            Timber.e("Error starting a call")
            showMeetingFailedDialog()
        }
    }

    private val nameChangeObserver = Observer<Long> { peerId ->
        if (peerId != MegaApiJava.INVALID_HANDLE) {
            Timber.d("Change in name")
            updateParticipantInfo(peerId, NAME_CHANGE)
        }
    }

    // Observer for getting avatar
    private val getAvatarObserver = Observer<Long> { peerId ->
        if (peerId != MegaApiJava.INVALID_HANDLE) {
            Timber.d("Change in avatar")
            updateParticipantInfo(peerId, AVATAR_CHANGE)
        }
    }

    // Observer for changing avatar
    private val avatarChangeObserver = Observer<Long> { peerId ->
        if (peerId != MegaApiJava.INVALID_HANDLE) {
            Timber.d("Change in avatar")
            inMeetingViewModel.getRemoteAvatar(peerId)
        }
    }

    private val noOutgoingCallObserver = Observer<Long> {
        if (inMeetingViewModel.isSameCall(it)) {
            val call = inMeetingViewModel.getCall()
            call?.let { chatCall ->
                Timber.d("The call is no longer an outgoing call")
                enableOnHoldFab(chatCall.isOnHold)
            }
        }
    }

    private val visibilityChangeObserver = Observer<Long> {
        Timber.d("Change in the visibility of a participant")
        inMeetingViewModel.updateParticipantsVisibility(it)
    }

    private val privilegesChangeObserver = Observer<MegaChatListItem> { item ->
        if (inMeetingViewModel.isSameChatRoom(item.chatId)) {
            inMeetingViewModel.getCall()?.let { call ->
                if (call.status == MegaChatCall.CALL_STATUS_IN_PROGRESS) {
                    if (item.hasChanged(MegaChatListItem.CHANGE_TYPE_OWN_PRIV)) {
                        Timber.d("Change in my privileges")
                        if (MegaChatRoom.PRIV_MODERATOR == inMeetingViewModel.getOwnPrivileges()) {
                            showSnackbar(
                                SNACKBAR_TYPE,
                                StringResourcesUtils.getString(R.string.be_new_moderator),
                                MEGACHAT_INVALID_HANDLE
                            )
                        }

                        inMeetingViewModel.updateOwnPrivileges()
                        bottomFloatingPanelViewHolder.updateShareAndInviteButton()
                    }

                    if (item.hasChanged(MegaChatListItem.CHANGE_TYPE_PARTICIPANTS)) {
                        Timber.d("Change in the privileges of a participant")
                        inMeetingViewModel.updateParticipantsPrivileges().run {
                            updateRemotePrivileges(this)
                        }
                    }
                }
            }
        }
    }

    private val callStatusObserver = Observer<MegaChatCall> {
        if (inMeetingViewModel.isSameCall(it.callId)) {
            updatePanel()

            when (it.status) {
                MegaChatCall.CALL_STATUS_INITIAL -> {
                    bottomFloatingPanelViewHolder.disableEnableButtons(
                        false,
                        inMeetingViewModel.isCallOnHold()
                    )
                }
                MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION,
                MegaChatCall.CALL_STATUS_DESTROYED,
                -> {
                    disableCamera()
                    removeUI()
                }
                MegaChatCall.CALL_STATUS_CONNECTING -> {
                    bottomFloatingPanelViewHolder.disableEnableButtons(
                        false,
                        inMeetingViewModel.isCallOnHold()
                    )

                    checkMenuItemsVisibility()
                }
                MegaChatCall.CALL_STATUS_IN_PROGRESS -> {
                    bottomFloatingPanelViewHolder.disableEnableButtons(
                        true,
                        inMeetingViewModel.isCallOnHold()
                    )

                    checkMenuItemsVisibility()
                    checkChildFragments()
                    controlVideoLocalOneToOneCall(it.hasLocalVideo())
                }
            }
        }
    }

    /**
     * Method that updates the Bottom panel
     */
    private fun updatePanel() {
        bottomFloatingPanelViewHolder.updatePanel(false)
    }

    private val callCompositionObserver = Observer<MegaChatCall> {
        if (inMeetingViewModel.isSameCall(it.callId) &&
            it.status != INVALID_CALL_STATUS &&
            (it.callCompositionChange == 1 || it.callCompositionChange == -1)
        ) {
            if (inMeetingViewModel.showReconnectingBanner.value || !isOnline(
                    requireContext()
                )
            ) {
                Timber.d("Back from reconnecting")
            } else {
                Timber.d("Change in call composition, review the UI")
                if (inMeetingViewModel.isOneToOneCall()) {
                    if (it.numParticipants == 1 || it.numParticipants == 2) {
                        checkChildFragments()
                    }
                } else {
                    checkChildFragments()
                }
            }
        }
    }

    private val callOnHoldObserver = Observer<MegaChatCall> {
        if (inMeetingViewModel.isSameCall(it.callId)) {
            Timber.d("Change in call on hold status")
            isCallOnHold(it.isOnHold)
            checkSwapCameraMenuItemVisibility()
        }
    }

    private val sessionOnHoldObserver =
        Observer<Pair<Long, MegaChatSession>> { callAndSession ->
            if (inMeetingViewModel.isSameCall(callAndSession.first)) {
                showMuteBanner()
                val call = inMeetingViewModel.getCall()
                call?.let {
                    Timber.d("Change in session on hold status")
                    if (!inMeetingViewModel.isOneToOneCall()) {
                        updateOnHoldRemote(callAndSession.second)
                    } else if (it.hasLocalVideo() && callAndSession.second.isOnHold) {
                        sharedModel.clickCamera(false)
                    }
                }
            }
        }

    private val sessionStatusObserver =
        Observer<Pair<MegaChatCall, MegaChatSession>> { callAndSession ->
            if (!inMeetingViewModel.isOneToOneCall()) {
                when (callAndSession.second.status) {
                    MegaChatSession.SESSION_STATUS_IN_PROGRESS -> {
                        Timber.d("Session in progress, clientID = ${callAndSession.second.clientid}")
                        val position =
                            inMeetingViewModel.addParticipant(
                                callAndSession.second
                            )
                        position?.let {
                            if (position != INVALID_POSITION) {
                                checkChildFragments()
                                participantAddedOfLeftMeeting(true, it)
                            }
                        }
                    }
                    MegaChatSession.SESSION_STATUS_DESTROYED -> {
                        Timber.d("Session destroyed, clientID = ${callAndSession.second.clientid}")
                        val position =
                            inMeetingViewModel.removeParticipant(callAndSession.second)
                        position.let {
                            if (position != INVALID_POSITION) {
                                checkChildFragments()
                                participantAddedOfLeftMeeting(false, it)
                            }
                        }
                    }
                }
            } else {
                checkChildFragments()
            }

            showMuteBanner()
        }

    private val remoteAVFlagsObserver =
        Observer<Pair<Long, MegaChatSession>> { callAndSession ->
            //As the session has been established, I am no longer in the Request sent state
            if (inMeetingViewModel.isSameCall(callAndSession.first)) {
                showMuteBanner()
                if (!inMeetingViewModel.isOneToOneCall()) {
                    val isAudioChange =
                        inMeetingViewModel.changesInRemoteAudioFlag(callAndSession.second)
                    val isVideoChange =
                        inMeetingViewModel.changesInRemoteVideoFlag(callAndSession.second)
                    Timber.d("Changes in AV flags. audio change $isAudioChange, video change $isVideoChange")
                    updateRemoteAVFlags(callAndSession.second, isAudioChange, isVideoChange)
                }
            }
        }

    private val chatConnectionStatusObserver =
        Observer<Pair<Long, Int>> { chatAndState ->
            if (inMeetingViewModel.isSameChatRoom(chatAndState.first) && MegaApplication.isWaitingForCall) {
                startCall()
            }
        }

    private val sessionLowResObserver =
        Observer<Pair<Long, MegaChatSession>> { callAndSession ->
            if (inMeetingViewModel.isSameCall(callAndSession.first) && !inMeetingViewModel.isOneToOneCall()) {
                inMeetingViewModel.getParticipant(
                    callAndSession.second.peerid,
                    callAndSession.second.clientid
                )?.let { participant ->
                    if (callAndSession.second.canRecvVideoLowRes() && callAndSession.second.isLowResVideo) {
                        if (!participant.hasHiRes) {
                            if (inMeetingViewModel.sessionHasVideo(participant.clientId)) {
                                Timber.d("Client ID ${callAndSession.second.clientid} can receive lowRes, has lowRes, video on, checking if listener should be added...")
                                checkVideoListener(
                                    participant,
                                    shouldAddListener = true,
                                    isHiRes = false
                                )
                            }
                        }
                    } else if (!participant.hasHiRes) {
                        Timber.d("Client ID ${callAndSession.second.clientid} can not receive lowRes, has lowRes, checking if listener should be removed...")
                        checkVideoListener(
                            participant,
                            shouldAddListener = false,
                            isHiRes = false
                        )

                        //I have stopped receiving LowResolution. I have to verify that I no longer need it.
                        if (callAndSession.second.hasVideo() && !inMeetingViewModel.isCallOrSessionOnHold(
                                callAndSession.second.clientid
                            )
                        ) {
                            inMeetingViewModel.getSession(callAndSession.second.clientid)
                                ?.let { session ->
                                    if (session.status == MegaChatSession.SESSION_STATUS_IN_PROGRESS && inMeetingViewModel.isParticipantVisible(
                                            participant
                                        )
                                    ) {
                                        Timber.d("Client ID ${callAndSession.second.clientid} can not receive lowRes, has lowRes, asking for low-resolution video...")
                                        inMeetingViewModel.requestLowResVideo(
                                            session,
                                            inMeetingViewModel.getChatId()
                                        )
                                    }
                                }
                        }
                    }
                }
            }
        }

    private val sessionHiResObserver =
        Observer<Pair<Long, MegaChatSession>> { callAndSession ->
            if (inMeetingViewModel.isSameCall(callAndSession.first) && !inMeetingViewModel.isOneToOneCall()) {
                inMeetingViewModel.getParticipant(
                    callAndSession.second.peerid,
                    callAndSession.second.clientid
                )?.let { participant ->
                    if (callAndSession.second.canRecvVideoHiRes() && callAndSession.second.isHiResVideo) {
                        if (participant.hasHiRes) {
                            if (inMeetingViewModel.sessionHasVideo(participant.clientId)) {
                                Timber.d("Client ID ${callAndSession.second.clientid} can receive hiRes, has hiRes, video on, checking if listener should be added...")
                                checkVideoListener(
                                    participant,
                                    shouldAddListener = true,
                                    isHiRes = true
                                )
                            }
                        } else {
                            if (inMeetingViewModel.sessionHasVideo(participant.clientId)) {
                                Timber.d("Client ID ${callAndSession.second.clientid} can receive hiRes, has lowRes, video on, checking if listener should be added for speaker...")
                                checkSpeakerVideoListener(
                                    callAndSession.second.peerid, callAndSession.second.clientid,
                                    shouldAddListener = true
                                )
                            }
                        }
                    } else if (participant.hasHiRes) {
                        Timber.d("Client ID ${callAndSession.second.clientid} can not receive hiRes, has hiRes, checking if listener should be removed...")
                        checkVideoListener(
                            participant,
                            shouldAddListener = false,
                            isHiRes = true
                        )

                        //I have stopped receiving HiResolution. I have to verify that I no longer need it.
                        if (callAndSession.second.hasVideo() && !inMeetingViewModel.isCallOrSessionOnHold(
                                callAndSession.second.clientid
                            )
                        ) {
                            inMeetingViewModel.getSession(callAndSession.second.clientid)
                                ?.let { session ->
                                    if (session.status == MegaChatSession.SESSION_STATUS_IN_PROGRESS && inMeetingViewModel.isParticipantVisible(
                                            participant
                                        )
                                    ) {
                                        Timber.d("Client ID ${callAndSession.second.clientid} can not receive hiRes, has hiRes, asking for high-resolution video...")
                                        inMeetingViewModel.requestHiResVideo(
                                            session,
                                            inMeetingViewModel.getChatId()
                                        )
                                    }
                                }
                        }
                    } else {
                        Timber.d("Client ID ${callAndSession.second.clientid} can not receive hiRes, has lowRes, checking if listener of speaker should be removed...")
                        checkSpeakerVideoListener(
                            callAndSession.second.peerid, callAndSession.second.clientid,
                            shouldAddListener = false
                        )
                    }
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = InMeetingFragmentBinding.inflate(inflater)

        floatingWindowContainer = binding.selfFeedFloatingWindowContainer
        floatingBottomSheet = binding.bottomFloatingPanel.root

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Timber.d("In the meeting fragment")

        blink = AnimationUtils.loadAnimation(requireContext(), R.anim.blink)

        initViewModel()
        MegaApplication.getInstance().startProximitySensor()
        initToolbar()
        initFloatingWindowContainerDragListener(view)
        initFloatingPanel()

        val meetingName: String = args.meetingName
        meetingName.let {
            if (!TextUtil.isTextEmpty(it)) {
                sharedModel.setMeetingsName(it)
            }
        }

        // Get meeting link from the arguments supplied when the fragment was instantiated
        meetingLink = args.meetingLink

        initLiveEventBus()
        takeActionByArgs()

        // Set on page tapping listener.
        setPageOnClickListener(view)
        checkChildFragments()
    }

    /**
     * Method for checking the different types of actions received
     */
    private fun takeActionByArgs() {
        when (args.action) {
            MEETING_ACTION_CREATE -> {
                Timber.d("Action create")
                updateMicAndCam()
                initStartMeeting()
            }
            MEETING_ACTION_JOIN -> {
                Timber.d("Action join")
                updateMicAndCam()

                inMeetingViewModel.joinPublicChat(
                    args.chatId,
                    AutoJoinPublicChatListener(requireContext(), this)
                )
            }
            MEETING_ACTION_REJOIN -> {
                Timber.d("Action rejoin")
                updateMicAndCam()

                if (args.publicChatHandle != MEGACHAT_INVALID_HANDLE) {
                    inMeetingViewModel.rejoinPublicChat(
                        args.chatId, args.publicChatHandle,
                        AutoJoinPublicChatListener(requireContext(), this)
                    )
                }

            }
            MEETING_ACTION_GUEST -> {
                Timber.d("Action guest")
                inMeetingViewModel.chatLogout(
                    SimpleChatRequestListener(
                        MegaChatRequest.TYPE_LOGOUT,
                        onSuccess = { _, _, _ ->
                            Timber.d("Action guest. Log out, done")
                            inMeetingViewModel.createEphemeralAccountAndJoinChat(
                                args.firstName,
                                args.lastName,
                                SimpleMegaRequestListener(
                                    MegaRequest.TYPE_CREATE_ACCOUNT,
                                    onSuccess = { _, _, _ ->
                                        Timber.d("Action guest. Create ephemeral Account, done")
                                        inMeetingViewModel.openChatPreview(
                                            meetingLink,
                                            SimpleChatRequestListener(
                                                MegaChatRequest.TYPE_LOAD_PREVIEW,
                                                onSuccess = { _, request, _ ->
                                                    Timber.d(
                                                        "Action guest. Open chat preview, done. Param type: ${request.paramType}, Chat id: ${request.chatHandle}, Flag: ${request.flag}, Call id: ${
                                                            request.megaHandleList?.get(
                                                                0
                                                            )
                                                        }"
                                                    )
                                                    MegaApplication.getChatManagement()
                                                        .setOpeningMeetingLink(
                                                            request.chatHandle,
                                                            true
                                                        )

                                                    camIsEnable =
                                                        sharedModel.cameraLiveData.value!!
                                                    speakerIsEnable =
                                                        sharedModel.speakerLiveData.value!! == AppRTCAudioManager.AudioDevice.SPEAKER_PHONE

                                                    inMeetingViewModel.joinPublicChat(
                                                        args.chatId,
                                                        AutoJoinPublicChatListener(
                                                            context,
                                                            this@InMeetingFragment
                                                        )
                                                    )
                                                })
                                        )
                                    })
                            )
                        })
                )
            }
            MEETING_ACTION_RINGING_VIDEO_ON -> {
                Timber.d("Action ringing with video on")
                inMeetingViewModel.getCall()?.let {
                    if (it.hasLocalAudio()) {
                        sharedModel.micInitiallyOn()
                    }
                    if (it.hasLocalVideo()) {
                        sharedModel.camInitiallyOn()
                    }
                }
            }
            MEETING_ACTION_RINGING_VIDEO_OFF -> {
                Timber.d("Action ringing with video off")
                inMeetingViewModel.getCall()?.let {
                    if (it.hasLocalAudio()) {
                        sharedModel.micInitiallyOn()
                    }
                }
            }
            MEETING_ACTION_START -> {
                Timber.d("Action need answer call")
                updateMicAndCam()
                controlWhenJoinedAChat(inMeetingViewModel.currentChatId)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        pauseAudioPlayer(meetingActivity)

        // Keep screen on
        meetingActivity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    /**
     * Observe the Orientation changes and Update the layout for landscape and portrait screen
     *
     * @param newConfig Portrait or landscape
     */
    @Suppress("DEPRECATION")
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val outMetrics = DisplayMetrics()
        val display = meetingActivity.windowManager.defaultDisplay
        display.getMetrics(outMetrics)
        bottomFloatingPanelViewHolder.updateWidth(newConfig.orientation, outMetrics.widthPixels)

        floatingWindowFragment?.let {
            if (it.isAdded) {
                it.updateOrientation()
            }
        }

        individualCallFragment?.let {
            if (it.isAdded) {
                it.updateOrientation()
            }
        }

        floatingWindowContainer.let {
            val menuLayoutParams = it.layoutParams as ViewGroup.MarginLayoutParams
            menuLayoutParams.setMargins(0, 0, 0, Util.dp2px(125f, outMetrics))
            it.layoutParams = menuLayoutParams
            onConfigurationChangedOfFloatingWindow(outMetrics)
        }

        gridViewCallFragment?.let {
            if (it.isAdded) {
                it.updateLayout(
                    outMetrics.widthPixels,
                    outMetrics.heightPixels
                )
            }
        }
    }

    private fun initLiveEventBus() {
        LiveEventBus.get(EVENT_ENABLE_OR_DISABLE_LOCAL_VIDEO_CHANGE, Boolean::class.java)
            .observe(this, enableOrDisableLocalVideoObserver)

        LiveEventBus.get(EVENT_ERROR_STARTING_CALL, Long::class.java)
            .observe(this, errorStatingCallObserver)

        LiveEventBus.get(EVENT_NOT_OUTGOING_CALL, Long::class.java)
            .observe(this, noOutgoingCallObserver)

        LiveEventBus.get(EVENT_CONTACT_NAME_CHANGE, Long::class.java)
            .observe(this, nameChangeObserver)

        LiveEventBus.get(EVENT_PRIVILEGES_CHANGE, MegaChatListItem::class.java)
            .observe(this, privilegesChangeObserver)

        LiveEventBus.get(EVENT_USER_VISIBILITY_CHANGE, Long::class.java)
            .observe(this, visibilityChangeObserver)

        LiveEventBus.get(EVENT_MEETING_GET_AVATAR, Long::class.java)
            .observe(this, getAvatarObserver)

        LiveEventBus.get(EVENT_MEETING_AVATAR_CHANGE, Long::class.java)
            .observe(this, avatarChangeObserver)

        //Calls level
        LiveEventBus.get(EVENT_CALL_STATUS_CHANGE, MegaChatCall::class.java)
            .observe(this, callStatusObserver)

        LiveEventBus.get(EVENT_CALL_COMPOSITION_CHANGE, MegaChatCall::class.java)
            .observe(this, callCompositionObserver)

        LiveEventBus.get(EVENT_CALL_ON_HOLD_CHANGE, MegaChatCall::class.java)
            .observe(this, callOnHoldObserver)

        //Sessions Level
        @Suppress("UNCHECKED_CAST")
        LiveEventBus.get(EVENT_SESSION_STATUS_CHANGE)
            .observe(this, sessionStatusObserver as Observer<Any>)
        @Suppress("UNCHECKED_CAST")
        LiveEventBus.get(EVENT_SESSION_ON_HOLD_CHANGE)
            .observe(this, sessionOnHoldObserver as Observer<Any>)
        @Suppress("UNCHECKED_CAST")
        LiveEventBus.get(EVENT_REMOTE_AVFLAGS_CHANGE)
            .observe(this, remoteAVFlagsObserver as Observer<Any>)
        @Suppress("UNCHECKED_CAST")
        LiveEventBus.get(EVENT_SESSION_ON_HIRES_CHANGE)
            .observe(this, sessionHiResObserver as Observer<Any>)
        @Suppress("UNCHECKED_CAST")
        LiveEventBus.get(EVENT_SESSION_ON_LOWRES_CHANGE)
            .observe(this, sessionLowResObserver as Observer<Any>)
        @Suppress("UNCHECKED_CAST")
        LiveEventBus.get(EVENT_CHAT_CONNECTION_STATUS)
            .observe(this, chatConnectionStatusObserver as Observer<Any>)
    }

    private fun initToolbar() {
        Timber.d("Update toolbar elements")
        val root = meetingActivity.binding.root
        toolbar = meetingActivity.binding.toolbar
        toolbarTitle = meetingActivity.binding.titleToolbar
        toolbarSubtitle = meetingActivity.binding.subtitleToolbar

        root.apply {
            setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.grey_900))
        }

        toolbar.apply {
            setBackgroundColor(ContextCompat.getColor(requireContext(),
                R.color.dark_grey_alpha_070))
        }

        meetingActivity.window.statusBarColor =
            ContextCompat.getColor(requireContext(), R.color.dark_grey_alpha_070)


        toolbarTitle?.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        toolbarSubtitle?.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))

        bannerAnotherCallLayout = meetingActivity.binding.bannerAnotherCall
        bannerAnotherCallTitle = meetingActivity.binding.bannerAnotherCallTitle
        bannerAnotherCallSubtitle = meetingActivity.binding.bannerAnotherCallSubtitle
        participantsChangesBanner = meetingActivity.binding.participantsChangesBanner
        callBanner = meetingActivity.binding.callBanner

        bannerInfo = meetingActivity.binding.bannerInfo
        bannerMuteLayout = meetingActivity.binding.bannerMute
        bannerMuteIcon = meetingActivity.binding.bannerMuteIcon
        bannerMuteText = meetingActivity.binding.bannerMuteText
        meetingChrono = meetingActivity.binding.simpleChronometer

        meetingActivity.setSupportActionBar(toolbar)
        val actionBar = meetingActivity.supportActionBar ?: return

        val isGuest = arguments?.getBoolean(MEETING_IS_GUEST, false) ?: false

        if (!isGuest && args.action != MEETING_ACTION_GUEST) {
            actionBar.setHomeButtonEnabled(true)
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white)
        } else {
            actionBar.setHomeButtonEnabled(false)
            actionBar.setDisplayHomeAsUpEnabled(false)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_close_white)
        }

        setHasOptionsMenu(true)

        bannerAnotherCallLayout.setOnClickListener {
            sharedModel.clickSwitchCall()
        }
    }

    private fun showMeetingInfoFragment() {
        MeetingInfoBottomSheetDialogFragment.newInstance()
            .run {
                show(
                    this@InMeetingFragment.childFragmentManager,
                    tag
                )
            }
    }

    /**
     * Init View Models
     */
    private fun initViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                inMeetingViewModel.state.collect { (error, enabled) ->
                    if (error != null) {
                        sharedModel.showSnackBar(StringResourcesUtils.getString(error))
                        bottomFloatingPanelViewHolder.checkErrorAllowAddParticipants()
                    } else if (enabled != null) {
                        bottomFloatingPanelViewHolder.updateShareAndInviteButton()
                        bottomFloatingPanelViewHolder.updateAllowAddParticipantsSwitch(enabled)
                    }
                }
            }
        }

        sharedModel.currentChatId.observe(viewLifecycleOwner) {
            it?.let {
                Timber.d("Chat has changed")
                inMeetingViewModel.setChatId(it)
                if (!sharedModel.isChatOpen) {
                    chatManagement.openChatRoom(it)
                }
            }
        }

        sharedModel.meetingLinkLiveData.observe(viewLifecycleOwner) {
            if (!it.isNullOrEmpty()) {
                Timber.d("Link has changed")
                meetingLink = it
                inMeetingViewModel.getCall()?.let {
                    if (inMeetingViewModel.isWaitingForLink()) {
                        inMeetingViewModel.setWaitingForLink(false)
                        shareLink()
                    }
                }
            }
        }

        sharedModel.meetingNameLiveData.observe(viewLifecycleOwner) {
            if (!TextUtil.isTextEmpty(it)) {
                Timber.d("Meeting name has changed")
                inMeetingViewModel.updateMeetingName(it)
            }
        }

        inMeetingViewModel.callLiveData.observe(viewLifecycleOwner) {
            if (it != null && isWaitingForAnswerCall) {
                answerCallAfterJoin()
            }
        }

        inMeetingViewModel.chatTitle.observe(viewLifecycleOwner) {
            if (toolbarTitle != null) {
                Timber.d("Chat title has changed")
                toolbarTitle?.text = it
            }
        }

        sharedModel.micLiveData.observe(viewLifecycleOwner) {
            if (micIsEnable != it) {
                Timber.d("Mic status has changed to $it")
                micIsEnable = it
                updateLocalAudio(it)
            }
        }

        sharedModel.cameraLiveData.observe(viewLifecycleOwner) {
            if (camIsEnable != it) {
                Timber.d("Camera status has changed to $it")
                camIsEnable = it
                updateLocalVideo(it)
            }
        }

        sharedModel.speakerLiveData.observe(viewLifecycleOwner) {
            Timber.d("Speaker status has changed to $it")
            speakerIsEnable = it == AppRTCAudioManager.AudioDevice.SPEAKER_PHONE
            updateSpeaker(it)
        }

        sharedModel.snackBarLiveData.observe(viewLifecycleOwner) {
            if (it.isNotEmpty()) {
                showSnackbar(SNACKBAR_TYPE, it, MEGACHAT_INVALID_HANDLE)
            }
        }

        lifecycleScope.launchWhenStarted {
            sharedModel.cameraGranted.collect { allowed ->
                if (allowed) {
                    bottomFloatingPanelViewHolder.updateCamPermissionWaring(allowed)
                }
            }
        }

        sharedModel.cameraPermissionCheck.observe(viewLifecycleOwner) { allowed ->
            if (allowed) {
                permissionsRequester = permissionsBuilder(
                    arrayOf(Manifest.permission.CAMERA)
                )
                    .setOnRequiresPermission { l ->
                        run {
                            onRequiresPermission(l)

                            // Continue expected action after granted
                            sharedModel.clickCamera(true)
                        }
                    }
                    .setOnShowRationale { l ->
                        onShowRationale(l)

                    }
                    .setOnNeverAskAgain { l ->
                        onPermNeverAskAgain(l)
                    }
                    .build()
                permissionsRequester.launch(false)
            }
        }

        sharedModel.recordAudioPermissionCheck.observe(viewLifecycleOwner) { allowed ->
            sharedModel.lockMic()

            if (allowed) {
                permissionsRequester = permissionsBuilder(
                    arrayOf(Manifest.permission.RECORD_AUDIO)
                )
                    .setOnRequiresPermission { l ->
                        run {
                            onRequiresPermission(l)
                            // Continue expected action after granted
                            sharedModel.clickMic(true)
                        }
                    }
                    .setOnShowRationale { l ->
                        onShowRationale(l)
                    }
                    .setOnNeverAskAgain { l ->
                        onPermNeverAskAgain(l)
                    }
                    .build()
                permissionsRequester.launch(false)
            }
        }

        lifecycleScope.launchWhenStarted {
            sharedModel.recordAudioGranted.collect { allowed ->
                if (allowed && !sharedModel.micLocked) {
                    bottomFloatingPanelViewHolder.updateMicPermissionWaring(allowed)
                }
            }
        }

        viewLifecycleOwner.collectFlow(sharedModel.monitorConnectivityEvent) { haveConnection ->
            inMeetingViewModel.updateNetworkStatus(haveConnection)
        }

        lifecycleScope.launchWhenStarted {
            inMeetingViewModel.updateCallId.collect {
                if (it != MEGACHAT_INVALID_HANDLE) {
                    checkButtonsStatus()
                    showMuteBanner()
                    inMeetingViewModel.getCall()
                        ?.let { isCallOnHold(inMeetingViewModel.isCallOnHold()) }
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            inMeetingViewModel.showCallDuration.collect { isVisible ->
                if (isVisible) {
                    meetingChrono?.let { chrono ->
                        chrono.stop()
                        inMeetingViewModel.getCallDuration().let {
                            if (it != INVALID_VALUE.toLong()) {
                                chrono.base =
                                    SystemClock.elapsedRealtime() - it * MILLISECONDS_IN_ONE_SECOND
                                chrono.start()
                                chrono.format = " %s"
                                chrono.isVisible = true
                            }
                        }
                    }
                } else {
                    meetingChrono?.let {
                        it.stop()
                        it.isVisible = false
                    }
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            inMeetingViewModel.updateCallSubtitle.collect { type ->
                toolbarSubtitle?.let {
                    when (type) {
                        InMeetingViewModel.SubtitleCallType.TYPE_CONNECTING -> {
                            it.text = StringResourcesUtils.getString(R.string.chat_connecting)
                        }
                        InMeetingViewModel.SubtitleCallType.TYPE_CALLING -> {
                            it.text =
                                StringResourcesUtils.getString(R.string.outgoing_call_starting)
                        }
                        InMeetingViewModel.SubtitleCallType.TYPE_ESTABLISHED -> {
                            it.text = StringResourcesUtils.getString(R.string.duration_meeting)
                        }
                    }
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            inMeetingViewModel.allowClickingOnToolbar.collect { allowed ->
                if (allowed) {
                    meetingActivity.binding.toolbar.setOnClickListener {
                        showMeetingInfoFragment()
                    }
                } else {
                    meetingActivity.binding.toolbar.setOnClickListener(null)
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            inMeetingViewModel.anotherChatTitle.collect { title ->
                if (title.isNotEmpty()) {
                    bannerAnotherCallTitle?.text = title
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            inMeetingViewModel.getParticipantsChanges.collect { (type, title) ->
                if (title.trim().isNotEmpty()) {
                    participantsChangesBanner?.apply {
                        clearAnimation()
                        hideCallWillEndInBanner()

                        text = title
                        isVisible = true
                        alpha =
                            if (bottomFloatingPanelViewHolder.getState() == BottomSheetBehavior.STATE_EXPANDED) 0f
                            else 1f

                        animate()
                            .alpha(0f)
                            .setDuration(INFO_ANIMATION)
                            .withEndAction {
                                isVisible = false
                                if (type == TYPE_LEFT) {
                                    inMeetingViewModel.checkShowOnlyMeBanner()
                                }
                            }
                    }
                } else {
                    participantsChangesBanner?.apply {
                        clearAnimation()
                        isVisible = false
                    }
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            inMeetingViewModel.showOnlyMeBanner.collect { shouldBeShown ->
                checkMenuItemsVisibility()
                if (shouldBeShown && !MegaApplication.getChatManagement().hasEndCallDialogBeenIgnored) {
                    showCallWillEndBannerAndOnlyMeDialog()
                } else {
                    hideCallBannerAndOnlyMeDialog()
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            inMeetingViewModel.showWaitingForOthersBanner.collect { shouldBeShown ->
                checkMenuItemsVisibility()
                if (shouldBeShown) {
                    showWaitingForOthersBanner()
                } else {
                    hideCallWillEndInBanner()
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            inMeetingViewModel.updateAnotherCallBannerType.collect { type ->
                when (type) {
                    InMeetingViewModel.AnotherCallType.TYPE_NO_CALL -> {
                        Timber.d("No other calls in progress or on hold")
                        bannerAnotherCallLayout.isVisible = false
                        bottomFloatingPanelViewHolder.changeOnHoldIconDrawable(
                            false
                        )

                    }
                    InMeetingViewModel.AnotherCallType.TYPE_IN_PROGRESS -> {
                        bannerAnotherCallLayout.let {
                            it.alpha = 1f
                            it.isVisible = true
                        }
                        bannerAnotherCallSubtitle?.text =
                            StringResourcesUtils.getString(R.string.call_in_progress_layout)

                        bottomFloatingPanelViewHolder.changeOnHoldIconDrawable(
                            false
                        )

                    }
                    InMeetingViewModel.AnotherCallType.TYPE_ON_HOLD -> {
                        bannerAnotherCallLayout.let {
                            it.alpha = 0.9f
                            it.isVisible = true
                        }

                        bannerAnotherCallSubtitle?.text =
                            StringResourcesUtils.getString(R.string.call_on_hold)

                        bottomFloatingPanelViewHolder.changeOnHoldIconDrawable(
                            true
                        )
                    }
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            inMeetingViewModel.showPoorConnectionBanner.collect { shouldBeShown ->
                bannerInfo?.apply {
                    alpha = 1f
                    isVisible = shouldBeShown
                    if (shouldBeShown) {
                        setBackgroundColor(ContextCompat.getColor(
                            MegaApplication.getInstance().applicationContext,
                            R.color.dark_grey_alpha_070
                        ))
                        text =
                            StringResourcesUtils.getString(R.string.calls_call_screen_poor_network_quality)

                        reconnecting()
                    } else {
                        inMeetingViewModel.checkBannerInfo()
                    }
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            inMeetingViewModel.showReconnectingBanner.collect { shouldBeShown ->
                bannerInfo?.apply {
                    alpha = 1f
                    isVisible = shouldBeShown
                    if (shouldBeShown) {
                        setBackgroundColor(ContextCompat.getColor(
                            MegaApplication.getInstance().applicationContext,
                            android.R.color.transparent))
                        text =
                            StringResourcesUtils.getString(R.string.reconnecting_message)
                        startAnimation(blink)
                    } else {
                        clearAnimation()
                        inMeetingViewModel.checkBannerInfo()
                    }
                }
            }
        }

        inMeetingViewModel.showAssignModeratorBottomPanel.observe(viewLifecycleOwner) { shouldBeShown ->
            if (shouldBeShown) {
                assignModeratorDialog = AssignModeratorBottomSheetDialogFragment()
                assignModeratorDialog?.run {
                    setLeaveMeetingCallBack { inMeetingViewModel.hangCall() }
                    setAssignModeratorCallBack(showAssignModeratorFragment)
                    show(
                        this@InMeetingFragment.childFragmentManager,
                        tag
                    )
                }
            } else {
                assignModeratorDialog?.dismissAllowingStateLoss()
            }
        }

        inMeetingViewModel.showEndMeetingAsModeratorBottomPanel.observe(viewLifecycleOwner) { shouldBeShown ->
            if (shouldBeShown) {
                endMeetingAsModeratorDialog = EndMeetingAsModeratorBottomSheetDialogFragment()
                endMeetingAsModeratorDialog?.run {
                    setLeaveMeetingCallBack { inMeetingViewModel.checkClickLeaveButton() }
                    setEndForAllCallBack { inMeetingViewModel.endCallForAll() }
                    show(
                        this@InMeetingFragment.childFragmentManager,
                        tag
                    )
                }
            } else {
                endMeetingAsModeratorDialog?.dismissAllowingStateLoss()
            }
        }
    }

    /**
     * user denies the RECORD_AUDIO or CAMERA permission
     *
     * @param permissions permission list
     */
    private fun onPermNeverAskAgain(permissions: java.util.ArrayList<String>) {
        if (permissions.contains(Manifest.permission.RECORD_AUDIO)) {
            onAudioNeverAskAgain(permissions)
        } else if (permissions.contains(Manifest.permission.CAMERA)) {
            onCameraNeverAskAgain(permissions)
        }
    }

    /**
     * Control the initial state of the buttons when a call is established
     */
    private fun checkButtonsStatus() {
        inMeetingViewModel.getCall()?.let {
            if (it.hasLocalAudio()) {
                sharedModel.micInitiallyOn()
            }

            if (it.hasLocalVideo()) {
                sharedModel.camInitiallyOn()
            }
            enableOnHoldFab(it.isOnHold)
            updatePanel()
            return
        }
        enableOnHoldFab(false)
    }

    /**
     * User denies the RECORD_AUDIO permission
     *
     * @param permissions permission list
     */
    private fun onAudioNeverAskAgain(permissions: ArrayList<String>) {
        if (permissions.contains(Manifest.permission.RECORD_AUDIO)) {
            Timber.d("user denies the RECORD_AUDIO permissions")
            showRequestPermissionSnackBar()
        }
    }

    /**
     * User denies the CAMERA permission
     *
     * @param permissions permission list
     */
    private fun onCameraNeverAskAgain(permissions: ArrayList<String>) {
        if (permissions.contains(Manifest.permission.CAMERA)) {
            Timber.d("user denies the CAMERA permission")
            showRequestPermissionSnackBar()
        }
    }

    /**
     * Create `OnDragTouchListener` and set it as `OnTouchListener` for the target view to make it draggable.
     *
     * @param parent Parent view of the draggable view.
     */
    private fun initFloatingWindowContainerDragListener(parent: View) {
        dragTouchListener = OnDragTouchListener(
            floatingWindowContainer,
            parent,
            object :
                OnDragTouchListener.OnDragActionListener {

                override fun onDragStart(view: View?) {
                    if (toolbar.isVisible) {
                        val maxTop =
                            if (bannerMuteLayout.isVisible) bannerMuteLayout.bottom else toolbar.bottom
                        dragTouchListener.setToolbarHeight(maxTop)
                        dragTouchListener.setBottomSheetHeight(floatingBottomSheet.top)
                    } else {
                        dragTouchListener.setToolbarHeight(0)
                        dragTouchListener.setBottomSheetHeight(0)
                    }
                }

                override fun onDragEnd(view: View) {
                    // Record the last Y of the floating window after dragging ended.
                    previousY = view.y
                }
            }
        )

        floatingWindowContainer.setOnTouchListener(dragTouchListener)
    }

    private fun setPageOnClickListener(view: View) = view.setOnClickListener {
        onPageClick()
    }

    fun onPageClick() {
        // If the tips is showing or bottom is fully expanded, can not hide the toolbar and panel
        if (bottomFloatingPanelViewHolder.isPopWindowShowing()
            || bottomFloatingPanelViewHolder.getState() == BottomSheetBehavior.STATE_EXPANDED
        ) return

        // Prevent fast tapping.
        if (System.currentTimeMillis() - lastTouch < TAP_THRESHOLD) return

        toolbar.fadeInOut(dy = TOOLBAR_DY, toTop = true)

        if (bannerShouldBeShown) {
            bannerMuteLayout.fadeInOut(dy = FLOATING_BOTTOM_SHEET_DY, toTop = true)
        }

        floatingBottomSheet.fadeInOut(dy = FLOATING_BOTTOM_SHEET_DY, toTop = false)

        @Suppress("DEPRECATION")
        if (toolbar.isVisible) {
            meetingActivity.window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        } else {
            meetingActivity.window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }

        // Need not consider the snack bar
        adjustPositionOfFloatingWindow(bTop = true, bBottom = true)

        lastTouch = System.currentTimeMillis()
    }

    /**
     * Method to control the position of the floating window in relation to the configuration change
     *
     * @param outMetrics display metrics
     */
    private fun onConfigurationChangedOfFloatingWindow(outMetrics: DisplayMetrics) {
        floatingWindowContainer.post {
            previousY = -1f
            val dx = outMetrics.widthPixels - floatingWindowContainer.width
            var dy = outMetrics.heightPixels - floatingWindowContainer.height

            if (BottomSheetBehavior.STATE_COLLAPSED == bottomFloatingPanelViewHolder.getState() && floatingBottomSheet.isVisible) {
                dy = floatingBottomSheet.top - floatingWindowContainer.height
            }

            floatingWindowContainer.moveX(dx.toFloat())
            floatingWindowContainer.moveY(dy.toFloat())
        }
    }

    /**
     * Method to control the position of the floating window in relation to the toolbar, bottom sheet panel
     *
     * @param bTop Calculate the position related to top
     * @param bBottom Calculate the position related to bottom
     */
    private fun adjustPositionOfFloatingWindow(
        bTop: Boolean = false,
        bBottom: Boolean = true,
    ) {
        var isIntersect: Boolean
        var isIntersectPreviously: Boolean

        floatingWindowContainer.apply {
            // Control the position of the floating window in relation to the toolbar, including the banner
            if (bTop) {
                val maxTop =
                    if (bannerMuteLayout.isVisible) bannerMuteLayout.bottom else toolbar.bottom

                isIntersect = (maxTop - this.y) > 0
                if (toolbar.isVisible && isIntersect) {
                    this.moveY(maxTop.toFloat())
                }

                isIntersectPreviously = (maxTop - previousY) > 0
                if (!toolbar.isVisible && isIntersectPreviously && previousY >= 0) {
                    this.moveY(previousY)
                }
            }
            if (bBottom) {
                // When the bottom panel is expanded, keep the current position
                if (bottomFloatingPanelViewHolder.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                    return
                }

                val bottom = this.y + this.height
                val top = floatingBottomSheet.top
                val margin1 = bottom - top

                isIntersect = margin1 > 0
                if (floatingBottomSheet.isVisible && isIntersect) {
                    this.moveY(this.y - margin1)
                }

                val margin2 = previousY + this.height - floatingBottomSheet.top
                isIntersectPreviously = margin2 > 0
                if (!floatingBottomSheet.isVisible && isIntersectPreviously && previousY >= 0) {
                    this.moveY(previousY)
                }
            }
        }
    }

    /**
     * Show the correct UI in a one-to-one call
     */
    private fun updateOneToOneUI() {
        inMeetingViewModel.getCall()?.let {
            if (inMeetingViewModel.amIAloneOnTheCall(it.chatid)) {
                Timber.d("One to one call. Waiting for connection")
                waitingForConnection(it.chatid)
            } else {
                val session = inMeetingViewModel.getSessionOneToOneCall(it)
                session?.let {
                    Timber.d("One to one call. Session exists")
                    initOneToOneCall()
                }
            }
        }
    }

    /**
     * Show the correct UI in a meeting
     */
    private fun updateGroupUI() {
        inMeetingViewModel.getCall()?.let { call ->
            initGroupCall(call.chatid)
        }
    }

    /**
     * Method to remove all video listeners and all child fragments
     */
    private fun removeListenersAndFragments() {
        Timber.d("Remove listeners and fragments")
        removeAllListeners()
        removeAllFragments()
    }

    /**
     * Method to remove all video listeners
     */
    private fun removeAllListeners() {
        Timber.d("Remove all listeners")
        individualCallFragment?.let {
            if (it.isAdded) {
                it.removeChatVideoListener()
            }
        }

        floatingWindowFragment?.let {
            if (it.isAdded) {
                it.removeChatVideoListener()
            }
        }

        speakerViewCallFragment?.let {
            if (it.isAdded) {
                it.removeTextureView()
            }
        }

        gridViewCallFragment?.let {
            if (it.isAdded) {
                it.removeTextureView()
            }
        }

        inMeetingViewModel.removeListeners()
    }

    /**
     * Method to remove all child fragments
     */
    private fun removeAllFragments() {
        inMeetingViewModel.removeAllParticipantVisible()
        Timber.d("Remove all fragments")
        floatingWindowFragment?.let {
            if (it.isAdded) {
                removeChildFragment(it)
                floatingWindowFragment = null
            }
        }

        individualCallFragment?.let {
            if (it.isAdded) {
                removeChildFragment(it)
                individualCallFragment = null
            }
        }

        gridViewCallFragment?.let {
            if (it.isAdded) {
                removeChildFragment(it)
                gridViewCallFragment = null
            }
        }

        speakerViewCallFragment?.let {
            if (it.isAdded) {
                removeChildFragment(it)
                speakerViewCallFragment = null
            }
        }
    }

    /**
     * Show reconnecting UI
     */
    private fun reconnecting() {
        Timber.d("Show reconnecting UI, the current status is ${inMeetingViewModel.status}")
        if (inMeetingViewModel.status == NOT_TYPE)
            return

        inMeetingViewModel.status = NOT_TYPE

        removeListenersAndFragments()
        binding.reconnecting.isVisible = true
    }

    /**
     * Remove fragments
     */
    fun removeUI() {
        Timber.d("Removing call UI, the current status is ${inMeetingViewModel.status}")
        if (inMeetingViewModel.status == NOT_TYPE)
            return

        inMeetingViewModel.status = NOT_TYPE

        removeListenersAndFragments()
    }

    private fun checkMenuItemsVisibility() {
        checkGridSpeakerViewMenuItemVisibility()
        checkSwapCameraMenuItemVisibility()
    }

    /**
     * Control the UI of the call, whether one-to-one or meeting
     */
    private fun checkChildFragments() {
        val call: MegaChatCall = inMeetingViewModel.getCall() ?: return

        Timber.d("Check child fragments")
        binding.reconnecting.isVisible = false

        if (call.status >= MegaChatCall.CALL_STATUS_JOINING) {
            if (inMeetingViewModel.isOneToOneCall()) {
                Timber.d("One to one call")
                updateOneToOneUI()
            } else {
                Timber.d("Group call")
                updateGroupUI()
            }
        } else {
            Timber.d("Waiting For Connection")
            waitingForConnection(call.chatid)
        }
    }

    /**
     * Show one to one call UI
     */
    private fun initOneToOneCall() {
        if (inMeetingViewModel.status == TYPE_IN_ONE_TO_ONE) return

        removeListenersAndFragments()

        val call: MegaChatCall? = inMeetingViewModel.getCall()
        call?.let { currentCall ->
            val session = inMeetingViewModel.getSessionOneToOneCall(currentCall)
            session?.let { userSession ->
                Timber.d("Show one to one call UI")
                inMeetingViewModel.status = TYPE_IN_ONE_TO_ONE

                Timber.d("Create fragment")
                individualCallFragment = IndividualCallFragment.newInstance(
                    currentCall.chatid,
                    userSession.peerid,
                    userSession.clientid
                )

                individualCallFragment?.let { it ->
                    loadChildFragment(
                        R.id.meeting_container,
                        it,
                        IndividualCallFragment.TAG
                    )
                }
            }

            initLocal(currentCall.chatid)
        }
    }

    /**
     * Method to display the waiting for connection UI
     *
     * @param chatId ID of chat
     */
    private fun waitingForConnection(chatId: Long) {
        if (inMeetingViewModel.status == TYPE_WAITING_CONNECTION) return

        Timber.d("Show waiting for connection call UI")
        inMeetingViewModel.status = TYPE_WAITING_CONNECTION

        removeListenersAndFragments()

        individualCallFragment = IndividualCallFragment.newInstance(
            chatId,
            megaApi.myUserHandleBinary,
            false
        )

        individualCallFragment?.let {
            loadChildFragment(
                R.id.meeting_container,
                it,
                IndividualCallFragment.TAG
            )
        }

        checkGridSpeakerViewMenuItemVisibility()
    }

    /**
     * Show local fragment UI
     *
     * @param chatId the chat ID
     */
    private fun initLocal(chatId: Long) {
        Timber.d("Init local fragment")
        floatingWindowFragment?.let {
            if (it.isAdded) {
                it.removeChatVideoListener()
                removeChildFragment(it)
                floatingWindowFragment = null
            }
        }

        floatingWindowFragment = IndividualCallFragment.newInstance(
            chatId,
            megaApi.myUserHandleBinary,
            true
        )

        floatingWindowFragment?.let {
            loadChildFragment(
                R.id.self_feed_floating_window_container,
                it,
                IndividualCallFragment.TAG
            )
        }

        // Calculate the position of floating window if it is shown after the snack bar.
        meetingActivity.snackbar?.let {
            floatingWindowContainer.post {
                adjustPositionOfFloatingWindow(
                    bTop = false,
                    bBottom = true
                )
            }
        }
    }

    /**
     * Method to display the speaker view UI
     */
    private fun initSpeakerViewMode() {
        if (inMeetingViewModel.status == TYPE_IN_SPEAKER_VIEW) return

        Timber.d("Show group call - Speaker View UI")
        inMeetingViewModel.status = TYPE_IN_SPEAKER_VIEW
        inMeetingViewModel.removeAllParticipantVisible()

        gridViewCallFragment?.let {
            if (it.isAdded) {
                it.removeTextureView()
                removeChildFragment(it)
                gridViewCallFragment = null
            }
        }

        speakerViewCallFragment?.let {
            if (it.isAdded) {
                it.removeTextureView()
                removeChildFragment(it)
                speakerViewCallFragment = null
            }
        }

        speakerViewCallFragment = SpeakerViewCallFragment.newInstance()
        speakerViewCallFragment?.let {
            loadChildFragment(
                R.id.meeting_container,
                it,
                SpeakerViewCallFragment.TAG
            )
        }

        inMeetingViewModel.updateParticipantResolution()
        checkGridSpeakerViewMenuItemVisibility()
    }

    /**
     * Method to display the grid view UI
     */
    private fun initGridViewMode() {
        if (inMeetingViewModel.status == TYPE_IN_GRID_VIEW) return

        Timber.d("Show group call - Grid View UI")
        inMeetingViewModel.status = TYPE_IN_GRID_VIEW

        inMeetingViewModel.removeAllParticipantVisible()

        speakerViewCallFragment?.let {
            if (it.isAdded) {
                it.removeTextureView()
                removeChildFragment(it)
                speakerViewCallFragment = null
            }
        }

        gridViewCallFragment?.let {
            if (it.isAdded) {
                it.removeTextureView()
                removeChildFragment(it)
                gridViewCallFragment = null
            }
        }

        gridViewCallFragment = GridViewCallFragment.newInstance()
        gridViewCallFragment?.let {
            loadChildFragment(
                R.id.meeting_container,
                it,
                GridViewCallFragment.TAG
            )
        }

        inMeetingViewModel.updateParticipantResolution()
        checkGridSpeakerViewMenuItemVisibility()
    }

    /**
     * Method to control which group UI to display
     *
     * @param chatId the chat ID
     */
    private fun initGroupCall(chatId: Long) {
        if (inMeetingViewModel.status != TYPE_IN_GRID_VIEW && inMeetingViewModel.status != TYPE_IN_SPEAKER_VIEW) {
            individualCallFragment?.let {
                if (it.isAdded) {
                    it.removeChatVideoListener()
                    removeChildFragment(it)
                    individualCallFragment = null
                }
            }
            initLocal(chatId)
        }

        when {
            !isManualModeView -> {
                inMeetingViewModel.getCall()?.let {
                    if (it.sessionsClientid.size() <= MAX_PARTICIPANTS_GRID_VIEW_AUTOMATIC) {
                        Timber.d("Automatic mode - Grid view")
                        initGridViewMode()
                    } else {
                        Timber.d("Automatic mode - Speaker view")
                        initSpeakerViewMode()
                    }
                }
            }
            inMeetingViewModel.status == TYPE_IN_SPEAKER_VIEW -> {
                Timber.d("Manual mode - Speaker view")
                initSpeakerViewMode()
            }
            else -> {
                Timber.d("Manual mode - Grid view")
                initGridViewMode()
            }
        }
    }

    /**
     * Method controlling whether to initiate a call
     */
    private fun initStartMeeting() {
        if (sharedModel.currentChatId.value == MEGACHAT_INVALID_HANDLE) {
            val nameChosen: String? = sharedModel.getMeetingName()
            nameChosen?.let {
                when {
                    !TextUtil.isTextEmpty(it) -> {
                        inMeetingViewModel.setTitleChat(it)
                    }
                }
            }

            Timber.d("Starting meeting ...")
            startCall()
        }
    }

    /**
     * Method for loading fragments
     *
     * @param containerId The fragment ID
     * @param fragment The fragment to load
     * @param tag The specific tag to differentiate the fragment
     */
    private fun loadChildFragment(containerId: Int, fragment: Fragment, tag: String) {
        if (requireActivity().isFinishing) return

        childFragmentManager.beginTransaction().replace(
            containerId,
            fragment,
            tag
        ).commit()
    }

    private fun removeChildFragment(fragment: Fragment?) {
        fragment?.let {
            childFragmentManager.beginTransaction().remove(it).commit()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.in_meeting_fragment_menu, menu)
        speakerViewMenuItem = menu.findItem(R.id.speaker_view)
        gridViewMenuItem = menu.findItem(R.id.grid_view)
        swapCameraMenuItem = menu.findItem(R.id.swap_camera)

        checkMenuItemsVisibility()
    }

    /**
     * Method to show or hide the button of swap camera
     */
    private fun checkSwapCameraMenuItemVisibility() {
        swapCameraMenuItem?.let {
            it.isVisible = inMeetingViewModel.isNecessaryToShowSwapCameraOption()
        }
    }

    /**
     * Method to show or hide the buttons change to grid/speaker view
     */
    private fun checkGridSpeakerViewMenuItemVisibility() {
        inMeetingViewModel.getCall()?.let { call ->
            if (call.status == MegaChatCall.CALL_STATUS_CONNECTING || inMeetingViewModel.showOnlyMeBanner.value || inMeetingViewModel.showWaitingForOthersBanner.value) {
                gridViewMenuItem?.apply {
                    isVisible = false
                }
                speakerViewMenuItem?.apply {
                    isVisible = false
                }
                return
            }
        }

        when (inMeetingViewModel.status) {
            TYPE_IN_GRID_VIEW -> {
                gridViewMenuItem?.let {
                    it.isVisible = false
                }
                speakerViewMenuItem?.let {
                    it.isVisible = true
                }
            }
            TYPE_IN_SPEAKER_VIEW -> {
                gridViewMenuItem?.let {
                    it.isVisible = true
                }
                speakerViewMenuItem?.let {
                    it.isVisible = false
                }
            }
            else -> {
                gridViewMenuItem?.let {
                    it.isVisible = false
                }
                speakerViewMenuItem?.let {
                    it.isVisible = false
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.swap_camera -> {
                Timber.d("Swap camera.")
                VideoCaptureUtils.swapCamera(ChatChangeVideoStreamListener(getContext()))
                true
            }
            R.id.grid_view -> {
                Timber.d("Change to grid view.")
                isManualModeView = true
                initGridViewMode()
                true
            }
            R.id.speaker_view -> {
                Timber.d("Change to speaker view.")
                isManualModeView = true
                initSpeakerViewMode()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun showSnackbar(type: Int, content: String?, chatId: Long) {
        val anchor =
            if (BottomSheetBehavior.STATE_COLLAPSED == bottomFloatingPanelViewHolder.getState() && floatingBottomSheet.isVisible) {
                binding.snackbarPosition
            } else null

        meetingActivity.showSnackbarWithAnchorView(
            type,
            binding.root,
            anchor,
            content,
            chatId
        )
    }

    private fun showRequestPermissionSnackBar() {
        val warningText =
            StringResourcesUtils.getString(R.string.meeting_required_permissions_warning)
        showSnackbar(PERMISSIONS_TYPE, warningText, MEGACHAT_INVALID_HANDLE)
    }

    /**
     * Init Floating Panel
     */
    private fun initFloatingPanel() {
        bottomFloatingPanelViewHolder =
            BottomFloatingPanelViewHolder(
                inMeetingViewModel,
                binding,
                this,
                resources.displayMetrics
            )

        //Observer the participant List
        inMeetingViewModel.participants.observe(viewLifecycleOwner) { participants ->
            participants?.let {
                bottomFloatingPanelViewHolder
                    .setParticipantsPanel(
                        it.toMutableList(),
                        inMeetingViewModel.getMyOwnInfo(
                            sharedModel.micLiveData.value ?: false,
                            sharedModel.cameraLiveData.value ?: false
                        )
                    )
            }
        }

        bottomFloatingPanelViewHolder.propertyUpdaters.add {
            toolbar.alpha = 1 - it
            // When the bottom on the top, will set the toolbar invisible, otherwise will cover the scroll event of the panel
            toolbar.isVisible = it != 1.0f

            bannerMuteLayout.apply {
                if (isVisible) {
                    alpha = 1 - it
                }
            }

            callBanner?.apply {
                if (isVisible) {
                    alpha = 1 - it
                }
            }

            bannerInfo?.apply {
                if (isVisible) {
                    alpha = 1 - it
                }
            }

            bannerAnotherCallLayout.apply {
                if (isVisible) {
                    alpha = 1 - it
                }
            }
        }
    }

    /**
     * Change Mic State
     *
     * @param micOn True, if the microphone is on. False, if the microphone is off
     */
    override fun onChangeMicState(micOn: Boolean) {
        Timber.d("Change in mic state")
        sharedModel.clickMic(!micOn)
    }

    /**
     * Method for controlling the microphone status update
     *
     * @param isMicOn True, if the microphone is on. False, if the microphone is off
     */
    private fun updateLocalAudio(isMicOn: Boolean) {
        bottomFloatingPanelViewHolder.updateMicIcon(isMicOn)
        updateParticipantsBottomPanel()
        showMuteBanner()
    }

    /**
     * Method that controls whether the banner should be displayed.
     * This banner is displayed when the call is muted
     * or if the session or call is on hold.
     */
    private fun showMuteBanner() {
        bannerMuteLayout.let {
            bannerShouldBeShown = inMeetingViewModel.showAppropriateBanner(
                bannerMuteIcon,
                bannerMuteText
            )

            if (bannerShouldBeShown) {
                it.setBackgroundColor(ContextCompat.getColor(requireContext(),
                    R.color.dark_grey_alpha_070))
                Timber.d("Show banner info")
                it.isVisible = true
            } else {
                Timber.d("Hide banner info")
                it.isVisible = false
            }

            if (it.isVisible && (bottomFloatingPanelViewHolder.getState() == BottomSheetBehavior.STATE_EXPANDED || !toolbar.isVisible)) {
                it.alpha = 0f
            }

            adjustPositionOfFloatingWindow(
                bTop = true,
                bBottom = false
            )
        }
    }

    /**
     * Check if a call is outgoing, on hold button must be disabled
     */
    private fun enableOnHoldFab(callIsOnHold: Boolean) {
        bottomFloatingPanelViewHolder.enableHoldIcon(
            !inMeetingViewModel.isRequestSent(),
            callIsOnHold
        )
    }

    /**
     * Method that controls whether the call has been put on or taken off hold, and updates the UI.
     */
    private fun isCallOnHold(isHold: Boolean) {
        Timber.d("Changes in the on hold status of the call")
        bottomFloatingPanelViewHolder.let {
            bottomFloatingPanelViewHolder.disableEnableButtons(
                inMeetingViewModel.isCallEstablished(),
                isHold
            )
        }

        showMuteBanner()
        if (!inMeetingViewModel.isOneToOneCall()) {
            gridViewCallFragment?.let {
                if (it.isAdded) {
                    it.updateCallOnHold(isHold)
                }
            }

            speakerViewCallFragment?.let {
                if (it.isAdded) {
                    it.updateCallOnHold(isHold)
                }
            }
        }
    }

    /**
     * Change Cam State
     *
     * @param camOn True, if the camera is switched on. False, if the camera is switched off
     */
    override fun onChangeCamState(camOn: Boolean) {
        Timber.d("Change in camera state")
        sharedModel.clickCamera(!camOn)
    }

    /**
     * Method to disable the local camera
     */
    private fun disableCamera() {
        if (camIsEnable) {
            sharedModel.clickCamera(false)
            camIsEnable = false
        }
    }

    /**
     * Method that checks if the local video has changed in one to one call and updates the UI.
     *
     * @param isCamOn True, if the camera is switched on. False, if the camera is switched off
     */
    private fun controlVideoLocalOneToOneCall(isCamOn: Boolean) {
        val call = inMeetingViewModel.getCall()

        val shouldCheckVideoOn =
            call != null && (call.status == MegaChatCall.CALL_STATUS_IN_PROGRESS ||
                    call.status == MegaChatCall.CALL_STATUS_JOINING) && isCamOn

        individualCallFragment?.let {
            if (it.isAdded) {
                if (shouldCheckVideoOn) {
                    it.checkVideoOn(megaApi.myUserHandleBinary, MEGACHAT_INVALID_HANDLE)
                } else {
                    it.videoOffUI(megaApi.myUserHandleBinary, MEGACHAT_INVALID_HANDLE)
                }
            }
        }

        floatingWindowFragment?.let {
            if (it.isAdded) {
                if (shouldCheckVideoOn) {
                    it.checkVideoOn(megaApi.myUserHandleBinary, MEGACHAT_INVALID_HANDLE)
                } else {
                    it.videoOffUI(megaApi.myUserHandleBinary, MEGACHAT_INVALID_HANDLE)
                }
            }
        }
    }

    /**
     * Method that checks if the local video has changed and updates the UI.
     *
     * @param isCamOn True, if the camera is switched on. False, if the camera is switched off
     */
    private fun updateLocalVideo(isCamOn: Boolean) {
        Timber.d("Local audio or video changes")
        inMeetingViewModel.getCall()?.let {
            val isVideoOn: Boolean = it.hasLocalVideo()
            if (!MegaApplication.getChatManagement().isInTemporaryState) {
                MegaApplication.getChatManagement().setVideoStatus(it.chatid, isVideoOn)
            }
        }

        bottomFloatingPanelViewHolder.updateCamIcon(isCamOn)
        updateParticipantsBottomPanel()
        checkSwapCameraMenuItemVisibility()
        controlVideoLocalOneToOneCall(isCamOn)
    }

    /*
     * Method for updating the list of participants in the bottom panel
     */
    fun updateParticipantsBottomPanel() {
        inMeetingViewModel.participants.value?.let { participants ->
            bottomFloatingPanelViewHolder
                .updateParticipants(
                    participants.toMutableList(),
                    inMeetingViewModel.getMyOwnInfo(
                        sharedModel.micLiveData.value ?: false,
                        sharedModel.cameraLiveData.value ?: false
                    )
                )
        }
    }

    /**
     * Method that checks if the session's on hold state has changed and updates the UI
     *
     * @param session The session of a participant
     */
    private fun updateOnHoldRemote(session: MegaChatSession) {
        Timber.d("Changes to the on hold status of the session")
        gridViewCallFragment?.let {
            if (it.isAdded) {
                it.updateSessionOnHold(session)
            }
        }

        speakerViewCallFragment?.let {
            if (it.isAdded) {
                it.updateSessionOnHold(session)
            }
        }
    }

    /**
     * Method that controls when the resolution of a participant's video has changed and should add or remove the listener
     *
     * @param participant The participant whose listener of the video is to be added or deleted
     * @param shouldAddListener True, should add the listener. False, should remove the listener
     * @param isHiRes True, if is High resolution. False, if is Low resolution
     */
    private fun checkVideoListener(
        participant: Participant,
        shouldAddListener: Boolean,
        isHiRes: Boolean,
    ) {
        gridViewCallFragment?.let {
            if (it.isAdded) {
                it.updateListener(participant, shouldAddListener, isHiRes)
            }
        }
        speakerViewCallFragment?.let {
            if (it.isAdded) {
                it.updateListener(participant, shouldAddListener, isHiRes)
            }
        }
    }

    /**
     * Method that controls when the resolution of a speaker's video has changed and should add or remove the listener
     *
     * @param peerId Peer ID of participant whose listener of the video is to be added or deleted
     * @param clientId Client ID of participant whose listener of the video is to be added or deleted
     * @param shouldAddListener True, should add the listener. False, should remove the listener
     */
    private fun checkSpeakerVideoListener(
        peerId: Long, clientId: Long,
        shouldAddListener: Boolean,
    ) {
        speakerViewCallFragment?.let {
            if (it.isAdded) {
                it.updateListenerSpeaker(peerId, clientId, shouldAddListener)
            }
        }
    }

    /**
     * Method that checks if the remote video/audio has changed and updates the UI.
     */
    private fun updateRemoteAVFlags(
        session: MegaChatSession,
        isAudioChange: Boolean,
        isVideoChange: Boolean,
    ) {
        Timber.d("Remote changes detected")
        gridViewCallFragment?.let {
            if (it.isAdded) {
                if (isAudioChange) {
                    it.updateRemoteAudioVideo(TYPE_AUDIO, session)
                }
                if (isVideoChange) {
                    Timber.d("Remote AVFlag")
                    it.updateRemoteAudioVideo(TYPE_VIDEO, session)
                }
            }
        }

        speakerViewCallFragment?.let {
            if (it.isAdded) {
                if (isAudioChange) {
                    it.updateRemoteAudioVideo(TYPE_AUDIO, session)
                }
                if (isVideoChange) {
                    it.updateRemoteAudioVideo(TYPE_VIDEO, session)
                }
            }
        }
    }

    /**
     * Method that controls changes when a participant joins or leaves the call
     *
     * @param isAdded True, if added. False, if gone
     * @param position The position that has changed
     */
    private fun participantAddedOfLeftMeeting(isAdded: Boolean, position: Int) {
        Timber.d("Participant was added or left the meeting in $position")
        speakerViewCallFragment?.let {
            if (it.isAdded) {
                it.peerAddedOrRemoved(isAdded, position)
            }
        }

        gridViewCallFragment?.let {
            if (it.isAdded) {
                it.peerAddedOrRemoved(isAdded, position)
            }
        }

        inMeetingViewModel.updateParticipantResolution()
    }

    /**
     * Method that checks if a participant's name or avatar has changed and updates the UI
     *
     * @param peerId user handle that has changed
     * @param type the type of change, name or avatar
     */
    private fun updateParticipantInfo(peerId: Long, type: Int) {
        Timber.d("Participant's name has changed")
        val listParticipants = inMeetingViewModel.updateParticipantsNameOrAvatar(peerId, type)
        if (listParticipants.isNotEmpty()) {
            gridViewCallFragment?.let {
                if (it.isAdded) {
                    it.updateNameOrAvatar(listParticipants, type)
                }
            }
            speakerViewCallFragment?.let {
                if (it.isAdded) {
                    it.updateNameOrAvatar(listParticipants, type)
                }
            }
        }

        if (type == AVATAR_CHANGE) {
            individualCallFragment?.let {
                if (it.isAdded && inMeetingViewModel.isMe(peerId)) {
                    it.updateMyAvatar()
                }
            }

            floatingWindowFragment?.let {
                if (it.isAdded && inMeetingViewModel.isMe(peerId)) {
                    it.updateMyAvatar()
                }
            }
        }
    }

    /**
     * Method that checks if several participants privileges has changed and updates the UI
     *
     * @param listParticipants list of participants with changes
     */
    private fun updateRemotePrivileges(listParticipants: MutableSet<Participant>) {
        if (listParticipants.isNotEmpty()) {
            Timber.d("Update remote privileges")
            gridViewCallFragment?.let {
                if (it.isAdded) {
                    it.updatePrivileges(listParticipants)
                }
            }

            speakerViewCallFragment?.let {
                if (it.isAdded) {
                    it.updatePrivileges(listParticipants)
                }
            }
        }
    }

    /**
     * Change Hold State
     *
     * @param isHold True, if should be on hold. False, otherwise.
     */
    override fun onChangeHoldState(isHold: Boolean) {
        val anotherCall = inMeetingViewModel.getAnotherCall()
        when {
            anotherCall == null -> {
                Timber.d("No other calls in progress")
                inMeetingViewModel.setCallOnHold(isHold)
            }
            anotherCall.isOnHold -> {
                Timber.d("Change of status on hold and switch of call")
                inMeetingViewModel.setCallOnHold(true)
                inMeetingViewModel.setAnotherCallOnHold(anotherCall.chatid, false)
                sharedModel.clickSwitchCall()
            }
            inMeetingViewModel.isCallOnHold() -> {
                Timber.d("Change of status on hold")
                inMeetingViewModel.setCallOnHold(false)
                inMeetingViewModel.setAnotherCallOnHold(anotherCall.chatid, true)
            }
            else -> {
                Timber.d("The current call is not on hold, change the status")
                inMeetingViewModel.setCallOnHold(isHold)
            }
        }
    }

    /**
     * Change Speaker state
     */
    override fun onChangeSpeakerState() {
        Timber.d("Change in speaker state")
        sharedModel.clickSpeaker()
    }

    /**
     * Method that updates UI when speaker changes
     *
     * @param device The current device selected
     */
    private fun updateSpeaker(device: AppRTCAudioManager.AudioDevice) {
        bottomFloatingPanelViewHolder.updateSpeakerIcon(device)
    }

    /**
     * Pop up dialog for end meeting for the user/guest
     *
     * Will show bottom sheet fragment for the moderator
     */
    override fun onEndMeeting() {
        inMeetingViewModel.checkClickEndButton()
    }

    private fun collapsePanel() {
        val isPanelExpanded =
            bottomFloatingPanelViewHolder.getState() == BottomSheetBehavior.STATE_EXPANDED
        isWaitingForMakeModerator = isPanelExpanded

        if (isPanelExpanded)
            bottomFloatingPanelViewHolder.collapse()
    }

    /**
     * Method to navigate to the Make moderator screen
     */
    private val showAssignModeratorFragment = fun() {
        collapsePanel()
        inMeetingViewModel.hideBottomPanels()
        findNavController().navigate(
            InMeetingFragmentDirections.actionGlobalMakeModerator()
        )

    }

    /**
     * Method to control when call ended
     */
    private fun finishActivity() {
        disableCamera()
        removeUI()
        meetingActivity.finish()
    }

    /**
     * Send share link
     *
     * @param sendLink The link of the meeting
     */
    override fun onShareLink(sendLink: Boolean) {
        if (inMeetingViewModel.isOneToOneCall() || !inMeetingViewModel.isChatRoomPublic() || inMeetingViewModel.isWaitingForLink()) {
            Timber.e("Error getting the link, it is a private chat")
            return
        }

        if (meetingLink.isEmpty()) {
            inMeetingViewModel.setWaitingForLink(sendLink)
            sharedModel.createChatLink(
                inMeetingViewModel.getChatId(),
                inMeetingViewModel.isModerator()
            )
            Timber.e("Error, the link doesn't exist")
            return
        }

        if (sendLink)
            shareLink()
    }

    /**
     * Method for sharing the meeting link
     */
    fun shareLink() {
        Timber.d("Share the link")
        meetingActivity.startActivity(Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, meetingLink)
            type = "text/plain"
        })
    }

    /**
     * Open invite participant page
     */
    @Suppress("deprecation") // TODO Migrate to registerForActivityResult()
    override fun onInviteParticipants() {
        Timber.d("chooseAddContactDialog")
        val contacts = megaApi.contacts
        if (contacts.isNullOrEmpty() || !contacts.any { it.visibility == VISIBILITY_VISIBLE }) {
            val dialog = AddParticipantsNoContactsDialogFragment.newInstance()
            dialog.show(childFragmentManager, dialog.tag)
        } else if (ChatUtil.areAllMyContactsChatParticipants(inMeetingViewModel.getChat())) {
            val dialog = AddParticipantsNoContactsLeftToAddDialogFragment.newInstance()
            dialog.show(childFragmentManager, dialog.tag)
        } else {
            val inviteParticipantIntent =
                Intent(meetingActivity, AddContactActivity::class.java).apply {
                    putExtra(INTENT_EXTRA_KEY_CONTACT_TYPE, CONTACT_TYPE_MEGA)
                    putExtra(INTENT_EXTRA_KEY_CHAT, true)
                    putExtra(INTENT_EXTRA_IS_FROM_MEETING, true)
                    putExtra(INTENT_EXTRA_KEY_CHAT_ID, inMeetingViewModel.currentChatId)
                    putExtra(
                        INTENT_EXTRA_KEY_TOOL_BAR_TITLE,
                        StringResourcesUtils.getString(R.string.invite_participants)
                    )
                }
            meetingActivity.startActivityForResult(
                inviteParticipantIntent, REQUEST_ADD_PARTICIPANTS
            )
        }
    }

    override fun onAllowAddParticipants() {
        inMeetingViewModel.onAllowAddParticipantsTap()
    }

    /**
     * Show participant bottom sheet when user click the three dots on participant item
     *
     * @param participant Participant of the meeting
     */
    override fun onParticipantOption(participant: Participant) {
        val participantBottomSheet =
            MeetingParticipantBottomSheetDialogFragment.newInstance(
                inMeetingViewModel.amIAGuest(),
                inMeetingViewModel.isModerator(),
                inMeetingViewModel.status == TYPE_IN_SPEAKER_VIEW,
                participant
            )
        participantBottomSheet.show(childFragmentManager, participantBottomSheet.tag)
    }

    companion object {
        const val ANIMATION_DURATION: Long = 500

        const val TAP_THRESHOLD: Long = 500

        const val TOOLBAR_DY = 300f

        const val FLOATING_BOTTOM_SHEET_DY = 400f

        const val NOT_TYPE = "NOT_TYPE"
        const val TYPE_WAITING_CONNECTION = "TYPE_WAITING_CONNECTION"
        const val TYPE_IN_ONE_TO_ONE = "TYPE_IN_ONE_TO_ONE"
        const val TYPE_IN_GRID_VIEW = "TYPE_IN_GRID_VIEW"
        const val TYPE_IN_SPEAKER_VIEW = "TYPE_IN_SPEAKER_VIEW"

        const val MAX_PARTICIPANTS_GRID_VIEW_AUTOMATIC = 6

        const val MILLISECONDS_IN_ONE_SECOND:Long = 1000

        const val INFO_ANIMATION = MILLISECONDS_IN_ONE_SECOND
    }

    /**
     * Method to control when a call is to be initiated
     *
     * @param chatId The chat ID of the call
     */
    private fun checkCallStarted(chatId: Long) {
        MegaApplication.getInstance().openCallService(chatId)
        inMeetingViewModel.setCall(chatId)
        checkChildFragments()
        showMuteBanner()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        disableCamera()
    }

    override fun onDestroy() {
        super.onDestroy()

        sharedModel.hideSnackBar()
        countDownTimerToEndCall?.cancel()

        removeUI()
        Timber.d("Fragment destroyed")

        resumeAudioPlayerIfNotInCall(meetingActivity)
        RunOnUIThreadUtils.stop()
        bottomFloatingPanelViewHolder.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        MegaApplication.getInstance().startProximitySensor()
        checkChildFragments()
        inMeetingViewModel.checkParticipantsList()
    }

    override fun onPause() {
        super.onPause()
        rtcAudioManagerGateway.unregisterProximitySensor()
    }

    override fun onStop() {
        super.onStop()
        dismissDialog(leaveDialog)
        dismissDialog(failedDialog)
        dismissDialog(onlyMeDialog)
        assignModeratorDialog?.dismissAllowingStateLoss()
        endMeetingAsModeratorDialog?.dismissAllowingStateLoss()
    }

    /**
     * Method that updates the microphone and camera values
     */
    private fun updateMicAndCam() {
        camIsEnable = sharedModel.cameraLiveData.value!!
        micIsEnable = sharedModel.micLiveData.value!!
        bottomFloatingPanelViewHolder.updateCamIcon(camIsEnable)
        bottomFloatingPanelViewHolder.updateMicIcon(micIsEnable)
        updateParticipantsBottomPanel()
    }

    /**
     * Method to control what to do when I have joined the chatroom
     *
     * @param chatId The chat ID
     */
    private fun controlWhenJoinedAChat(chatId: Long) {
        if (chatId != MEGACHAT_INVALID_HANDLE) {
            Timber.d("Update chat id $chatId")
            sharedModel.updateChatRoomId(chatId)
        }

        inMeetingViewModel.checkAnotherCallsInProgress(chatId)
        if (args.action != MEETING_ACTION_GUEST || CallUtil.isStatusConnected(
                context,
                args.chatId
            )
        ) {
            answerCallAfterJoin()
        } else {
            inMeetingViewModel.registerConnectionUpdateListener(args.chatId) {
                answerCallAfterJoin()
            }
        }
    }

    override fun onJoinedChat(chatId: Long, userHandle: Long) {
        Timber.d("Joined to the chat")
        controlWhenJoinedAChat(chatId)
    }

    private fun answerCallAfterJoin() {
        val call = inMeetingViewModel.getCall()
        if (call == null) {
            Timber.d("Call is null")
            isWaitingForAnswerCall = true
        } else {
            isWaitingForAnswerCall = false
            Timber.d("Joined to chat, answer call")
            answerCall()
        }
    }

    /**
     * Method to answer the call
     */
    private fun answerCall() {
        var audio = micIsEnable
        if (audio) {
            audio =
                PermissionUtils.hasPermissions(requireContext(), Manifest.permission.RECORD_AUDIO)
        }

        var video = camIsEnable
        if (video) {
            video = PermissionUtils.hasPermissions(requireContext(), Manifest.permission.CAMERA)
        }

        sharedModel.answerCall(video, audio, speakerIsEnable)
            .observe(viewLifecycleOwner) { (chatHandle) ->
                checkCallStarted(chatHandle)
            }
    }

    /**
     * Method to start the call
     */
    private fun startCall() {
        var audio = micIsEnable
        if (audio) {
            audio =
                PermissionUtils.hasPermissions(requireContext(), Manifest.permission.RECORD_AUDIO)
        }

        var video = camIsEnable
        if (video) {
            video = PermissionUtils.hasPermissions(requireContext(), Manifest.permission.CAMERA)
        }

        inMeetingViewModel.startMeeting(video, audio)
            .observe(viewLifecycleOwner) { chatIdResult ->
                checkCallStarted(chatIdResult)
            }
    }

    override fun onErrorJoinedChat(chatId: Long, userHandle: Long, error: Int) {
        Timber.d("Error joining the meeting so close it, error code is $error")
        finishActivity()
    }

    /**
     * The dialog for alerting the meeting is failed to created
     */
    private fun showMeetingFailedDialog() {
        failedDialog = MaterialAlertDialogBuilder(
            requireContext(),
            R.style.ThemeOverlay_Mega_MaterialAlertDialog
        ).setMessage(StringResourcesUtils.getString(R.string.meeting_is_failed_content))
            .setCancelable(false)
            .setPositiveButton(R.string.general_ok) { _, _ ->
                rtcAudioManagerGateway.removeRTCAudioManager()
                finishActivity()
            }.show()
    }

    /**
     * Send add contact invitation
     *
     * @param peerId the peerId of users
     */
    fun addContact(peerId: Long) {
        inMeetingViewModel.addContact(requireContext(), peerId) { content ->
            sharedModel.showSnackBar(content)
        }
    }

    /**
     * Show Call will end banner
     *
     * @param milliseconds Time remaining until the end of the call
     */
    private fun showCallWillEndInBanner(milliseconds: Long) {
        callBanner?.apply {
            collapsePanel()

            isVisible = true
            text = StringResourcesUtils.getString(
                R.string.calls_call_screen_count_down_timer_to_end_call,
                TimeUtils.getMinutesAndSecondsFromMilliseconds(milliseconds)
            )

            countDownTimerToEndCall?.cancel()
            countDownTimerToEndCall = object :
                CountDownTimer(milliseconds, MILLISECONDS_IN_ONE_SECOND) {
                override fun onTick(millisUntilFinished: Long) {
                    text = StringResourcesUtils.getString(
                        R.string.calls_call_screen_count_down_timer_to_end_call,
                        TimeUtils.getMinutesAndSecondsFromMilliseconds(millisUntilFinished)
                    )
                }

                override fun onFinish() {
                    countDownTimerToEndCall = null
                    this@apply.isVisible = false
                }
            }.start()
        }
    }

    /**
     * Dialogue displayed when you are left alone in the group call or meeting and you can stay on the call or end it
     */
    private fun showOnlyMeInTheCallDialog() {
        if (MegaApplication.getChatManagement().hasEndCallDialogBeenIgnored) {
            dismissDialog(onlyMeDialog)
            return
        }

        onlyMeDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(StringResourcesUtils.getString(R.string.calls_call_screen_dialog_title_only_you_in_the_call))
            .setMessage(StringResourcesUtils.getString(R.string.calls_call_screen_dialog_description_only_you_in_the_call))
            .setPositiveButton(R.string.calls_call_screen_button_to_end_call) { _, _ ->
                inMeetingViewModel.checkEndCall()
            }
            .setNegativeButton(R.string.calls_call_screen_button_to_stay_alone_in_call) { _, _ ->
                inMeetingViewModel.checkStayCall()
            }
            .setCancelable(false)
            .create()
        onlyMeDialog?.show()

    }

    /**
     * Show Waiting for others banner
     */
    private fun showWaitingForOthersBanner() {
        callBanner?.apply {
            if (!isVisible) {
                collapsePanel()
                isVisible = true
            }

            text = StringResourcesUtils.getString(
                R.string.calls_call_screen_waiting_for_participants)
        }
    }

    /**
     * Method to show call will end banner and only me dialog
     */
    private fun showCallWillEndBannerAndOnlyMeDialog() {
        inMeetingViewModel.startCounterTimerAfterBanner()
        val currentTime =
            MegaApplication.getChatManagement().millisecondsOnlyMeInCallDialog
        showCallWillEndInBanner(if (currentTime > 0) currentTime else TimeUnit.SECONDS.toMillis(
            SECONDS_TO_WAIT_ALONE_ON_THE_CALL))
        showOnlyMeInTheCallDialog()
    }

    /**
     * Method to hide call banner and only me dialog
     */
    private fun hideCallBannerAndOnlyMeDialog() {
        hideCallWillEndInBanner()
        dismissDialog(onlyMeDialog)
    }

    /**
     * Hide call banner and counter down timer
     */
    private fun hideCallWillEndInBanner() {
        callBanner?.apply {
            if (isVisible) {
                isVisible = false
                countDownTimerToEndCall?.cancel()
                countDownTimerToEndCall = null
            }
        }
    }

    /**
     * Dismiss the dialog
     *
     * @param dialog the dialog should be dismiss
     */
    fun dismissDialog(dialog: Dialog?) {
        dialog?.let {
            if (it.isShowing) it.dismiss()
        }
    }
}