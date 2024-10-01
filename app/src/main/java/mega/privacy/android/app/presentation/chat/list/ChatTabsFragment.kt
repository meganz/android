package mega.privacy.android.app.presentation.chat.list

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import androidx.core.view.MenuProvider
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint
import de.palm.composestateevents.EventEffect
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.contract.SendToChatActivityContract
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.main.NavigationDrawerManager
import mega.privacy.android.app.main.dialog.chatstatus.ChatStatusDialogFragment
import mega.privacy.android.app.meeting.activity.MeetingActivity
import mega.privacy.android.app.modalbottomsheet.MeetingBottomSheetDialogFragment
import mega.privacy.android.app.objects.PasscodeManagement
import mega.privacy.android.app.presentation.chat.list.model.ChatTab
import mega.privacy.android.app.presentation.chat.list.view.ChatTabsView
import mega.privacy.android.app.presentation.contact.invite.InviteContactActivity
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.meeting.CreateScheduledMeetingActivity
import mega.privacy.android.app.presentation.meeting.ScheduledMeetingManagementViewModel
import mega.privacy.android.app.presentation.meeting.WaitingRoomActivity
import mega.privacy.android.app.presentation.meeting.model.ShareLinkOption
import mega.privacy.android.app.presentation.startconversation.StartConversationActivity
import mega.privacy.android.app.presentation.startconversation.StartConversationActivity.Companion.EXTRA_NEW_CHAT_ID
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.ChatUtil
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.MenuUtils.setupSearchView
import mega.privacy.android.app.utils.ScheduledMeetingDateUtil
import mega.privacy.android.app.utils.ViewUtils.hideKeyboard
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.chat.ChatRoomItem
import mega.privacy.android.domain.entity.chat.ChatStatus
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.mobile.analytics.event.ChatScreenEvent
import mega.privacy.mobile.analytics.event.ChatTabFABPressedEvent
import mega.privacy.mobile.analytics.event.ChatsTabEvent
import mega.privacy.mobile.analytics.event.InviteFriendsPressedEvent
import mega.privacy.mobile.analytics.event.MeetingsTabEvent
import mega.privacy.mobile.analytics.event.ScheduleMeetingPressedEvent
import mega.privacy.mobile.analytics.event.ScheduledMeetingShareMeetingLinkButtonEvent
import mega.privacy.mobile.analytics.event.SendMeetingLinkToChatScheduledMeetingEvent
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import timber.log.Timber
import javax.inject.Inject

/**
 * Chat tabs fragment containing Chat and Meeting fragment
 */
@AndroidEntryPoint
class ChatTabsFragment : Fragment() {
    @Inject
    lateinit var navigator: MegaNavigator

    companion object {
        private const val EXTRA_SHOW_MEETING_TAB = "EXTRA_SHOW_MEETING_TAB"

        /**
         * Create a new instance of [ChatTabsFragment]
         *
         * @param showMeetingTab    Flag to show Meeting tab as initial tab
         * @return                  New instance of [ChatTabsFragment]
         */
        @JvmStatic
        fun newInstance(showMeetingTab: Boolean): ChatTabsFragment =
            ChatTabsFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(EXTRA_SHOW_MEETING_TAB, showMeetingTab)
                }
            }
    }

    @Inject
    lateinit var getThemeMode: GetThemeMode

    @Inject
    lateinit var passcodeManagement: PasscodeManagement

    private val showMeetingTab by lazy {
        arguments?.getBoolean(EXTRA_SHOW_MEETING_TAB, false) ?: false
    }

    private var actionMode: ActionMode? = null
    private var archivedMenuItem: MenuItem? = null
    private var currentTab: ChatTab = ChatTab.CHATS

    private val viewModel by viewModels<ChatTabsViewModel>()
    private val scheduledMeetingManagementViewModel by viewModels<ScheduledMeetingManagementViewModel>()

    private val bluetoothPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGrant ->
            if (isGrant) {
                openMeetingToCreate()
            } else {
                (activity as? BaseActivity)?.showSnackbar(
                    Constants.PERMISSIONS_TYPE,
                    getString(R.string.meeting_bluetooth_connect_required_permissions_warning),
                    MegaApiJava.INVALID_HANDLE
                )
            }
        }

    private val startConversationLauncher: ActivityResultLauncher<Intent?> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val intentData = result.data
            if (result.resultCode == Activity.RESULT_OK && intentData?.hasExtra(EXTRA_NEW_CHAT_ID) == true) {
                val chatId = intentData.getLongExtra(EXTRA_NEW_CHAT_ID, MEGACHAT_INVALID_HANDLE)
                launchChatScreen(chatId)
            } else {
                Timber.w("StartConversationActivity invalid result: $result")
            }
        }

    private val sendToChatLauncher = registerForActivityResult(SendToChatActivityContract()) {
        if (it != null) {
            scheduledMeetingManagementViewModel.sendToChat(
                data = it,
                link = scheduledMeetingManagementViewModel.state.value.meetingLink
            )
        }
    }

    private val scheduleResultContract =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            Timber.d("Schedule meeting result: ${result.resultCode}")
            if (result.resultCode == Activity.RESULT_OK) {
                val isLinkCreated = result.data?.getBooleanExtra(
                    CreateScheduledMeetingActivity.MEETING_LINK_CREATED_TAG,
                    false
                ) ?: false
                if (isLinkCreated) {
                    // show bottom sheet dialog
                    val chatId = result.data?.getLongExtra(
                        Constants.CHAT_ID,
                        -1L
                    ) ?: -1L
                    if (chatId != -1L) {
                        val link = result.data?.getStringExtra(
                            CreateScheduledMeetingActivity.MEETING_LINK_TAG
                        )
                        val title = result.data?.getStringExtra(
                            CreateScheduledMeetingActivity.MEETING_TITLE_TAG
                        ) ?: ""
                        scheduledMeetingManagementViewModel.setMeetingLink(
                            chatId,
                            link,
                            title
                        )
                    }
                }
            }
        }

    private val drawerListener = object : DrawerLayout.DrawerListener {
        override fun onDrawerOpened(drawerView: View) {
            viewModel.showTooltips(false)
        }

        override fun onDrawerClosed(drawerView: View) {
            viewModel.showTooltips(true)
        }

        override fun onDrawerStateChanged(newState: Int) {}
        override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        ComposeView(requireContext()).apply {
            setContent {
                val mode by getThemeMode().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
                val chatsTabState by viewModel.getState().collectAsStateWithLifecycle()
                val managementState by scheduledMeetingManagementViewModel.state.collectAsStateWithLifecycle()

                EventEffect(
                    managementState.meetingLinkCreated,
                    scheduledMeetingManagementViewModel::onMeetingLinkShareShown
                ) {
                    showMeetingShareLink()
                }

                EventEffect(
                    managementState.meetingLinkAction,
                    scheduledMeetingManagementViewModel::onMeetingLinkShareConsumed
                ) {
                    when (it) {

                        ShareLinkOption.SendLinkToChat -> {
                            Analytics.tracker.trackEvent(SendMeetingLinkToChatScheduledMeetingEvent)
                            sendToChatLauncher.launch(
                                longArrayOf()
                            )
                        }

                        ShareLinkOption.ShareLink -> {
                            Analytics.tracker.trackEvent(ScheduledMeetingShareMeetingLinkButtonEvent)
                            showMeetingShareOptions()
                        }
                    }
                }
                OriginalTempTheme(isDark = mode.isDarkMode()) {
                    ChatTabsView(
                        state = chatsTabState,
                        managementState = managementState,
                        showMeetingTab = showMeetingTab,
                        onTabSelected = ::onTabSelected,
                        onItemClick = ::onItemClick,
                        onItemMoreClick = ::onItemMoreClick,
                        onItemSelected = ::onItemSelected,
                        onResetStateSnackbarMessage = viewModel::onSnackbarMessageConsumed,
                        onResetManagementStateSnackbarMessage = scheduledMeetingManagementViewModel::onSnackbarMessageConsumed,
                        onCancelScheduledMeeting = {
                            scheduledMeetingManagementViewModel.onCancelScheduledMeeting()
                            onDismissDialog()
                        },
                        onDismissDialog = ::onDismissDialog,
                        onStartChatClick = ::startChatAction,
                        onShowNextTooltip = viewModel::setNextMeetingTooltip,
                        onDismissForceAppUpdateDialog = viewModel::onForceUpdateDialogDismissed,
                        onScheduleMeeting = ::onScheduleMeeting
                    )
                }
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewLifecycleOwner.collectFlow(viewModel.getState(), Lifecycle.State.RESUMED) { state ->
            if (state.selectedIds.isNotEmpty()) {
                if (actionMode == null) {
                    actionMode = (activity as? AppCompatActivity?)
                        ?.startSupportActionMode(buildActionMode())
                } else if (state.selectedIds.size.toString() != actionMode?.title) {
                    actionMode?.invalidate()
                }
                actionMode?.title = state.selectedIds.size.toString()
            } else {
                actionMode?.finish()
            }

            when (state.currentChatStatus) {
                ChatStatus.Online -> R.string.online_status
                ChatStatus.Offline -> R.string.offline_status
                ChatStatus.Away -> R.string.away_status
                ChatStatus.Busy -> R.string.busy_status
                ChatStatus.NoNetworkConnection -> R.string.error_server_connection_problem
                ChatStatus.Reconnecting -> R.string.invalid_connection_state
                ChatStatus.Connecting -> R.string.chat_connecting
                else -> null
            }?.let { subtitle ->
                (activity as? AppCompatActivity?)?.supportActionBar?.setSubtitle(subtitle)
            }

            state.currentCallChatId?.let { chatId ->
                launchChatCallScreen(chatId)
                viewModel.removeCurrentCallAndWaitingRoom()
            }

            state.currentWaitingRoom?.let { chatId ->
                launchWaitingRoomScreen(chatId)
                viewModel.removeCurrentCallAndWaitingRoom()
            }

            state.snackBar?.let { snackBar ->
                (activity as? BaseActivity?)?.showSnackbar(
                    snackBar.type,
                    view,
                    snackBar.getMessage(resources),
                    MEGACHAT_INVALID_HANDLE
                )
                viewModel.updateSnackBar(null)
            }

            state.isParticipatingInChatCallResult?.let { isInCall ->
                handleUserInCall(isInCall)
            }
            archivedMenuItem?.isVisible = state.hasArchivedChats
        }
        (activity as? NavigationDrawerManager)?.addDrawerListener(drawerListener)
        view.post {
            (activity as? ManagerActivity?)?.showHideBottomNavigationView(false)
            (activity as? ManagerActivity?)?.invalidateOptionsMenu()
            (activity as? ManagerActivity?)?.findViewById<View>(R.id.toolbar)?.setOnClickListener {
                ChatStatusDialogFragment().show(childFragmentManager, ChatStatusDialogFragment.TAG)
            }
            setupMenu()
        }
    }

    private fun handleUserInCall(isInCall: Boolean) {
        if (isInCall) {
            CallUtil.showConfirmationInACall(
                requireContext(),
                getString(R.string.ongoing_call_content),
                passcodeManagement
            )
        } else {
            if (hasBluetoothPermission()) {
                openMeetingToCreate()
            } else {
                bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
            }
        }
        viewModel.markHandleIsParticipatingInChatCall()
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkHasArchivedChats()
        Analytics.tracker.trackEvent(ChatScreenEvent)
        Firebase.crashlytics.log("Screen: ${ChatScreenEvent.eventName}")
    }

    override fun onStop() {
        viewModel.clearSearchQuery()
        super.onStop()
    }

    override fun onDestroyView() {
        (activity as? ManagerActivity?)?.findViewById<View>(R.id.toolbar)?.setOnClickListener(null)
        scheduledMeetingManagementViewModel.stopMonitoringLoadMessages()
        (activity as? NavigationDrawerManager)?.removeDrawerListener(drawerListener)
        super.onDestroyView()
    }

    private fun setupMenu() {
        activity?.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.fragment_chat_tabs, menu)
                menu.findItem(R.id.menu_search)?.setupSearchView(viewModel::setSearchQuery)
                archivedMenuItem = menu.findItem(R.id.action_menu_archived)?.also {
                    it.isVisible = viewModel.getState().value.hasArchivedChats
                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean = true
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun onItemClick(chatId: Long) {
        viewModel.signalChatPresence()
        viewModel.cancelCallUpdate()

        navigator.openChat(
            context = requireActivity(),
            chatId = chatId,
            action = Constants.ACTION_CHAT_SHOW_MESSAGES
        )
    }


    private fun onItemMoreClick(chatRoomItem: ChatRoomItem) {
        scheduledMeetingManagementViewModel.setChatId(chatRoomItem.chatId)
        ChatListBottomSheetDialogFragment.newInstance(chatRoomItem.chatId)
            .show(childFragmentManager)
    }

    private fun showMeetingShareLink() {
        MeetingShareLinkBottomSheetFragment.newInstance()
            .show(childFragmentManager)
    }

    private fun onItemSelected(chatId: Long) {
        viewModel.onItemSelected(chatId)
    }

    private fun onTabSelected(selectedTab: ChatTab) {
        currentTab = selectedTab

        viewModel.clearSelection()
        viewModel.clearSearchQuery()
        activity?.invalidateMenu()
        view?.hideKeyboard()

        if (currentTab == ChatTab.CHATS) {
            Analytics.tracker.trackEvent(ChatsTabEvent)
        } else {
            viewModel.requestMeetings()
            Analytics.tracker.trackEvent(MeetingsTabEvent)
        }
    }

    /**
     * Check if meeting tab is shown
     */
    private fun isMeetingTabShown(): Boolean =
        currentTab == ChatTab.MEETINGS

    /**
     * On create meeting
     */
    fun onCreateMeeting() {
        viewModel.checkParticipatingInChatCall()
    }

    private fun openMeetingToCreate() {
        val meetingIntent = Intent(context, MeetingActivity::class.java).apply {
            action = MeetingActivity.MEETING_ACTION_CREATE
        }
        startActivity(meetingIntent)
    }

    private fun hasBluetoothPermission() =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED

    /**
     * Launch chat call screen
     *
     * @param chatId    Chat id to be shown
     */
    private fun launchChatCallScreen(chatId: Long) {
        startActivity(
            Intent(context, MeetingActivity::class.java).apply {
                action = MeetingActivity.MEETING_ACTION_IN
                putExtra(MeetingActivity.MEETING_CHAT_ID, chatId)
                putExtra(MeetingActivity.MEETING_AUDIO_ENABLE, true)
                putExtra(MeetingActivity.MEETING_VIDEO_ENABLE, false)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        )
    }

    /**
     * Launch waiting room screen
     *
     * @param chatId    Chat Room Id
     */
    private fun launchWaitingRoomScreen(chatId: Long) {
        startActivity(
            Intent(context, WaitingRoomActivity::class.java).apply {
                putExtra(WaitingRoomActivity.EXTRA_CHAT_ID, chatId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        )
    }

    /**
     * Launch chat screen
     *
     * @param chatId    Chat id
     */
    private fun launchChatScreen(chatId: Long) {
        navigator.openChat(
            context = requireActivity(),
            chatId = chatId,
            action = Constants.ACTION_CHAT_SHOW_MESSAGES
        )
    }

    /**
     * On schedule meeting
     */
    private fun onScheduleMeeting() {
        Analytics.tracker.trackEvent(ScheduleMeetingPressedEvent)
        scheduleResultContract.launch(Intent(context, CreateScheduledMeetingActivity::class.java))
    }

    private fun startChatAction(isFabClicked: Boolean) {
        if (isMeetingTabShown()) {
            if (isFabClicked) {
                Analytics.tracker.trackEvent(ChatTabFABPressedEvent)
                MeetingBottomSheetDialogFragment.newInstance(true).apply {
                    onScheduleMeeting = ::onScheduleMeeting
                }.show(
                    childFragmentManager,
                    MeetingBottomSheetDialogFragment.TAG
                )
            } else {
                (activity as? ManagerActivity?)?.onCreateMeeting()
            }

        } else {
            if (isFabClicked || viewModel.getState().value.hasAnyContact) {
                Analytics.tracker.trackEvent(ChatTabFABPressedEvent)
                startConversationLauncher.launch(
                    StartConversationActivity.getChatIntent(
                        requireContext()
                    )
                )
            } else {
                Analytics.tracker.trackEvent(InviteFriendsPressedEvent)
                val activity = InviteContactActivity::class.java
                val intent = Intent(context, activity)
                startActivity(intent)
            }
        }
    }

    private fun buildActionMode(): ActionMode.Callback =
        object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                mode.menuInflater.inflate(R.menu.fragment_chat_tabs_selection, menu)
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
                val currentState = viewModel.getState().value
                val currentItems = if (!isMeetingTabShown()) {
                    currentState.chats
                } else {
                    currentState.meetings
                }
                val selectedItems = currentState.selectedIds.mapNotNull { chatId ->
                    currentItems.firstOrNull { it.chatId == chatId }
                }

                menu.apply {
                    findItem(R.id.menu_chat_unmute)?.isVisible =
                        selectedItems.all { it.isMuted }
                    findItem(R.id.menu_chat_mute)?.isVisible =
                        selectedItems.all { !it.isMuted }
                    findItem(R.id.menu_chat_select_all)?.isVisible =
                        selectedItems.size != currentItems.size
                    findItem(R.id.menu_chat_leave)?.isVisible =
                        selectedItems.all {
                            it is ChatRoomItem.GroupChatRoomItem
                                    || it is ChatRoomItem.MeetingChatRoomItem
                        }
                }
                return true
            }

            override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean =
                when (item.itemId) {
                    R.id.menu_chat_select_all -> {
                        val allItems = if (!isMeetingTabShown()) {
                            viewModel.getState().value.chats.map(ChatRoomItem::chatId)
                        } else {
                            viewModel.getState().value.meetings.map(ChatRoomItem::chatId)
                        }
                        viewModel.onItemsSelected(allItems)
                        true
                    }

                    R.id.menu_chat_unselect_all -> {
                        viewModel.clearSelection()
                        true
                    }

                    R.id.menu_chat_mute -> {
                        ChatUtil.createMuteNotificationsAlertDialogOfChats(
                            requireActivity(),
                            viewModel.getState().value.selectedIds
                        )
                        viewModel.clearSelection()
                        true
                    }

                    R.id.menu_chat_unmute -> {
                        viewModel.getState().value.selectedIds.forEach { id ->
                            MegaApplication.getPushNotificationSettingManagement()
                                .controlMuteNotificationsOfAChat(
                                    requireContext(),
                                    Constants.NOTIFICATIONS_ENABLED,
                                    id
                                )
                        }
                        viewModel.clearSelection()
                        true
                    }

                    R.id.menu_chat_archive -> {
                        val selectedMeetings = viewModel.getState().value.selectedIds.toLongArray()
                        viewModel.archiveChats(*selectedMeetings)
                        viewModel.clearSelection()
                        true
                    }

                    R.id.menu_chat_leave -> {
                        val selectedMeetings = viewModel.getState().value.selectedIds
                        MaterialAlertDialogBuilder(
                            requireContext(),
                            R.style.ThemeOverlay_Mega_MaterialAlertDialog
                        )
                            .setTitle(getString(R.string.title_confirmation_leave_group_chat))
                            .setMessage(getString(R.string.confirmation_leave_group_chat))
                            .setPositiveButton(getString(R.string.general_leave)) { _, _ ->
                                viewModel.leaveChats(selectedMeetings)
                            }
                            .setNegativeButton(
                                getString(R.string.general_cancel),
                                null
                            )
                            .show()
                        viewModel.clearSelection()
                        true
                    }

                    else -> false
                }

            override fun onDestroyActionMode(mode: ActionMode) {
                viewModel.clearSelection()
                actionMode = null
            }
        }

    private fun onDismissDialog() {
        scheduledMeetingManagementViewModel.let {
            it.setOnChatHistoryEmptyConsumed()
            it.onResetSelectedOccurrence()
            it.setOnChatIdConsumed()
            it.setOnChatRoomItemConsumed()
        }
    }

    private fun showMeetingShareOptions() {
        val subject = getString(R.string.meetings_sharing_meeting_link_meeting_invite_subject)
        val message = getString(
            R.string.meetings_sharing_meeting_link_title,
            scheduledMeetingManagementViewModel.state.value.myFullName
        )
        val meetingName =
            getString(
                R.string.meetings_sharing_meeting_link_meeting_name,
                scheduledMeetingManagementViewModel.state.value.title
            )
        val meetingLink =
            getString(
                R.string.meetings_sharing_meeting_link_meeting_link,
                scheduledMeetingManagementViewModel.state.value.meetingLink
            )

        val body = StringBuilder()
        body.append("\n")
            .append(message)
            .append("\n\n")
            .append(meetingName)

        scheduledMeetingManagementViewModel.chatScheduledMeeting?.let {
            val meetingDateAndTime = getString(
                R.string.meetings_sharing_meeting_link_meeting_date_and_time,
                ScheduledMeetingDateUtil.getAppropriateStringForScheduledMeetingDate(
                    requireContext(),
                    scheduledMeetingManagementViewModel.is24HourFormat,
                    it
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
}
