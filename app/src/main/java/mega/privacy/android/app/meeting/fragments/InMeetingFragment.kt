package mega.privacy.android.app.meeting.fragments

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.SystemClock
import android.util.DisplayMetrics
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import de.palm.composestateevents.consumed
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.components.ChatManagement
import mega.privacy.android.app.components.twemoji.EmojiTextView
import mega.privacy.android.app.databinding.InMeetingFragmentBinding
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.listeners.AutoJoinPublicChatListener
import mega.privacy.android.app.listeners.ChatChangeVideoStreamListener
import mega.privacy.android.app.main.legacycontact.AddContactActivity
import mega.privacy.android.app.mediaplayer.service.AudioPlayerService.Companion.pauseAudioPlayer
import mega.privacy.android.app.mediaplayer.service.AudioPlayerService.Companion.resumeAudioPlayerIfNotInCall
import mega.privacy.android.app.meeting.AnimationTool.fadeInOut
import mega.privacy.android.app.meeting.AnimationTool.moveX
import mega.privacy.android.app.meeting.AnimationTool.moveY
import mega.privacy.android.app.meeting.OnDragTouchListener
import mega.privacy.android.app.meeting.activity.MeetingActivity
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_ACTION_CREATE
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_ACTION_GUEST
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_ACTION_IN
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_ACTION_JOIN
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_ACTION_REJOIN
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_ACTION_RINGING_VIDEO_OFF
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_ACTION_RINGING_VIDEO_ON
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_ACTION_START
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_IS_GUEST
import mega.privacy.android.app.meeting.adapter.Participant
import mega.privacy.android.app.meeting.gateway.RTCAudioManagerGateway
import mega.privacy.android.app.meeting.listeners.BottomFloatingPanelListener
import mega.privacy.android.app.meeting.pip.PictureInPictureCallFragment
import mega.privacy.android.app.presentation.chat.dialog.AddParticipantsNoContactsDialogFragment
import mega.privacy.android.app.presentation.chat.dialog.AddParticipantsNoContactsLeftToAddDialogFragment
import mega.privacy.android.app.presentation.meeting.CallRecordingViewModel
import mega.privacy.android.app.presentation.meeting.model.CallRecordingUIState
import mega.privacy.android.app.presentation.meeting.model.InMeetingUiState
import mega.privacy.android.app.presentation.meeting.model.MeetingState
import mega.privacy.android.app.presentation.meeting.model.WaitingRoomManagementState
import mega.privacy.android.app.presentation.meeting.view.SnackbarInMeetingView
import mega.privacy.android.app.presentation.meeting.view.sheet.LeaveMeetingBottomSheetView
import mega.privacy.android.app.presentation.meeting.view.sheet.MoreCallOptionsBottomSheetView
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.ChatUtil
import mega.privacy.android.app.utils.Constants.AVATAR_CHANGE
import mega.privacy.android.app.utils.Constants.CONTACT_TYPE_MEGA
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_IS_FROM_MEETING
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_CHAT
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_CHAT_ID
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_CONTACT_TYPE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_MAX_USER
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_TOOL_BAR_TITLE
import mega.privacy.android.app.utils.Constants.INVALID_POSITION
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import mega.privacy.android.app.utils.Constants.NAME_CHANGE
import mega.privacy.android.app.utils.Constants.PERMISSIONS_TYPE
import mega.privacy.android.app.utils.Constants.REQUEST_ADD_PARTICIPANTS
import mega.privacy.android.app.utils.Constants.SECONDS_TO_WAIT_ALONE_ON_THE_CALL
import mega.privacy.android.app.utils.Constants.SNACKBAR_TYPE
import mega.privacy.android.app.utils.RunOnUIThreadUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.VideoCaptureUtils
import mega.privacy.android.app.utils.permission.PermissionUtils
import mega.privacy.android.app.utils.permission.permissionsBuilder
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.domain.entity.call.AnotherCallType
import mega.privacy.android.domain.entity.call.AudioDevice
import mega.privacy.android.domain.entity.call.CallUIStatusType
import mega.privacy.android.domain.entity.call.ChatCallStatus
import mega.privacy.android.domain.entity.call.ChatSession
import mega.privacy.android.domain.entity.call.ChatSessionStatus
import mega.privacy.android.domain.entity.chat.ChatConnectionStatus
import mega.privacy.android.domain.entity.meeting.ParticipantsSection
import mega.privacy.android.domain.entity.meeting.SubtitleCallType
import mega.privacy.android.domain.entity.meeting.TypeRemoteAVFlagChange
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.shared.original.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.resources.R as sharedR
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaUser.VISIBILITY_VISIBLE
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import mega.privacy.android.shared.resources.R as sharedResR

@AndroidEntryPoint
class InMeetingFragment : MeetingBaseFragment(), BottomFloatingPanelListener, SnackbarShower,
    AutoJoinPublicChatListener.OnJoinedChatCallback {

    @Inject
    @MegaApi
    lateinit var megaApi: MegaApiAndroid

    @Inject
    lateinit var rtcAudioManagerGateway: RTCAudioManagerGateway

    @Inject
    lateinit var chatManagement: ChatManagement

    @Inject
    lateinit var megaNavigator: MegaNavigator

    private val callRecordingViewModel: CallRecordingViewModel by activityViewModels()

    val args: InMeetingFragmentArgs by navArgs()

    // Views
    lateinit var toolbar: MaterialToolbar
    lateinit var blink: Animation

    private var toolbarTitle: EmojiTextView? = null
    private var toolbarSubtitle: TextView? = null
    private var meetingChrono: Chronometer? = null
    private var subtitleType: SubtitleCallType? = null

    private lateinit var bannerAnotherCallLayout: View
    private var bannerAnotherCallTitle: EmojiTextView? = null
    private var bannerAnotherCallSubtitle: TextView? = null

    private lateinit var bannerMuteLayout: View
    private var bannerMuteText: EmojiTextView? = null
    private var bannerMuteIcon: ImageView? = null
    private var bannerInfo: TextView? = null

    private lateinit var floatingWindowContainer: View
    private lateinit var floatingBottomSheet: View

    var bottomFloatingPanelViewHolder: BottomFloatingPanelViewHolder? = null

    private var swapCameraMenuItem: MenuItem? = null
    private var gridViewMenuItem: MenuItem? = null
    private var speakerViewMenuItem: MenuItem? = null
    private var speakerIsEnable = false
    private var isManualModeView = false
    private var isWaitingForAnswerCall = false
    private var isWaitingForMakeModerator = false
    private var shouldExpandBottomPanel = false

    // Children fragments
    private var individualCallFragment: IndividualCallFragment? = null
    private var floatingWindowFragment: IndividualCallFragment? = null
    private var gridViewCallFragment: GridViewCallFragment? = null
    private var speakerViewCallFragment: SpeakerViewCallFragment? = null
    private var pictureCallFragment: PictureInPictureCallFragment? = null

    // For internal UI/UX use
    private var previousY = -1f
    private var lastTouch: Long = 0
    private lateinit var dragTouchListener: OnDragTouchListener
    private var bannerShouldBeShown = false

    private var isParticipantSharingScreen: Boolean = false

    private lateinit var binding: InMeetingFragmentBinding

    // Leave Meeting Dialog
    private var leaveDialog: Dialog? = null

    // Meeting failed Dialog
    private var failedDialog: Dialog? = null

    private var endMeetingAsModeratorDialog: EndMeetingAsModeratorBottomSheetDialogFragment? = null

    // Only me in the call Dialog
    private var onlyMeDialog: Dialog? = null


    private var countDownTimerToEndCall: CountDownTimer? = null

    private var orientation: Int = Configuration.ORIENTATION_PORTRAIT

    val inMeetingViewModel: InMeetingViewModel by activityViewModels()

    /**
     * Method that updates the Bottom panel
     */
    private fun updatePanel() {
        bottomFloatingPanelViewHolder?.updatePanel(false)
    }

    private fun sessionStatusChanged(session: ChatSession) {
        when (session.status) {
            ChatSessionStatus.Invalid -> {}
            ChatSessionStatus.Progress -> {
                Timber.d("Session in progress, clientID = ${session.clientId}")
                inMeetingViewModel.addParticipant(
                    session
                )?.let { position ->
                    if (inMeetingViewModel.isOneToOneCall() == false) {
                        if (position != INVALID_POSITION) {
                            checkChildFragments()
                            participantAddedOrLeftMeeting(true, position)
                        }
                    } else {
                        checkChildFragments()
                    }
                }
            }

            ChatSessionStatus.Destroyed -> {
                Timber.d("Session destroyed, clientID = ${session.clientId}")
                inMeetingViewModel.removeParticipant(
                    session
                ).let { position ->
                    if (inMeetingViewModel.isOneToOneCall() == false) {
                        if (position != INVALID_POSITION) {
                            checkChildFragments()
                            participantAddedOrLeftMeeting(false, position)
                        }
                    } else {
                        checkChildFragments()
                    }
                }
            }
        }

        showMuteBanner()
    }

    private fun sessionLowRes(session: ChatSession) {
        inMeetingViewModel.getParticipant(
            session.peerId,
            session.clientId
        )?.let { participant ->
            val canRecvVideoLowRes = session.canReceiveVideoLowRes
            val sessionIsLowResVideo = session.isLowResVideo
            val sessionHasVideo =
                inMeetingViewModel.sessionHasVideo(participant.clientId)
            val participantHasLowRes = !participant.hasHiRes

            if (participantHasLowRes) {
                when {
                    canRecvVideoLowRes && sessionIsLowResVideo -> {
                        Timber.d("Add participant listener for ${participant.clientId}")
                        //Can receive Low res. Add listener
                        checkVideoListener(
                            participant,
                            shouldAddListener = true,
                            isHiRes = false
                        )
                    }

                    else -> {
                        Timber.d("Remove participant listener for ${participant.clientId}")
                        //Cannot receive Low res. Remove listener
                        checkVideoListener(
                            participant,
                            shouldAddListener = false,
                            isHiRes = false
                        )

                        //I have stopped receiving LowResolution. I have to verify that I no longer need it.
                        if (sessionHasVideo && inMeetingViewModel.isParticipantVisible(
                                participant
                            )
                        ) {
                            Timber.d("Ask for low-resolution video for ${participant.clientId}")
                            inMeetingViewModel.requestLowResVideo(
                                session,
                                inMeetingViewModel.getChatId()
                            )

                        } else {
                            Timber.d("it is not necessary to ask for a low-resolution for ${participant.clientId}")
                        }
                    }
                }
            }
        }
    }

    private fun sessionHiRes(session: ChatSession) {
        inMeetingViewModel.getParticipant(
            session.peerId, session.clientId
        )?.let { participant ->
            val speaker: Participant? =
                if (participant.isSpeaker && !participant.hasHiRes) inMeetingViewModel.getSpeaker(
                    participant.peerId, participant.clientId
                ) ?: run { null } else null

            val existSpeaker: Boolean = speaker != null

            val screenSharedParticipant = inMeetingViewModel.getScreenShared(
                participant.peerId, participant.clientId
            )

            val existScreenShared = if (participant.isPresenting) screenSharedParticipant?.let {
                true
            } ?: run { false } else false

            if (session.canReceiveVideoHiRes && session.isHiResVideo) {
                //Can receive Hi res. Add listener.
                when {
                    existSpeaker -> {
                        //Speaker listener
                        Timber.d("Add speaker listener for ${participant.clientId}")
                        checkSpeakerVideoListener(
                            session.peerId, session.clientId, shouldAddListener = true
                        )
                    }

                    existScreenShared -> {
                        //Screen shared listener
                        Timber.d("Add screen shared listener for ${participant.clientId}")
                        screenSharedParticipant?.let {
                            checkVideoListener(
                                it, shouldAddListener = true, isHiRes = true
                            )
                        }
                    }

                    else -> {
                        //Participant listener
                        Timber.d("Add participant listener for ${participant.clientId}")
                        checkVideoListener(
                            participant, shouldAddListener = true, isHiRes = true
                        )
                    }
                }

            } else if (!participant.hasHiRes && participant.isVideoOn) {
                val isParticipantVisible = inMeetingViewModel.isParticipantVisible(participant)
                if ((existScreenShared && isParticipantVisible) || existSpeaker) {
                    Timber.d("Ask for high-resolution video for ${participant.clientId}")
                    inMeetingViewModel.requestHiResVideo(
                        inMeetingViewModel.getSessionByClientId(session.clientId),
                        inMeetingViewModel.getChatId()
                    )

                } else {
                    Timber.d("it is not necessary to ask for a high-resolution for ${participant.clientId}")
                }
            } else if (!session.canReceiveVideoHiRes) {
                if (session.hasVideo) {
                    if (!existSpeaker) {
                        Timber.d("Ask for high-resolution video for ${participant.clientId}")
                        inMeetingViewModel.requestHiResVideo(
                            inMeetingViewModel.getSessionByClientId(session.clientId),
                            inMeetingViewModel.getChatId()
                        )

                    } else {
                        Timber.d("it is not necessary to ask for a high-resolution for ${participant.clientId}")
                    }
                } else {
                    //Cannot receive Hi res. Remove listener.
                    when {
                        existSpeaker -> {
                            //Speaker listener
                            Timber.d("Remove speaker listener for ${participant.clientId}")
                            checkSpeakerVideoListener(
                                session.peerId, session.clientId, shouldAddListener = false
                            )
                        }

                        existScreenShared -> {
                            //Screen shared listener
                            Timber.d("Remove screen shared listener for ${participant.clientId}")
                            screenSharedParticipant?.let {
                                checkVideoListener(
                                    it, shouldAddListener = false, isHiRes = true
                                )
                            }
                        }

                        else -> {
                            //Participant listener
                            Timber.d("Remove participant listener for ${participant.clientId}")
                            checkVideoListener(
                                participant, shouldAddListener = false, isHiRes = true
                            )
                        }
                    }
                }

            } else {
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

    private fun updateCurrentOrientation() {
        orientation = resources.configuration.orientation
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("In the meeting fragment")
        meetingActivity.consumeInsetsWithToolbar()
        updateCurrentOrientation()
        blink = AnimationUtils.loadAnimation(requireContext(), R.anim.blink)

        initViewModel()
        MegaApplication.getInstance().startProximitySensor()
        initToolbar()
        initFloatingWindowContainerDragListener(view)
        initFloatingPanel()

        binding.snackbarComposeView.apply {
            isVisible = true
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setViewTreeViewModelStoreOwner(requireActivity())
            setContent {
                OriginalTheme(isDark = true) {
                    SnackbarInMeetingView(
                        meetingActivityViewModel = sharedModel,
                        inMeetingViewModel = inMeetingViewModel
                    )
                }
            }
        }

        binding.hostLeaveCallDialogComposeView.apply {
            isVisible = true
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setViewTreeViewModelStoreOwner(requireActivity())
            setContent {
                val sharedState by sharedModel.state.collectAsStateWithLifecycle()
                val state by inMeetingViewModel.state.collectAsStateWithLifecycle()

                OriginalTheme(isDark = true) {
                    LeaveMeetingBottomSheetView(
                        state = state,
                        onAssignAndLeaveClick = {
                            showAssignModeratorFragment()
                        },
                        onLeaveAnywayClick = inMeetingViewModel::hangCurrentCall,
                        onEndForAllClick = inMeetingViewModel::endCallForAll,
                        onDismiss = inMeetingViewModel::hideBottomPanels
                    )
                    if (sharedState.isCallUnlimitedProPlanFeatureFlagEnabled && state.showMeetingEndWarningDialog) {
                        MegaAlertDialog(
                            title = pluralStringResource(
                                R.plurals.meetings_in_call_warning_dialog_title,
                                inMeetingViewModel.state.value.minutesToEndMeeting ?: 1,
                                inMeetingViewModel.state.value.minutesToEndMeeting ?: 1
                            ),
                            text = stringResource(id = R.string.meetings_in_call_warning_dialog_body),
                            confirmButtonText = stringResource(id = sharedR.string.general_upgrade_button),
                            cancelButtonText = stringResource(id = R.string.meetings_in_call_warning_dialog_negative_button),
                            onConfirm = {
                                inMeetingViewModel.onMeetingEndWarningDialogDismissed()
                                megaNavigator.openUpgradeAccount(context = context)
                            },
                            onDismiss = {
                                inMeetingViewModel.onMeetingEndWarningDialogDismissed()
                            }
                        )
                    }
                }
            }
        }

        binding.moreOptionsListComposeView.apply {
            isVisible = true
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setViewTreeViewModelStoreOwner(requireActivity())
            setContent {
                OriginalTheme(isDark = true) {
                    MoreCallOptionsBottomSheetView()
                }
            }
        }

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

            MEETING_ACTION_GUEST -> inMeetingViewModel.joinMeetingAsGuest(
                sharedModel.state.value.meetingLink,
                sharedModel.state.value.guestFirstName,
                sharedModel.state.value.guestLastName
            )

            MEETING_ACTION_RINGING_VIDEO_ON -> {
                Timber.d("Action ringing with video on")
                inMeetingViewModel.getCall()?.let {
                    if (it.hasLocalAudio) {
                        sharedModel.micInitiallyOn()
                    }
                    if (it.hasLocalVideo) {
                        sharedModel.camInitiallyOn()
                    }
                }
            }

            MEETING_ACTION_RINGING_VIDEO_OFF -> {
                Timber.d("Action ringing with video off")
                inMeetingViewModel.getCall()?.let {
                    if (inMeetingViewModel.state.value.hasLocalVideo) {
                        sharedModel.micInitiallyOn()
                    }
                }
            }

            MEETING_ACTION_START -> {
                Timber.d("Action need answer call")
                updateMicAndCam()
                controlWhenJoinedAChat(inMeetingViewModel.getChatId())
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
        Timber.d("onConfigurationChanged called")
        super.onConfigurationChanged(newConfig)
        val outMetrics = DisplayMetrics()
        val display = meetingActivity.windowManager.defaultDisplay
        display.getMetrics(outMetrics)
        if (inMeetingViewModel.state.value.isInPipMode.not()) {
            bottomFloatingPanelViewHolder?.updateWidth(
                newConfig.orientation,
                outMetrics.widthPixels
            )

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
                Timber.d("Update floating window layout")
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
    }

    private fun initToolbar() {
        Timber.d("Update toolbar elements")
        val root = meetingActivity.binding.root
        toolbar = meetingActivity.binding.toolbar
        toolbar.isVisible = true
        toolbarTitle = meetingActivity.binding.titleToolbar
        toolbarSubtitle = meetingActivity.binding.subtitleToolbar

        root.apply {
            setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.grey_900))
        }

        toolbar.apply {
            setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.dark_grey_alpha_070
                )
            )
        }

        meetingActivity.window.statusBarColor =
            ContextCompat.getColor(requireContext(), R.color.dark_grey_alpha_070)


        toolbarTitle?.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        toolbarSubtitle?.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))

        bannerAnotherCallLayout = meetingActivity.binding.bannerAnotherCall
        bannerAnotherCallTitle = meetingActivity.binding.bannerAnotherCallTitle
        bannerAnotherCallSubtitle = meetingActivity.binding.bannerAnotherCallSubtitle

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
        meetingActivity.binding.toolbar.setOnClickListener {
            inMeetingViewModel.onToolbarTap(true)
        }
        meetingActivity.binding.callBannerCompose.apply {
            isVisible = true
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                OriginalTheme(isDark = false) {
                    MeetingBanner(inMeetingViewModel)
                }
            }
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
     * Checks updates in remote AV flags
     *
     * @param chatSession   [ChatSession]
     */
    private fun updatesInRemoteAVFlags(chatSession: ChatSession) {
        showMuteBanner()
        if (inMeetingViewModel.isOneToOneCall() == false) {
            val isAudioChange =
                inMeetingViewModel.changesInRemoteAudioFlag(chatSession)
            val isVideoChange =
                inMeetingViewModel.changesInRemoteVideoFlag(chatSession)
            val isPresentingChange =
                inMeetingViewModel.changesInScreenSharing(chatSession)
            Timber.d("Changes in AV flags. audio change $isAudioChange, video change $isVideoChange, is screen share change $isPresentingChange")
            updateRemoteAVFlags(
                chatSession,
                isAudioChange,
                isVideoChange,
                isPresentingChange
            )
        }
    }

    private fun collectFlows() {
        viewLifecycleOwner.collectFlow(sharedModel.state.map { it.micEnabled }
            .distinctUntilChanged()) {
            updateLocalAudio(it)
        }

        viewLifecycleOwner.collectFlow(sharedModel.state.map { it.answerResult }
            .distinctUntilChanged()) {
            it?.apply {
                checkCallStarted(chatHandle)
            }
        }

        viewLifecycleOwner.collectFlow(sharedModel.state.map { it.handRaisedSnackbarMsg }
            .distinctUntilChanged()) {
            if (it == consumed()) {
                binding.snackbarComposeView.apply {
                    isVisible = false
                }
            } else {
                binding.snackbarComposeView.apply {
                    isVisible = true
                }
            }
        }

        viewLifecycleOwner.collectFlow(sharedModel.state.map { it.camEnabled }
            .distinctUntilChanged()) {
            updateLocalVideo(it)
        }

        viewLifecycleOwner.collectFlow(sharedModel.state.map { it.speakerType }
            .distinctUntilChanged()) {
            Timber.d("Speaker status has changed to $it")
            speakerIsEnable = it == AudioDevice.SpeakerPhone
            updateSpeaker(it)
        }

        viewLifecycleOwner.collectFlow(inMeetingViewModel.state.map { it.joinedAsGuest }
            .distinctUntilChanged()) { joinedAsGuest ->
            if (joinedAsGuest) {
                inMeetingViewModel.onJoinedAsGuestConsumed()
                controlWhenJoinedAChat(inMeetingViewModel.state.value.currentChatId)
            }
        }

        viewLifecycleOwner.collectFlow(inMeetingViewModel.state.map { it.isVideoEnabledDueToProximitySensor }
            .distinctUntilChanged()) {
            it?.let { shouldBeEnabled ->
                if (inMeetingViewModel.getChatId() != MEGACHAT_INVALID_HANDLE && inMeetingViewModel.getCall() != null && shouldBeEnabled != sharedModel.state.value.camEnabled) {
                    MegaApplication.getChatManagement().isDisablingLocalVideo = true
                    inMeetingViewModel.onVideoEnabledDueToProximitySensorConsumed()
                    sharedModel.clickCamera(shouldBeEnabled)
                }
            }
        }

        viewLifecycleOwner.collectFlow(inMeetingViewModel.state.map { it.snackbarMsg }
            .distinctUntilChanged()) { msg ->
            msg?.let {
                inMeetingViewModel.onSnackbarMsgConsumed()
                showSnackbar(
                    SNACKBAR_TYPE,
                    msg,
                    MEGACHAT_INVALID_HANDLE
                )
            }
        }

        viewLifecycleOwner.collectFlow(inMeetingViewModel.state) { state: InMeetingUiState ->
            if (state.shouldFinish) {
                finishActivity()
            }

            if (state.error != null) {
                sharedModel.showSnackBar(getString(state.error))
            }

            if (state.updateListUi) {
                inMeetingViewModel.onUpdateListConsumed()
                speakerViewCallFragment?.let {
                    if (it.isAdded) {
                        it.updateFullList()
                    }
                }
            }

            if (state.chatTitle.isNotEmpty()) {
                toolbarTitle?.apply {
                    text = state.chatTitle
                }
            }

            meetingChrono?.apply {
                if (state.showCallDuration) {
                    if (!isVisible) {
                        inMeetingViewModel.getCallDuration().let { duration ->
                            if (duration != INVALID_VALUE.toLong()) {
                                base =
                                    SystemClock.elapsedRealtime() - duration * MILLISECONDS_IN_ONE_SECOND
                                start()
                                isVisible = true
                            }
                        }
                    }
                } else {
                    stop()
                    isVisible = false
                }
            }

            toolbarSubtitle?.apply {
                if (subtitleType != state.updateCallSubtitle) {
                    subtitleType = state.updateCallSubtitle
                    text = when (state.updateCallSubtitle) {
                        SubtitleCallType.Connecting -> getString(R.string.chat_connecting)
                        SubtitleCallType.Calling -> getString(R.string.outgoing_call_starting)
                        SubtitleCallType.Established -> ""
                    }
                }
            }

            state.addScreensSharedParticipantsList?.let {
                inMeetingViewModel.addScreenShareParticipant(it)
                    ?.let { position ->
                        if (position != INVALID_POSITION) {
                            checkChildFragments()
                            participantAddedOrLeftMeeting(true, position)
                        }
                    }
            }

            state.removeScreensSharedParticipantsList?.let {
                inMeetingViewModel.removeScreenShareParticipant(it)
                    .let { position ->
                        if (position != INVALID_POSITION) {
                            checkChildFragments()
                            participantAddedOrLeftMeeting(false, position)
                        }
                    }
            }

            if (state.showMeetingInfoFragment) {
                inMeetingViewModel.onToolbarTap(false)
                showMeetingInfoFragment()
            }

            bannerAnotherCallTitle?.apply {
                if (state.anotherChatTitle.isNotEmpty()) {
                    text = state.anotherChatTitle
                }
            }

            when (state.updateAnotherCallBannerType) {
                AnotherCallType.NotCall -> {
                    if (bannerAnotherCallLayout.isVisible) {
                        Timber.d("No other calls in progress or on hold")
                        bannerAnotherCallLayout.isVisible = false
                    }
                }

                AnotherCallType.CallInProgress -> {
                    if (!bannerAnotherCallLayout.isVisible) {
                        bannerAnotherCallLayout.let {
                            it.alpha = 1f
                            it.isVisible = true
                        }
                        bannerAnotherCallSubtitle?.text =
                            getString(R.string.call_in_progress_layout)
                    }
                }

                AnotherCallType.CallOnHold -> {
                    if (!bannerAnotherCallLayout.isVisible) {
                        bannerAnotherCallLayout.let {
                            it.alpha = 0.9f
                            it.isVisible = true
                        }

                        bannerAnotherCallSubtitle?.text =
                            getString(R.string.call_on_hold)
                    }
                }
            }

            sharedModel.setSpeakerView(isSpeakerMode = state.callUIStatus == CallUIStatusType.SpeakerView)

            if (state.showEndMeetingAsHostBottomPanel && state.isInPipMode.not()) {
                if (endMeetingAsModeratorDialog == null) {
                    endMeetingAsModeratorDialog = EndMeetingAsModeratorBottomSheetDialogFragment()
                    endMeetingAsModeratorDialog?.run {
                        setDismissCallBack {
                            inMeetingViewModel.hideBottomPanels()
                        }
                        setLeaveMeetingCallBack {
                            inMeetingViewModel.hangCurrentCall()
                        }
                        setEndForAllCallBack {
                            inMeetingViewModel.endCallForAll()
                        }
                        show(
                            this@InMeetingFragment.childFragmentManager,
                            tag
                        )
                    }
                }
            } else {
                endMeetingAsModeratorDialog?.dismissAllowingStateLoss()
                endMeetingAsModeratorDialog = null
            }
            if (state.showMeetingEndWarningDialog) {
                collapsePanel()
            }
            bottomFloatingPanelViewHolder?.setRaiseHandToolTipShown(state.isRaiseToHandSuggestionShown)
        }

        viewLifecycleOwner.collectFlow(inMeetingViewModel.state.map { it.userIdsWithChangesInRaisedHand }
            .distinctUntilChanged()) {
            if (it.isNotEmpty()) {
                updateParticipantsWithHandRaised(it)
            }
        }

        viewLifecycleOwner.collectFlow(sharedModel.state.map { it.startedMeetingChatId }
            .distinctUntilChanged()) {
            it?.let { chatId ->
                checkCallStarted(chatId)
            }
        }

        viewLifecycleOwner.collectFlow(inMeetingViewModel.state.map { it.call?.callId }
            .distinctUntilChanged(), minActiveState = Lifecycle.State.CREATED) { callId ->
            callId?.run {
                if (args.action == MEETING_ACTION_IN) {
                    if (arguments?.getBoolean(
                            MeetingActivity.MEETING_AUDIO_ENABLE,
                            false
                        ) == true
                    ) {
                        Timber.d("Action in with audio enable")
                        sharedModel.clickMic(true)
                    }
                    if (arguments?.getBoolean(
                            MeetingActivity.MEETING_VIDEO_ENABLE,
                            false
                        ) == true
                    ) {
                        Timber.d("Action in with camera enable")
                        sharedModel.clickCamera(true)
                    }
                    if (arguments?.getBoolean(
                            MeetingActivity.MEETING_SPEAKER_ENABLE,
                            false
                        ) == true
                    ) {
                        Timber.d("Action in with speaker enable")
                        sharedModel.clickSpeaker()
                    }
                }
            }
        }

        viewLifecycleOwner.collectFlow(inMeetingViewModel.state.map { it.isOneToOneCall }
            .distinctUntilChanged()) {
            it?.let { isOneToOneCall ->
                updatePanel()
            }
        }

        viewLifecycleOwner.collectFlow(inMeetingViewModel.state.map { it.shouldShowSwapCamera }
            .distinctUntilChanged()) {
            swapCameraMenuItem?.apply {
                isVisible = it
            }
        }

        viewLifecycleOwner.collectFlow(inMeetingViewModel.state.map { it.areButtonsEnabled }
            .distinctUntilChanged()) {
            bottomFloatingPanelViewHolder?.disableEnableButtons(it)
        }

        viewLifecycleOwner.collectFlow(inMeetingViewModel.state.map { it.call?.status }
            .distinctUntilChanged()) { callStatus ->
            callStatus?.let { status ->
                when (status) {
                    ChatCallStatus.Initial -> {}

                    ChatCallStatus.Connecting -> {
                        checkChildFragments()
                        checkMenuItemsVisibility()
                    }

                    ChatCallStatus.InProgress -> {
                        checkMenuItemsVisibility()
                        checkChildFragments()
                        controlVideoLocalOneToOneCall(
                            inMeetingViewModel.state.value.call?.hasLocalVideo ?: false
                        )
                    }

                    ChatCallStatus.TerminatingUserParticipation, ChatCallStatus.Destroyed -> {
                        disableCamera()
                        removeUI()
                    }

                    else -> {}
                }
            }
        }

        viewLifecycleOwner.collectFlow(inMeetingViewModel.state.map { it.call }
            .distinctUntilChanged()) {
            if (it != null && isWaitingForAnswerCall) {
                answerCallAfterJoin()
            }
        }

        viewLifecycleOwner.collectFlow(inMeetingViewModel.state.map { it.hasLocalAudio }
            .distinctUntilChanged()) { audio ->
            showMuteBanner()
        }

        viewLifecycleOwner.collectFlow(inMeetingViewModel.state.map { it.changesInAVFlagsInSession }
            .distinctUntilChanged()) {
            it?.let { chatSession ->
                updatesInRemoteAVFlags(chatSession)
            }
        }

        viewLifecycleOwner.collectFlow(inMeetingViewModel.state.map { it.changesInAudioLevelInSession }
            .distinctUntilChanged()) {
            it?.let { chatSession ->
                updatesInRemoteAVFlags(chatSession)
            }
        }

        viewLifecycleOwner.collectFlow(inMeetingViewModel.state.map { it.changesInHiResInSession }
            .distinctUntilChanged()) {
            it?.let { chatSession ->
                if (inMeetingViewModel.isOneToOneCall() == false) {
                    sessionHiRes(chatSession)
                }
            }
        }

        viewLifecycleOwner.collectFlow(inMeetingViewModel.state.map { it.changesInLowResInSession }
            .distinctUntilChanged()) {
            it?.let { chatSession ->
                if (inMeetingViewModel.isOneToOneCall() == false) {
                    sessionLowRes(chatSession)
                }
            }
        }

        viewLifecycleOwner.collectFlow(inMeetingViewModel.state.map { it.changesInStatusInSession }
            .distinctUntilChanged()) {
            it?.let { chatSession ->
                sessionStatusChanged(chatSession)
            }
        }

        viewLifecycleOwner.collectFlow(inMeetingViewModel.state.map { it.shouldCheckChildFragments }
            .distinctUntilChanged()) {
            if (it) {
                inMeetingViewModel.checkUpdatesInCallComposition(update = false)
                checkChildFragments()
            }
        }

        viewLifecycleOwner.collectFlow(inMeetingViewModel.state.map { it.isCallOnHold }
            .distinctUntilChanged()) {
            it?.let { isOnHold ->
                isCallOnHold(isOnHold)
            }
        }

        viewLifecycleOwner.collectFlow(inMeetingViewModel.state.map { it.sessionOnHoldChanges }
            .distinctUntilChanged()) { session ->
            session?.let {
                showMuteBanner()
                when (inMeetingViewModel.isOneToOneCall()) {
                    true -> if (inMeetingViewModel.state.value.hasLocalVideo && inMeetingViewModel.state.value.isCallOnHold == true) {
                        sharedModel.clickCamera(false)
                    }

                    false -> updateOnHoldRemote(session = it)
                    null -> {}
                }
            }
        }

        viewLifecycleOwner.collectFlow(inMeetingViewModel.state.map { it.showEndMeetingAsOnlyHostBottomPanel }
            .distinctUntilChanged()) { showedEndMeetingBottomPanel ->
            when {
                showedEndMeetingBottomPanel &&
                        floatingBottomSheet.isVisible -> {
                    Timber.d("floatingBottomSheet visibility is setting false")
                    floatingBottomSheet.isVisible = false
                }

                !showedEndMeetingBottomPanel &&
                        !inMeetingViewModel.state.value.showCallOptionsBottomSheet &&
                        !floatingBottomSheet.isVisible &&
                        toolbar.isVisible -> {
                    floatingBottomSheet.isVisible = true
                }
            }
        }

        viewLifecycleOwner.collectFlow(inMeetingViewModel.state.map { it.showCallOptionsBottomSheet }
            .distinctUntilChanged()) { showedCallOptionsBottomPanel ->
            when {
                showedCallOptionsBottomPanel &&
                        floatingBottomSheet.isVisible -> {
                    Timber.d("floatingBottomSheet visibility is setting false")
                    floatingBottomSheet.isVisible = false
                }

                !showedCallOptionsBottomPanel &&
                        !inMeetingViewModel.state.value.showEndMeetingAsOnlyHostBottomPanel &&
                        !floatingBottomSheet.isVisible &&
                        toolbar.isVisible -> {
                    floatingBottomSheet.isVisible = true
                }
            }
        }

        viewLifecycleOwner.collectFlow(sharedModel.state.map { it.chatId }
            .distinctUntilChanged()) {
            inMeetingViewModel.setChatId(it)
            callRecordingViewModel.setChatId(it)
        }

        viewLifecycleOwner.collectFlow(sharedModel.state) { state: MeetingState ->
            when {
                state.shouldParticipantInCallListBeShown -> {
                    if (floatingBottomSheet.isShown.not()) {
                        Timber.d("floatingBottomSheet.isShown = ${floatingBottomSheet.isShown}")
                        onPageClick()
                    }
                    sharedModel.showParticipantsList(shouldBeShown = false)
                    sharedModel.onHandRaisedSnackbarMsgConsumed()
                    if (bottomFloatingPanelViewHolder?.getState() != BottomSheetBehavior.STATE_EXPANDED) {
                        bottomFloatingPanelViewHolder?.expand()
                    }
                }

                state.shouldPinToSpeakerView -> {
                    state.chatParticipantSelected?.let {
                        inMeetingViewModel.onItemClick(it)
                        sharedModel.onPinToSpeakerView(false)
                    }
                }

                state.isParticipantSharingScreen != isParticipantSharingScreen -> {
                    isParticipantSharingScreen = state.isParticipantSharingScreen
                    speakerViewMenuItem?.apply {
                        isEnabled = !isParticipantSharingScreen
                        iconTintList = ColorStateList.valueOf(
                            ContextCompat.getColor(
                                requireContext(),
                                if (isParticipantSharingScreen) R.color.white_alpha_038 else R.color.white
                            )
                        )
                    }

                    gridViewMenuItem?.apply {
                        isEnabled = !isParticipantSharingScreen
                        iconTintList = ColorStateList.valueOf(
                            ContextCompat.getColor(
                                requireContext(),
                                if (isParticipantSharingScreen) R.color.white_alpha_038 else R.color.white
                            )
                        )
                    }

                    if (inMeetingViewModel.isOneToOneCall() == false) {
                        when (isParticipantSharingScreen) {
                            true -> changeToSpeakerView()
                            false -> changeToGridView()
                        }
                    }
                }

                state.isWaitingForCall && state.chatConnectionStatus == ChatConnectionStatus.Online -> {
                    startCall()
                    sharedModel.setIsWaitingForCall(false)
                }
            }

            setRecIndicatorVisibility()
        }

        viewLifecycleOwner.collectFlow(sharedModel.state.map { it.isMyHandRaisedToSpeak }
            .distinctUntilChanged()) {
            updateParticipantsBottomPanel()
        }

        viewLifecycleOwner.collectFlow(callRecordingViewModel.state) { state: CallRecordingUIState ->
            if (state.participantRecording != null) {
                showSnackbar(
                    SNACKBAR_TYPE, getString(
                        if (state.isSessionOnRecording) R.string.meetings_call_recording_started_snackbar_message else R.string.meetings_call_recording_stopped_snackbar_message,
                        state.participantRecording
                    ),
                    MEGACHAT_INVALID_HANDLE
                )
                callRecordingViewModel.setParticipantRecordingConsumed()
            }
        }

        viewLifecycleOwner.collectFlow(sharedWaitingRoomManagementViewModel.state) { state: WaitingRoomManagementState ->
            if (state.usersAdmitted) {
                sharedWaitingRoomManagementViewModel.onConsumeUsersAdmittedEvent()
                collapsePanel()
            }
            if (state.shouldWaitingRoomBeShown) {
                sharedWaitingRoomManagementViewModel.onConsumeShouldWaitingRoomBeShownEvent()
                sharedModel.updateParticipantsSection(ParticipantsSection.WaitingRoomSection)
                if (bottomFloatingPanelViewHolder?.getState() != BottomSheetBehavior.STATE_EXPANDED) {
                    bottomFloatingPanelViewHolder?.expand()
                }
            }
        }

        viewLifecycleOwner.collectFlow(inMeetingViewModel.state.map { it.isInPipMode }) {
            if (it) {
                checkChildFragments()
                Timber.d("Currently Picture in Picture Mode is $it")
            }
        }

        viewLifecycleOwner.collectFlow(
            inMeetingViewModel.monitorContactAndUserUpdate
                .debounce(1000)
        ) { userUpdate ->
            Timber.d("Contacts changed $userUpdate")
            val listParticipants = inMeetingViewModel.updateParticipantsName(userUpdate)
            if (listParticipants.isNotEmpty()) {
                gridViewCallFragment?.let {
                    if (it.isAdded) {
                        it.updateNameOrAvatar(listParticipants, NAME_CHANGE)
                    }
                }
                speakerViewCallFragment?.let {
                    if (it.isAdded) {
                        it.updateNameOrAvatar(listParticipants, NAME_CHANGE)
                    }
                }
            }
        }
    }

    /**
     * Update participants with hand raised
     *
     * @param list  List of ids of participants with hand raised
     */
    private fun updateParticipantsWithHandRaised(list: List<Long>) {
        inMeetingViewModel.cleanParticipantsWithRaisedOrLoweredHandsChanges()
        gridViewCallFragment?.apply {
            if (isAdded) {
                updateHandRaised(list.toMutableSet())
            }
        }

        speakerViewCallFragment?.apply {
            if (isAdded) {
                updateHandRaised(list.toMutableSet())
            }
        }
    }

    /**
     * Set the rec indicator visibility according to the call recording status values
     * and the visibility of the toolbar and the floating bottom sheet
     */
    private fun setRecIndicatorVisibility() {
        with(callRecordingViewModel.state.value) {
            if (inMeetingViewModel.state.value.isInPipMode.not()) {
                binding.recIndicator.visibility =
                    if (isSessionOnRecording && !toolbar.isVisible && !floatingBottomSheet.isVisible) View.VISIBLE
                    else View.GONE
            }
        }
    }

    /**
     * Force change to speaker view
     */
    private fun changeToSpeakerView() {
        isManualModeView = true
        initSpeakerViewMode()
    }

    /**
     * Force change to grid view
     */
    private fun changeToGridView() {
        isManualModeView = true
        initGridViewMode()
    }

    /**
     * Init View Models
     */
    private fun initViewModel() {
        collectFlows()

        sharedModel.snackBarLiveData.observe(viewLifecycleOwner) {
            if (it.isNotEmpty()) {
                showSnackbar(SNACKBAR_TYPE, it, MEGACHAT_INVALID_HANDLE)
            }
        }

        viewLifecycleOwner.collectFlow(sharedModel.cameraGranted) { allowed ->
            if (allowed) {
                bottomFloatingPanelViewHolder?.updateCamPermissionWaring(true)
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

        viewLifecycleOwner.collectFlow(sharedModel.recordAudioGranted) { allowed ->
            if (allowed) {
                bottomFloatingPanelViewHolder?.updateMicPermissionWaring(true)
            }
        }

        viewLifecycleOwner.collectFlow(sharedModel.monitorConnectivityEvent) { haveConnection ->
            inMeetingViewModel.updateNetworkStatus(haveConnection)
        }

        viewLifecycleOwner.collectFlow(inMeetingViewModel.updateCallId) {
            if (it != MEGACHAT_INVALID_HANDLE) {
                checkButtonsStatus()
                inMeetingViewModel.getCall()
                    ?.let { isCallOnHold(inMeetingViewModel.isCallOnHold()) }
            }
        }

        viewLifecycleOwner.collectFlow(inMeetingViewModel.getParticipantsChanges) { (type, getTitle) ->
            if (getTitle != null) {
                inMeetingViewModel.showParticipantChangesMessage(getTitle(requireContext()), type)
            } else {
                inMeetingViewModel.hideParticipantChangesMessage()
            }
        }

        viewLifecycleOwner.collectFlow(inMeetingViewModel.showOnlyMeBanner) { shouldBeShown ->
            checkMenuItemsVisibility()
            if (shouldBeShown && !MegaApplication.getChatManagement().hasEndCallDialogBeenIgnored) {
                showCallWillEndBannerAndOnlyMeDialog()
            } else {
                hideCallBannerAndOnlyMeDialog()
            }
        }

        viewLifecycleOwner.collectFlow(inMeetingViewModel.showWaitingForOthersBanner) { shouldBeShown ->
            checkMenuItemsVisibility()
            if (shouldBeShown.not()) {
                hideCallWillEndInBanner()
            }
        }

        viewLifecycleOwner.collectFlow(inMeetingViewModel.showPoorConnectionBanner) { shouldBeShown ->
            bannerInfo?.apply {
                alpha = 1f
                isVisible = shouldBeShown
                if (shouldBeShown) {
                    setBackgroundColor(
                        ContextCompat.getColor(
                            MegaApplication.getInstance().applicationContext,
                            R.color.dark_grey_alpha_070
                        )
                    )
                    text =
                        getString(R.string.calls_call_screen_poor_network_quality)
                }
            }

            if (shouldBeShown) {
                reconnecting()
            } else {
                checkChildFragments()
            }
        }

        viewLifecycleOwner.collectFlow(inMeetingViewModel.showReconnectingBanner) { shouldBeShown ->
            bannerInfo?.apply {
                alpha = 1f
                isVisible = shouldBeShown
                if (shouldBeShown) {
                    setBackgroundColor(
                        ContextCompat.getColor(
                            MegaApplication.getInstance().applicationContext,
                            android.R.color.transparent
                        )
                    )
                    text =
                        getString(R.string.reconnecting_message)
                    startAnimation(blink)
                } else {
                    clearAnimation()
                    inMeetingViewModel.checkBannerInfo()
                }
            }
        }
        viewLifecycleOwner.collectFlow(sharedModel.state.map { it.userAvatarUpdateId }
            .distinctUntilChanged()) { peerId ->
            peerId?.let {
                updateParticipantInfo(peerId, AVATAR_CHANGE)
            }
        }
        viewLifecycleOwner.collectFlow(inMeetingViewModel.state.map { it.userAvatarUpdateId }
            .distinctUntilChanged()) { peerId ->
            peerId?.let {
                updateParticipantInfo(peerId, AVATAR_CHANGE)
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
        inMeetingViewModel.getCall()?.run {
            if (inMeetingViewModel.state.value.hasLocalAudio) {
                sharedModel.micInitiallyOn()
            }

            if (inMeetingViewModel.state.value.hasLocalVideo) {
                sharedModel.camInitiallyOn()
            }
            return
        }
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

    /**
     * This method is triggered when the Picture-in-Picture mode is changed.
     */
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        if (inMeetingViewModel.state.value.isPictureInPictureFeatureFlagEnabled) {
            inMeetingViewModel.updateIsInPipMode(isInPipMode = isInPictureInPictureMode)
        }
    }

    fun onPageClick() {
        Timber.d("onPageClick")
        // If the tips is showing or bottom is fully expanded, can not hide the toolbar and panel
        if (bottomFloatingPanelViewHolder?.isPopWindowShowing() == true
            || bottomFloatingPanelViewHolder?.getState() == BottomSheetBehavior.STATE_EXPANDED
        ) return

        // Prevent fast tapping.
        if (System.currentTimeMillis() - lastTouch < TAP_THRESHOLD) return

        Timber.d("banner should be shown $bannerShouldBeShown")

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
        floatingWindowContainer.post {
            adjustPositionOfFloatingWindow(bTop = true, bBottom = true)
        }

        setRecIndicatorVisibility()

        lastTouch = System.currentTimeMillis()
    }

    /**
     * Method to control the position of the floating window in relation to the configuration change
     *
     * @param outMetrics display metrics
     */
    private fun onConfigurationChangedOfFloatingWindow(outMetrics: DisplayMetrics) {
        val currentOrientation =
            resources.configuration.orientation
        if (orientation == currentOrientation) return
        updateCurrentOrientation()
        inMeetingViewModel.state.value.run {
            Timber.d("onConfigurationChangedOfFloatingWindow with $outMetrics isInPip $isInPipMode")
            floatingWindowContainer.post {
                previousY = -1f
                val dx = outMetrics.widthPixels - floatingWindowContainer.width
                var dy = outMetrics.heightPixels - floatingWindowContainer.height

                if (BottomSheetBehavior.STATE_COLLAPSED == bottomFloatingPanelViewHolder?.getState() && floatingBottomSheet.isVisible) {
                    dy = floatingBottomSheet.top - floatingWindowContainer.height
                }
                Timber.d("Moved X: $dx, Y: $dy")
                floatingWindowContainer.moveX(dx.toFloat())
                floatingWindowContainer.moveY(dy.toFloat())
            }
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
        if (inMeetingViewModel.state.value.isInPipMode) return
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
                if (bottomFloatingPanelViewHolder?.getState() == BottomSheetBehavior.STATE_EXPANDED) {
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
            if (inMeetingViewModel.amIAloneOnTheCall(it.chatId)) {
                Timber.d("One to one call. Waiting for connection")
                waitingForConnection(it.chatId)
            } else {
                inMeetingViewModel.getSessionOneToOneCall()?.run {
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
            initGroupCall(call.chatId)
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
        removePictureInPictureFragment()
    }

    private fun removePictureInPictureFragment() {
        inMeetingViewModel.state.value.run {
            Timber.d("Remove Picture Call Fragment isInPipMode = $isInPipMode")
            if (isPictureInPictureFeatureFlagEnabled && !isInPipMode) {
                pictureCallFragment?.let {
                    if (isAdded) {
                        Timber.d("Removing Pip Fragment")
                        removeChildFragment(it)
                        pictureCallFragment = null
                    }
                }
            }
        }
    }

    /**
     * Show reconnecting UI
     */
    private fun reconnecting() {
        Timber.d("Show reconnecting UI, the current status is ${inMeetingViewModel.state.value.callUIStatus}")
        if (inMeetingViewModel.state.value.callUIStatus == CallUIStatusType.None)
            return

        inMeetingViewModel.setStatus(newStatus = CallUIStatusType.None)

        removeListenersAndFragments()
        binding.reconnecting.isVisible = true
    }

    /**
     * Remove fragments
     */
    fun removeUI() {
        Timber.d("Removing call UI, the current status is ${inMeetingViewModel.state.value.callUIStatus}")
        if (inMeetingViewModel.state.value.callUIStatus == CallUIStatusType.None)
            return

        inMeetingViewModel.setStatus(newStatus = CallUIStatusType.None)

        removeListenersAndFragments()
    }

    private fun checkMenuItemsVisibility() {
        checkGridSpeakerViewMenuItemVisibility()
    }

    /**
     * hide views that are not necessary for Picture in Picture Mode
     * show views when not in Picture in Picture Mode
     */
    private fun hideOrShowHelperViews(shouldHide: Boolean) {
        val visibility = if (shouldHide) View.GONE else View.VISIBLE
        binding.selfFeedFloatingWindowContainer.visibility = visibility
        binding.meetingContainer.visibility = visibility
        binding.hostLeaveCallDialogComposeView.visibility = visibility
        if (shouldHide.not()) {
            binding.hostLeaveCallDialogComposeView.bringToFront()
        }
        binding.snackbarComposeView.visibility = visibility
        binding.moreOptionsListComposeView.visibility = visibility
        toolbar.visibility = visibility
        binding.bottomFloatingPanel.root.visibility = visibility
        if (shouldHide.not()) {
            removePictureInPictureFragment()
        }
    }

    /**
     * Control the UI of the call, whether one-to-one or meeting
     */
    private fun checkChildFragments() {
        if (inMeetingViewModel.state.value.isPictureInPictureFeatureFlagEnabled && inMeetingViewModel.state.value.isInPipMode) {
            if (pictureCallFragment == null) {
                removeListenersAndFragments()
                pictureCallFragment = PictureInPictureCallFragment.newInstance().apply {
                    loadChildFragment(R.id.pip_container, this, PictureInPictureCallFragment.TAG)
                }
            }
            hideOrShowHelperViews(shouldHide = true)
        } else {
            if (inMeetingViewModel.state.value.isPictureInPictureFeatureFlagEnabled) {
                hideOrShowHelperViews(shouldHide = false)
            }
            inMeetingViewModel.state.value.call?.apply {
                binding.reconnecting.isVisible = false
                when (status) {
                    ChatCallStatus.Connecting, ChatCallStatus.Joining -> {
                        waitingForConnection(chatId)
                    }

                    ChatCallStatus.InProgress -> {
                        when (inMeetingViewModel.isOneToOneCall()) {
                            null -> {}
                            true -> {
                                Timber.d("One to one call")
                                updateOneToOneUI()
                            }

                            false -> {
                                Timber.d("Group call")
                                updateGroupUI()
                            }
                        }
                    }

                    else -> {}
                }
            }
        }
    }

    /**
     * Show one to one call UI
     */
    private fun initOneToOneCall() {
        if (inMeetingViewModel.state.value.callUIStatus == CallUIStatusType.OneToOne && pictureCallFragment == null) return

        removeListenersAndFragments()

        inMeetingViewModel.getCall()?.apply {
            inMeetingViewModel.getSessionOneToOneCall()?.let { userSession ->
                Timber.d("Show one to one call UI")
                inMeetingViewModel.setStatus(newStatus = CallUIStatusType.OneToOne)

                Timber.d("Create fragment")
                individualCallFragment = IndividualCallFragment.newInstance(
                    chatId,
                    userSession.peerId,
                    userSession.clientId
                )

                individualCallFragment?.let {
                    loadChildFragment(
                        R.id.meeting_container,
                        it,
                        IndividualCallFragment.TAG
                    )
                }

                initLocal(chatId)
                Timber.d("Floating BottomSheet visible ${floatingBottomSheet.isVisible}")
            }
        }
    }

    /**
     * Method to display the waiting for connection UI
     *
     * @param chatId ID of chat
     */
    private fun waitingForConnection(chatId: Long) {
        if (inMeetingViewModel.state.value.callUIStatus == CallUIStatusType.WaitingConnection && pictureCallFragment == null) return

        Timber.d("Show waiting for connection call UI")
        inMeetingViewModel.setStatus(newStatus = CallUIStatusType.WaitingConnection)

        if (inMeetingViewModel.state.value.isInPipMode) {
            // todo set current user's profile active
        } else {
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
        if (inMeetingViewModel.state.value.callUIStatus == CallUIStatusType.SpeakerView) return

        Timber.d("Show group call - Speaker View UI")
        inMeetingViewModel.setStatus(newStatus = CallUIStatusType.SpeakerView)
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
        if (inMeetingViewModel.state.value.callUIStatus == CallUIStatusType.GridView) return

        Timber.d("Show group call - Grid View UI")
        inMeetingViewModel.setStatus(newStatus = CallUIStatusType.GridView)

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
        if (inMeetingViewModel.state.value.callUIStatus != CallUIStatusType.GridView && inMeetingViewModel.state.value.callUIStatus != CallUIStatusType.SpeakerView) {
            individualCallFragment?.let {
                if (it.isAdded) {
                    it.removeChatVideoListener()
                    removeChildFragment(it)
                    individualCallFragment = null
                }
            }
        }

        initLocal(chatId)
        inMeetingViewModel.enableAudioLevelMonitor(chatId)

        if (isParticipantSharingScreen) {
            changeToSpeakerView()
        } else {
            when {
                !isManualModeView -> {
                    inMeetingViewModel.getCall()?.apply {
                        sessionsClientId?.let {
                            when {
                                it.size <= MAX_PARTICIPANTS_GRID_VIEW_AUTOMATIC -> {
                                    Timber.d("Automatic mode - Grid view")
                                    initGridViewMode()
                                }

                                else -> {
                                    Timber.d("Automatic mode - Speaker view")
                                    initSpeakerViewMode()
                                }
                            }
                        }
                    }
                }

                inMeetingViewModel.state.value.callUIStatus == CallUIStatusType.SpeakerView -> {
                    Timber.d("Manual mode - Speaker view")
                    changeToSpeakerView()
                }

                else -> {
                    Timber.d("Manual mode - Grid view")
                    changeToGridView()
                }
            }
        }
    }

    /**
     * Method controlling whether to initiate a call
     */
    private fun initStartMeeting() {
        if (sharedModel.state.value.chatId == MEGACHAT_INVALID_HANDLE) {
            val nameChosen: String = sharedModel.state.value.meetingName
            nameChosen.takeIf { it.isNotBlank() }?.let {
                inMeetingViewModel.setTitleChat(it)
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
        ).commitAllowingStateLoss()
    }

    private fun removeChildFragment(fragment: Fragment?) {
        fragment?.let {
            childFragmentManager.beginTransaction().remove(it).commitAllowingStateLoss()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.in_meeting_fragment_menu, menu)
        speakerViewMenuItem = menu.findItem(R.id.speaker_view)
        gridViewMenuItem = menu.findItem(R.id.grid_view)
        swapCameraMenuItem = menu.findItem(R.id.swap_camera)
        swapCameraMenuItem?.isVisible = inMeetingViewModel.state.value.shouldShowSwapCamera
        checkMenuItemsVisibility()
    }

    /**
     * Method to show or hide the buttons change to grid/speaker view
     */
    private fun checkGridSpeakerViewMenuItemVisibility() {
        inMeetingViewModel.getCall()?.let { call ->
            if (call.status == ChatCallStatus.Connecting || inMeetingViewModel.showOnlyMeBanner.value || inMeetingViewModel.showWaitingForOthersBanner.value) {
                gridViewMenuItem?.apply { isVisible = false }
                speakerViewMenuItem?.apply { isVisible = false }
                return
            }
        }

        when (inMeetingViewModel.state.value.callUIStatus) {
            CallUIStatusType.GridView -> {
                gridViewMenuItem?.apply { isVisible = false }
                speakerViewMenuItem?.apply {
                    isVisible = true
                    isEnabled = !isParticipantSharingScreen
                    iconTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(
                            requireContext(),
                            if (isParticipantSharingScreen) R.color.white_alpha_038 else R.color.white
                        )
                    )
                }
            }

            CallUIStatusType.SpeakerView -> {
                gridViewMenuItem?.apply {
                    isVisible = true
                    isEnabled = !isParticipantSharingScreen
                    iconTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(
                            requireContext(),
                            if (isParticipantSharingScreen) R.color.white_alpha_038 else R.color.white
                        )
                    )
                }
                speakerViewMenuItem?.apply { isVisible = false }
            }

            else -> {
                gridViewMenuItem?.apply { isVisible = false }
                speakerViewMenuItem?.apply { isVisible = false }
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
                changeToGridView()
                true
            }

            R.id.speaker_view -> {
                Timber.d("Change to speaker view.")
                changeToSpeakerView()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun showSnackbar(type: Int, content: String?, chatId: Long) {
        val anchor =
            if (BottomSheetBehavior.STATE_COLLAPSED == bottomFloatingPanelViewHolder?.getState() && floatingBottomSheet.isVisible) {
                binding.snackbarPosition
            } else null

        meetingActivity.showSnackbarWithAnchorView(
            type,
            binding.root,
            anchor,
            content,
            chatId,
            true
        )
    }

    override fun showSnackbar(type: Int, content: String, action: () -> Unit) {
        val anchor =
            if (BottomSheetBehavior.STATE_COLLAPSED == bottomFloatingPanelViewHolder?.getState() && floatingBottomSheet.isVisible) {
                binding.snackbarPosition
            } else null

        meetingActivity.showSnackbar(
            type = type,
            view = binding.root,
            anchor = anchor,
            s = content,
            forceDarkMode = true,
            action = action,
        )
    }

    private fun showRequestPermissionSnackBar() {
        val warningText =
            getString(R.string.meeting_required_permissions_warning)
        showSnackbar(PERMISSIONS_TYPE, warningText, MEGACHAT_INVALID_HANDLE)
    }

    /**
     * Init Floating Panel
     */
    private fun initFloatingPanel() {
        bottomFloatingPanelViewHolder =
            BottomFloatingPanelViewHolder(
                inMeetingViewModel,
                sharedModel,
                sharedWaitingRoomManagementViewModel,
                binding,
                this,
                resources.displayMetrics
            )

        shouldExpandBottomPanel =
            arguments?.getBoolean(MeetingActivity.MEETING_BOTTOM_PANEL_EXPANDED) ?: false

        bottomFloatingPanelViewHolder?.updateWidth(
            meetingActivity.resources.configuration.orientation,
            meetingActivity.resources.displayMetrics.widthPixels
        )

        //Observer the participant List
        inMeetingViewModel.participants.observe(viewLifecycleOwner) { participants ->
            participants?.let {
                bottomFloatingPanelViewHolder?.setParticipantsPanel(
                    it.toMutableList(),
                    inMeetingViewModel.getMyOwnInfo(
                        sharedModel.state.value.micEnabled,
                        sharedModel.state.value.camEnabled,
                    )
                )
            }
        }

        bottomFloatingPanelViewHolder?.propertyUpdaters?.add {
            toolbar.alpha = 1 - it
            // When the bottom on the top, will set the toolbar invisible, otherwise will cover the scroll event of the panel
            toolbar.isVisible = it != 1.0f

            bannerMuteLayout.apply {
                if (isVisible) {
                    alpha = 1 - it
                }
            }

            meetingActivity.binding.callBannerCompose.apply {
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
        bottomFloatingPanelViewHolder?.updateMicIcon(isMicOn)
        updateParticipantsBottomPanel()
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
                it.setBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.dark_grey_alpha_070
                    )
                )
                Timber.d("Show banner info")
                it.isVisible = true
            } else {
                Timber.d("Hide banner info")
                it.isVisible = false
            }

            if (it.isVisible && (bottomFloatingPanelViewHolder?.getState() == BottomSheetBehavior.STATE_EXPANDED || !toolbar.isVisible)) {
                it.alpha = 0f
            }

            floatingWindowContainer.post {
                adjustPositionOfFloatingWindow(bTop = true, bBottom = false)
            }
        }
    }

    /**
     * Method that controls whether the call has been put on or taken off hold, and updates the UI.
     */
    private fun isCallOnHold(isHold: Boolean) {
        Timber.d("Changes in the on hold status of the call")

        showMuteBanner()
        if (inMeetingViewModel.isOneToOneCall() == false) {
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
        if (sharedModel.state.value.camEnabled) {
            sharedModel.clickCamera(false)
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
            call != null && (call.status == ChatCallStatus.InProgress || call.status == ChatCallStatus.Joining) && isCamOn

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
        Timber.d("Update Local Video $isCamOn")
        inMeetingViewModel.getCall()?.apply {
            val isVideoOn: Boolean = inMeetingViewModel.state.value.hasLocalVideo
            if (!MegaApplication.getChatManagement().isInTemporaryState) {
                MegaApplication.getChatManagement().setVideoStatus(chatId, isVideoOn)
            }
        }

        bottomFloatingPanelViewHolder?.updateCamIcon(isCamOn)
        updateParticipantsBottomPanel()
        controlVideoLocalOneToOneCall(isCamOn)
    }

    /*
     * Method for updating the list of participants in the bottom panel
     */
    fun updateParticipantsBottomPanel() {
        inMeetingViewModel.participants.value?.let { participants ->
            bottomFloatingPanelViewHolder?.updateParticipants(
                participants.toMutableList(),
                inMeetingViewModel.getMyOwnInfo(
                    sharedModel.state.value.micEnabled,
                    sharedModel.state.value.camEnabled
                )
            )
        }
    }

    /**
     * Method that checks if the session's on hold state has changed and updates the UI
     *
     * @param session The session of a participant
     */
    private fun updateOnHoldRemote(session: ChatSession) {
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
        session: ChatSession,
        isAudioChange: Boolean,
        isVideoChange: Boolean,
        isPresentingChange: Boolean,
    ) {
        Timber.d("Remote changes detected")

        gridViewCallFragment?.let {
            if (it.isAdded) {
                if (isAudioChange) {
                    it.updateRemoteAudioVideo(TypeRemoteAVFlagChange.Audio, session)
                }
                if (isVideoChange) {
                    Timber.d("Remote AVFlag")
                    it.updateRemoteAudioVideo(TypeRemoteAVFlagChange.Video, session)
                }
            }
        }

        speakerViewCallFragment?.let {
            if (it.isAdded) {
                if (isAudioChange) {
                    it.updateRemoteAudioVideo(TypeRemoteAVFlagChange.Audio, session)
                }
                if (isVideoChange) {
                    it.updateRemoteAudioVideo(TypeRemoteAVFlagChange.Video, session)
                }
                if (isPresentingChange) {
                    it.updateRemoteAudioVideo(TypeRemoteAVFlagChange.ScreenSharing, session)
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
    private fun participantAddedOrLeftMeeting(isAdded: Boolean, position: Int) {
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
        val listParticipants =
            inMeetingViewModel.updateParticipantsNameOrAvatar(peerId, type)
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
    private fun updateSpeaker(device: AudioDevice) {
        bottomFloatingPanelViewHolder?.updateSpeakerIcon(device)
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
            bottomFloatingPanelViewHolder?.getState() == BottomSheetBehavior.STATE_EXPANDED
        isWaitingForMakeModerator = isPanelExpanded

        when {
            !isPanelExpanded && shouldExpandBottomPanel -> bottomFloatingPanelViewHolder?.expand()
            isPanelExpanded && !shouldExpandBottomPanel -> bottomFloatingPanelViewHolder?.collapse()
        }
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
     * Open invite participant page
     */
    @Suppress("deprecation")
    override fun onInviteParticipants() {
        Timber.d("chooseAddContactDialog")
        val contacts = megaApi.contacts
        if (contacts.isNullOrEmpty() || !contacts.any { it.visibility == VISIBILITY_VISIBLE }) {
            val dialog = AddParticipantsNoContactsDialogFragment.newInstance()
            dialog.show(childFragmentManager, dialog.tag)
        } else if (ChatUtil.areAllMyContactsChatParticipants(inMeetingViewModel.state.value.currentChatId)) {
            val dialog = AddParticipantsNoContactsLeftToAddDialogFragment.newInstance()
            dialog.show(childFragmentManager, dialog.tag)
        } else {
            val inviteParticipantIntent =
                Intent(meetingActivity, AddContactActivity::class.java).apply {
                    putExtra(INTENT_EXTRA_KEY_CONTACT_TYPE, CONTACT_TYPE_MEGA)
                    putExtra(INTENT_EXTRA_KEY_CHAT, true)
                    putExtra(INTENT_EXTRA_IS_FROM_MEETING, true)
                    putExtra(INTENT_EXTRA_KEY_CHAT_ID, inMeetingViewModel.getChatId())
                    putExtra(
                        INTENT_EXTRA_KEY_MAX_USER,
                        inMeetingViewModel.state.value.call?.callUsersLimit
                    )
                    putExtra(
                        INTENT_EXTRA_KEY_TOOL_BAR_TITLE,
                        getString(R.string.invite_participants)
                    )
                }
            meetingActivity.startActivityForResult(
                inviteParticipantIntent, REQUEST_ADD_PARTICIPANTS
            )
        }
    }

    companion object {
        const val ANIMATION_DURATION: Long = 500

        const val TAP_THRESHOLD: Long = 500

        const val TOOLBAR_DY = 300f

        const val FLOATING_BOTTOM_SHEET_DY = 400f

        const val MAX_PARTICIPANTS_GRID_VIEW_AUTOMATIC = 6

        const val MILLISECONDS_IN_ONE_SECOND: Long = 1000
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
        bottomFloatingPanelViewHolder?.onDestroy()
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
        endMeetingAsModeratorDialog?.dismissAllowingStateLoss()
    }

    /**
     * Method that updates the microphone and camera values
     */
    private fun updateMicAndCam() {
        val camIsEnable = sharedModel.state.value.camEnabled
        val micIsEnable = sharedModel.state.value.micEnabled
        bottomFloatingPanelViewHolder?.updateCamIcon(camIsEnable)
        bottomFloatingPanelViewHolder?.updateMicIcon(micIsEnable)
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
        chatManagement.removeJoiningChatId(chatId)
        chatManagement.removeJoiningChatId(userHandle)
        chatManagement.broadcastJoinedSuccessfully()
        controlWhenJoinedAChat(chatId)
    }

    private fun answerCallAfterJoin() {
        inMeetingViewModel.getCall()?.let {
            isWaitingForAnswerCall = false
            Timber.d("Joined to chat, answer call")
            answerCall()
        } ?: run {
            Timber.d("Call is null")
            isWaitingForAnswerCall = true
        }
    }

    /**
     * Method to answer the call
     */
    private fun answerCall() {
        var audio = sharedModel.state.value.micEnabled
        if (audio) {
            audio =
                PermissionUtils.hasPermissions(requireContext(), Manifest.permission.RECORD_AUDIO)
        }

        var video = sharedModel.state.value.camEnabled
        if (video) {
            video = PermissionUtils.hasPermissions(requireContext(), Manifest.permission.CAMERA)
        }

        sharedModel.answerCall(
            chatId = inMeetingViewModel.getChatId(),
            enableVideo = video,
            enableAudio = audio,
            speakerAudio = speakerIsEnable
        )

    }

    /**
     * Method to start the call
     */
    private fun startCall() {
        var audio = sharedModel.state.value.micEnabled
        if (audio) {
            audio =
                PermissionUtils.hasPermissions(requireContext(), Manifest.permission.RECORD_AUDIO)
        }

        var video = sharedModel.state.value.camEnabled
        if (video) {
            video = PermissionUtils.hasPermissions(requireContext(), Manifest.permission.CAMERA)
        }

        sharedModel.startOrCreateMeeting(
            title = inMeetingViewModel.state.value.chatTitle,
            video = video,
            audio = audio,
        )
    }

    override fun onErrorJoinedChat(chatId: Long, userHandle: Long, error: Int) {
        Timber.d("Error joining the meeting so close it, error code is $error")
        chatManagement.removeJoiningChatId(chatId)
        chatManagement.removeJoiningChatId(userHandle)
        finishActivity()
    }

    /**
     * The dialog for alerting the meeting is failed to created
     */
    private fun showMeetingFailedDialog() {
        failedDialog = MaterialAlertDialogBuilder(
            requireContext(),
            R.style.ThemeOverlay_Mega_MaterialAlertDialog
        ).setMessage(getString(R.string.meeting_is_failed_content))
            .setCancelable(false)
            .setPositiveButton(sharedResR.string.general_ok) { _, _ ->
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
     * Dialogue displayed when you are left alone in the group call or meeting and you can stay on the call or end it
     */
    private fun showOnlyMeInTheCallDialog() {
        if (MegaApplication.getChatManagement().hasEndCallDialogBeenIgnored) {
            dismissDialog(onlyMeDialog)
            return
        }

        onlyMeDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.calls_call_screen_dialog_title_only_you_in_the_call))
            .setMessage(getString(R.string.calls_call_screen_dialog_description_only_you_in_the_call))
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
     * Method to show call will end banner and only me dialog
     */
    private fun showCallWillEndBannerAndOnlyMeDialog() {
        inMeetingViewModel.startCounterTimerAfterBanner()
        val currentTime =
            MegaApplication.getChatManagement().millisecondsOnlyMeInCallDialog
        inMeetingViewModel.showOnlyMeEndCallTimer(
            if (currentTime > 0) currentTime else TimeUnit.SECONDS.toMillis(
                SECONDS_TO_WAIT_ALONE_ON_THE_CALL
            )
        )
        changeToGridView()
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
        inMeetingViewModel.hideOnlyMeEndCallTimer()
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
