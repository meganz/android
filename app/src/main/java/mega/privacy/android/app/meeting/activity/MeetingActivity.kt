package mega.privacy.android.app.meeting.activity

import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.KeyEvent
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavGraph
import androidx.navigation.fragment.NavHostFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.databinding.ActivityMeetingBinding
import mega.privacy.android.app.meeting.CallNotificationIntentService
import mega.privacy.android.app.meeting.fragments.CreateMeetingFragment
import mega.privacy.android.app.meeting.fragments.InMeetingFragment
import mega.privacy.android.app.meeting.fragments.JoinMeetingAsGuestFragment
import mega.privacy.android.app.meeting.fragments.JoinMeetingFragment
import mega.privacy.android.app.meeting.fragments.MakeModeratorFragment
import mega.privacy.android.app.meeting.fragments.MeetingBaseFragment
import mega.privacy.android.app.meeting.gateway.RTCAudioManagerGateway
import mega.privacy.android.app.myAccount.MyAccountActivity
import mega.privacy.android.app.presentation.contactinfo.ContactInfoActivity
import mega.privacy.android.app.presentation.extensions.changeStatusBarColor
import mega.privacy.android.app.presentation.meeting.CallRecordingViewModel
import mega.privacy.android.app.presentation.meeting.WaitingRoomManagementViewModel
import mega.privacy.android.app.presentation.meeting.model.CallRecordingUIState
import mega.privacy.android.app.presentation.meeting.model.MeetingState
import mega.privacy.android.app.presentation.meeting.model.WaitingRoomManagementState
import mega.privacy.android.app.presentation.meeting.view.ParticipantsFullListView
import mega.privacy.android.app.presentation.meeting.view.dialog.CallRecordingConsentDialog
import mega.privacy.android.app.presentation.meeting.view.dialog.DenyEntryToCallDialog
import mega.privacy.android.app.presentation.meeting.view.dialog.UsersInWaitingRoomDialog
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.REQUIRE_PASSCODE_INVALID
import mega.privacy.android.app.utils.ScheduledMeetingDateUtil.getAppropriateStringForScheduledMeetingDate
import mega.privacy.android.domain.entity.chat.ChatScheduledMeeting
import mega.privacy.android.domain.entity.meeting.ParticipantsSection
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MeetingActivity : PasscodeActivity() {

    companion object {
        /** The name of actions denoting set
        JOIN/CREATE/JOIN AS GUEST/In-meeting screen as the initial screen */
        const val MEETING_ACTION_JOIN = "join_meeting"
        const val MEETING_ACTION_REJOIN = "rejoin_meeting"
        const val MEETING_ACTION_CREATE = "create_meeting"
        const val MEETING_ACTION_GUEST = "join_meeting_as_guest"
        const val MEETING_ACTION_IN = "in_meeting"
        const val MEETING_ACTION_MAKE_MODERATOR = "make_moderator"
        const val MEETING_ACTION_RINGING = "ringing_meeting"
        const val MEETING_ACTION_RINGING_VIDEO_ON = "ringing_meeting_video_on"
        const val MEETING_ACTION_RINGING_VIDEO_OFF = "ringing_meeting_video_off"
        const val MEETING_ACTION_START = "start_meeting"

        /** The names of the Extra data being passed to the initial fragment */
        const val MEETING_NAME = "meeting_name"
        const val MEETING_LINK = "meeting_link"
        const val MEETING_CHAT_ID = "chat_id"
        const val MEETING_PUBLIC_CHAT_HANDLE = "public_chat_handle"
        const val MEETING_AUDIO_ENABLE = "audio_enable"
        const val MEETING_VIDEO_ENABLE = "video_enable"
        const val MEETING_SPEAKER_ENABLE = "speaker_enable"
        const val MEETING_IS_GUEST = "is_guest"
        const val MEETING_GUEST_FIRST_NAME = "guest_first_name"
        const val MEETING_GUEST_LAST_NAME = "guest_last_name"
        const val CALL_ACTION = "call_action"
        const val MEETING_BOTTOM_PANEL_EXPANDED = "meeting_bottom_panel_expanded"
        const val MEETING_IS_RINGIN_ALL = "meeting_is_ringing_all"
        const val MEETING_FREE_PLAN_USERS_LIMIT = "meeting_free_plan_users_limit"
        const val MEETING_PARTICIPANTS_LIMIT = "meeting_participants_limit"

        fun getIntentOngoingCall(context: Context, chatId: Long): Intent {
            return Intent(context, MeetingActivity::class.java).apply {
                putExtra(MEETING_CHAT_ID, chatId)
                action = MEETING_ACTION_IN
            }
        }
    }

    @Inject
    lateinit var notificationManager: NotificationManagerCompat

    @Inject
    lateinit var navigator: MegaNavigator

    /**
     * Rtc audio manager gateway
     */
    @Inject
    lateinit var rtcAudioManagerGateway: RTCAudioManagerGateway

    lateinit var binding: ActivityMeetingBinding
    private lateinit var pipBuilderParams: PictureInPictureParams.Builder
    private val meetingViewModel: MeetingActivityViewModel by viewModels()
    private val waitingRoomManagementViewModel: WaitingRoomManagementViewModel by viewModels()
    private val callRecordingViewModel: CallRecordingViewModel by viewModels()

    private var meetingAction: String? = null

    private var isGuest = false
    private var isLockingEnabled = false

    private var navController: NavController? = null
    private var navGraph: NavGraph? = null

    private val destinationChangedListener: NavController.OnDestinationChangedListener by lazy {
        NavController.OnDestinationChangedListener { _, destination, _ ->
            updateAutoPiPModeParams(isAutoEnterEnabled = destination.id == R.id.inMeetingFragment)
            binding.bannerMute.elevation = when (destination.id) {
                R.id.makeModeratorFragment -> 0f
                else -> 1f
            }
        }
    }

    /**
     * Handle events when a Back Press is detected
     */
    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            val currentFragment = getCurrentFragment()

            when (currentFragment) {
                is CreateMeetingFragment -> {
                    currentFragment.releaseVideoAndHideKeyboard()
                    removeRTCAudioManager()
                }

                is JoinMeetingFragment -> {
                    currentFragment.releaseVideoDeviceAndRemoveChatVideoListener()
                    removeRTCAudioManager()
                }

                is JoinMeetingAsGuestFragment -> {
                    currentFragment.releaseVideoAndHideKeyboard()
                    removeRTCAudioManager()
                }

                is InMeetingFragment -> {
                    // Prevent guest from quitting the call by pressing back
                    if (!isGuest) {
                        if (enterPipModeIfPossible()) return
                        currentFragment.removeUI()
                    }
                }

                is MakeModeratorFragment -> {
                    currentFragment.cancel()
                }
            }

            if (currentFragment !is MakeModeratorFragment && (currentFragment !is InMeetingFragment || !isGuest)) {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    private fun initializePictureInPictureParams() {
        if (isSystemPipEnabledAndAvailable()) {
            pipBuilderParams = PictureInPictureParams.Builder()
            pipBuilderParams.setAspectRatio(Rational(PIP_WIDTH_RATIO, PIP_HEIGHT_RATIO))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                pipBuilderParams.setSeamlessResizeEnabled(false)
            }
        }
    }

    private fun updateAutoPiPModeParams(isAutoEnterEnabled: Boolean) {
        if (meetingViewModel.state.value.isPictureInPictureFeatureFlagEnabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                pipBuilderParams.setAutoEnterEnabled(isAutoEnterEnabled)
                setPictureInPictureParams(pipBuilderParams.build())
            }
        }
    }

    /**
     * This method is triggered when the Picture-in-Picture mode is changed.
     */
    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration,
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (meetingViewModel.state.value.isPictureInPictureFeatureFlagEnabled) {
            meetingViewModel.updateIsInPipMode(isInPipMode = isInPictureInPictureMode)
        }
    }

    /**
     * This method is triggered when Home button is pressed.
     * Handle the case when the user leaves the app
     */
    override fun onUserLeaveHint() {
        val currentFragment = getCurrentFragment()
        if (currentFragment is InMeetingFragment) {
            enterPipModeIfPossible()
        }
    }

    private fun enterPipModeIfPossible(): Boolean {
        if (isSystemPipEnabledAndAvailable() && meetingViewModel.state.value.isPictureInPictureFeatureFlagEnabled) {
            return try {
                enterPictureInPictureMode(pipBuilderParams.build())
                true
            } catch (e: Exception) {
                Timber.w("Device lied to us about supporting PiP. $e")
                false
            }
        }
        return false
    }

    private fun isInPipMode(): Boolean {
        return isSystemPipEnabledAndAvailable() && isInPictureInPictureMode
    }


    private fun View.setMarginTop(marginTop: Int) {
        val menuLayoutParams = this.layoutParams as ViewGroup.MarginLayoutParams
        menuLayoutParams.setMargins(0, marginTop, 0, 0)
        this.layoutParams = menuLayoutParams
    }

    override fun onNewIntent(newIntent: Intent) {
        super.onNewIntent(newIntent)
        intent = newIntent

        initIntent()
        initActionBar()
        initNavigation()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializePictureInPictureParams()

        // Setup the Back Press dispatcher to receive Back Press events
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        initIntent()

        binding = ActivityMeetingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initActionBar()
        initNavigation()
        setStatusBarTranslucent()
        binding.waitingRoomDialogComposeView.apply {
            isVisible = true
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                OriginalTempTheme(isDark = true) {
                    UsersInWaitingRoomDialog()
                    DenyEntryToCallDialog()
                }
            }
        }

        binding.waitingRoomListComposeView.apply {
            isVisible = true
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                OriginalTempTheme(isDark = true) {
                    ParticipantsFullListView(
                        onScrollChange = { scrolled ->
                            this@MeetingActivity.changeStatusBarColor(
                                scrolled,
                                true
                            )
                        },
                        onEditProfileClicked = { editProfile() },
                        onContactInfoClicked = { email -> openContactInfo(email) },
                    )
                }
            }
        }

        binding.callRecordingConsentDialogComposeView.apply {
            isVisible = true
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                OriginalTempTheme(isDark = true) {
                    CallRecordingConsentDialog()
                }
            }
        }

        collectFlows()

        collectFlow(meetingViewModel.finishMeetingActivity) { shouldFinish ->
            if (shouldFinish) {
                if (meetingViewModel.amIAGuest()) {
                    meetingViewModel.logout()
                } else {
                    finish()
                }
            }
        }

        collectFlow(meetingViewModel.switchCall) { chatId ->
            if (chatId != MEGACHAT_INVALID_HANDLE && meetingViewModel.state.value.chatId != chatId) {
                Timber.d("Switch call")
                passcodeManagement.showPasscodeScreen = true
                MegaApplication.getInstance().openCallService(chatId)
                startActivity(getIntentOngoingCall(this@MeetingActivity, chatId))
            }
        }

    }

    private fun collectFlows() {
        collectFlow(meetingViewModel.state) { state: MeetingState ->

            if (state.shouldLaunchLeftMeetingActivity) {
                startActivity(
                    Intent(this, LeftMeetingActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        .putExtra(MEETING_FREE_PLAN_USERS_LIMIT, state.callEndedDueToFreePlanLimits)
                        .putExtra(
                            MEETING_PARTICIPANTS_LIMIT,
                            state.callEndedDueToTooManyParticipants
                        )
                )
                finish()
            }

            if (state.callEndedDueToFreePlanLimits) {
                finish()
            }

            if (state.chatId != -1L) {
                waitingRoomManagementViewModel.setChatIdCallOpened(state.chatId)
            }
            waitingRoomManagementViewModel.setWaitingRoomSectionOpened(state.isWaitingRoomOpened())

            if (state.shouldShareMeetingLink && state.meetingLink.isNotEmpty()) {
                meetingViewModel.onConsumeShouldShareMeetingLinkEvent()
                shareLink(
                    state.meetingLink,
                    state.title,
                    state.chatScheduledMeeting,
                    state.myFullName
                )
            }

            if (state.chatIdToOpen != -1L) {
                navigator.openChat(
                    context = this,
                    chatId = state.chatIdToOpen,
                    action = Constants.ACTION_CHAT_SHOW_MESSAGES
                )
                meetingViewModel.onConsumeNavigateToChatEvent()
            }

            if (state.isNecessaryToUpdateCall) {
                meetingViewModel.getChatCall(false)
            }
        }

        collectFlow(callRecordingViewModel.state) { state: CallRecordingUIState ->
            binding.recIndicator.visibility =
                if (state.isSessionOnRecording) View.VISIBLE else View.GONE
        }

        collectFlow(waitingRoomManagementViewModel.state) { state: WaitingRoomManagementState ->
            state.snackbarString?.let {
                showSnackbar(it)
                waitingRoomManagementViewModel.onConsumeSnackBarMessageEvent()
            }
        }
    }

    override fun onDestroy() {
        navController?.removeOnDestinationChangedListener(destinationChangedListener)

        super.onDestroy()
    }

    private fun initIntent() {
        intent?.let {
            val chatId = it.getLongExtra(MEETING_CHAT_ID, MEGACHAT_INVALID_HANDLE).let { chatId ->
                meetingViewModel.updateChatRoomId(chatId)
                chatId
            }

            if (it.action == CallNotificationIntentService.ANSWER) {
                it.extras?.let { extra ->
                    val chatIdIncomingCall = extra.getLong(
                        Constants.CHAT_ID_OF_INCOMING_CALL,
                        MEGACHAT_INVALID_HANDLE
                    )

                    val answerIntent = Intent(this, CallNotificationIntentService::class.java)
                    answerIntent.putExtra(
                        Constants.CHAT_ID_OF_CURRENT_CALL,
                        MEGACHAT_INVALID_HANDLE
                    )
                    answerIntent.putExtra(Constants.CHAT_ID_OF_INCOMING_CALL, chatIdIncomingCall)
                    answerIntent.action = it.action
                    startService(answerIntent)
                    finish()
                    return
                }
            }

            if (it.action == CallNotificationIntentService.START_SCHED_MEET) {
                it.extras?.let { extra ->
                    val chatIdIncomingCall = extra.getLong(
                        Constants.CHAT_ID_OF_INCOMING_CALL,
                        MEGACHAT_INVALID_HANDLE
                    )

                    val schedIdIncomingCall = extra.getLong(
                        Constants.SCHEDULED_MEETING_ID,
                        MEGACHAT_INVALID_HANDLE
                    )

                    val intent = Intent(this, CallNotificationIntentService::class.java).apply {
                        action = it.action
                        putExtra(Constants.CHAT_ID_OF_CURRENT_CALL, MEGACHAT_INVALID_HANDLE)
                        putExtra(Constants.CHAT_ID_OF_INCOMING_CALL, chatIdIncomingCall)
                        putExtra(Constants.SCHEDULED_MEETING_ID, schedIdIncomingCall)
                    }
                    startService(intent)
                    finish()
                    return
                }
            }

            if (it.getBooleanExtra(MEETING_IS_RINGIN_ALL, false)) {
                meetingViewModel.meetingStartedRingingAll()
            }

            // Cancel current notification if needed
            notificationManager.cancel(chatId.toInt())

            isGuest = it.getBooleanExtra(
                MEETING_IS_GUEST,
                false
            )

            if ((!isGuest && shouldRefreshSessionDueToSDK()) || shouldRefreshSessionDueToKarere()) {
                meetingViewModel.state.value.chatId.let { currentChatId ->
                    if (currentChatId != MEGACHAT_INVALID_HANDLE) {
                        //Notification of this call should be displayed again
                        MegaApplication.getChatManagement().removeNotificationShown(currentChatId)
                    }
                }

                return
            }

            meetingAction = it.action
            meetingViewModel.setAction(meetingAction)
        }
    }

    @Suppress("DEPRECATION")
    private fun setStatusBarTranslucent() {
        val decorView: View = window.decorView

        decorView.setOnApplyWindowInsetsListener { v: View, insets: WindowInsets? ->
            val defaultInsets = v.onApplyWindowInsets(insets)

            binding.toolbar.setMarginTop(defaultInsets.systemWindowInsetTop)

            defaultInsets.replaceSystemWindowInsets(
                defaultInsets.systemWindowInsetLeft,
                0,
                defaultInsets.systemWindowInsetRight,
                defaultInsets.systemWindowInsetBottom
            )
        }

        ViewCompat.requestApplyInsets(decorView)
    }

    /**
     * Initialize Action Bar and set icon according to param
     */
    private fun initActionBar() {
        setSupportActionBar(binding.toolbar)
        val actionBar = supportActionBar ?: return
        actionBar.setHomeButtonEnabled(true)
        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.title = ""

        when (meetingAction) {
            MEETING_ACTION_CREATE, MEETING_ACTION_JOIN, MEETING_ACTION_GUEST -> {
                actionBar.setHomeAsUpIndicator(R.drawable.ic_close_white)
            }

            MEETING_ACTION_IN -> actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white)
        }
    }

    /**
     * Initialize Navigation and set startDestination(initial screen)
     * according to the meeting action
     */
    private fun initNavigation() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        navGraph?.clear()
        navGraph = navHostFragment.navController.navInflater.inflate(R.navigation.meeting)

        // The args to be passed to startDestination
        val bundle = Bundle()
        meetingViewModel.state.value.chatId.let { chatId ->
            if (chatId != -1L) {
                bundle.putLong(
                    MEETING_CHAT_ID,
                    meetingViewModel.state.value.chatId
                )
            }
        }

        bundle.putLong(
            MEETING_PUBLIC_CHAT_HANDLE,
            intent.getLongExtra(MEETING_PUBLIC_CHAT_HANDLE, MEGACHAT_INVALID_HANDLE)
        )

        val shouldExpandPanel = intent.getBooleanExtra(MEETING_BOTTOM_PANEL_EXPANDED, false)
        if (shouldExpandPanel) {
            waitingRoomManagementViewModel.setDialogClosed()
            meetingViewModel.updateParticipantsSection(ParticipantsSection.WaitingRoomSection)
        }

        bundle.putBoolean(
            MEETING_BOTTOM_PANEL_EXPANDED, shouldExpandPanel
        )

        // Pass the meeting data to Join Meeting screen
        if (meetingAction == MEETING_ACTION_GUEST || meetingAction == MEETING_ACTION_JOIN) {
            bundle.putString(MEETING_LINK, intent.dataString)
            bundle.putString(MEETING_NAME, intent.getStringExtra(MEETING_NAME))
            bundle.putString(
                MEETING_GUEST_FIRST_NAME,
                intent.getStringExtra(MEETING_GUEST_FIRST_NAME)
            )
            bundle.putString(
                MEETING_GUEST_LAST_NAME,
                intent.getStringExtra(MEETING_GUEST_LAST_NAME)
            )
        }

        if (meetingAction == MEETING_ACTION_IN) {
            bundle.putBoolean(
                MEETING_AUDIO_ENABLE, intent.getBooleanExtra(
                    MEETING_AUDIO_ENABLE,
                    false
                )
            )
            bundle.putBoolean(
                MEETING_VIDEO_ENABLE, intent.getBooleanExtra(
                    MEETING_VIDEO_ENABLE,
                    false
                )
            )
            bundle.putBoolean(
                MEETING_SPEAKER_ENABLE, intent.getBooleanExtra(
                    MEETING_SPEAKER_ENABLE,
                    false
                )
            )
            bundle.putBoolean(
                MEETING_IS_GUEST,
                isGuest
            )
        }

        navGraph?.apply {
            val startDestination = when (meetingAction) {
                MEETING_ACTION_CREATE -> R.id.createMeetingFragment
                MEETING_ACTION_JOIN, MEETING_ACTION_REJOIN -> R.id.joinMeetingFragment
                MEETING_ACTION_GUEST -> R.id.joinMeetingAsGuestFragment
                MEETING_ACTION_START, MEETING_ACTION_IN -> R.id.inMeetingFragment
                MEETING_ACTION_RINGING -> R.id.ringingMeetingFragment
                MEETING_ACTION_MAKE_MODERATOR -> R.id.makeModeratorFragment
                else -> R.id.createMeetingFragment
            }
            setStartDestination(startDestination)
            navController?.setGraph(this, bundle)
        }

        navController?.addOnDestinationChangedListener(destinationChangedListener)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Get current fragment from navHostFragment
     */
    fun getCurrentFragment(): MeetingBaseFragment? {
        val navHostFragment: Fragment? =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
        return navHostFragment?.childFragmentManager?.fragments?.get(0) as MeetingBaseFragment?
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        meetingViewModel.inviteToChat(this, requestCode, resultCode, intent)
        super.onActivityResult(requestCode, resultCode, intent)
    }

    fun showSnackbar(content: String) {
        showSnackbar(binding.navHostFragment, content)
    }

    override fun onStop() {
        super.onStop()
        val currentFragment = getCurrentFragment()
        if (currentFragment is InMeetingFragment) {
            meetingViewModel.sendEnterCallEvent(false)
        }
    }

    override fun onPause() {
        super.onPause()
        lifecycleScope.launch {
            val timeRequired = passcodeUtil.timeRequiredForPasscode()
            if (timeRequired != REQUIRE_PASSCODE_INVALID) {
                if (isLockingEnabled) {
                    passcodeManagement.lastPause = System.currentTimeMillis() - timeRequired
                } else {
                    passcodeUtil.pauseUpdate()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            isLockingEnabled = passcodeUtil.shouldLock(false)
        }

        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or 0x00000010

        val currentFragment = getCurrentFragment()
        if (currentFragment is InMeetingFragment) {
            meetingViewModel.sendEnterCallEvent(true)
        }
    }

    /**
     * Method for sharing the link
     *
     * @param meetingLink               meeting link
     * @param meetingTitle              meeting title
     * @param chatScheduledMeeting      [ChatScheduledMeeting]
     * @param participantFullName       participant full name
     */
    fun shareLink(
        meetingLink: String,
        meetingTitle: String,
        chatScheduledMeeting: ChatScheduledMeeting?,
        participantFullName: String,
    ) {
        val subject = getString(R.string.meetings_sharing_meeting_link_meeting_invite_subject)
        val title = getString(R.string.meetings_sharing_meeting_link_title, participantFullName)
        val meetingName =
            getString(R.string.meetings_sharing_meeting_link_meeting_name, meetingTitle)
        val meetingLink =
            getString(R.string.meetings_sharing_meeting_link_meeting_link, meetingLink)

        val body = StringBuilder()
        body.append("\n")
            .append(title)
            .append("\n\n")
            .append(meetingName)

        chatScheduledMeeting?.let {
            val meetingDateAndTime = getString(
                R.string.meetings_sharing_meeting_link_meeting_date_and_time,
                getAppropriateStringForScheduledMeetingDate(
                    this@MeetingActivity,
                    meetingViewModel.is24HourFormat,
                    chatScheduledMeeting
                )
            )
            body.append(meetingDateAndTime)
        }

        body.append("\n")
            .append(meetingLink)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = Constants.TYPE_TEXT_PLAIN
            putExtra(Intent.EXTRA_SUBJECT, "\n${subject}")
            putExtra(Intent.EXTRA_TEXT, body.toString())
        }

        startActivity(Intent.createChooser(intent, " "))
    }

    /**
     * Method to remove the RTC Audio Manager when the call has not been finally established
     */
    private fun removeRTCAudioManager() {
        if (!meetingViewModel.isChatCreatedAndIParticipating()) {
            rtcAudioManagerGateway.removeRTCAudioManager()
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        return when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                if (rtcAudioManagerGateway.isAnIncomingCallRinging) {
                    rtcAudioManagerGateway.muteOrUnMute(false)
                }
                false
            }

            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (rtcAudioManagerGateway.isAnIncomingCallRinging) {
                    rtcAudioManagerGateway.muteOrUnMute(true)
                }
                false
            }

            else -> super.dispatchKeyEvent(event)
        }
    }

    /**
     * Open edit profile page
     */
    private fun editProfile() {
        startActivity(Intent(this, MyAccountActivity::class.java))
    }

    /**
     * Open Contact info
     *
     * @param email        User email
     */
    private fun openContactInfo(email: String?) {
        startActivity(Intent(this, ContactInfoActivity::class.java).apply {
            putExtra(
                Constants.NAME,
                email
            )
        })
    }

    private fun isSystemPipEnabledAndAvailable(): Boolean {
        return packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
    }
}

private const val PIP_WIDTH_RATIO = 9
private const val PIP_HEIGHT_RATIO = 16
