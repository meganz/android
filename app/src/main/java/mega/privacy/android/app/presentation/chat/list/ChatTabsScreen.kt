package mega.privacy.android.app.presentation.chat.list

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.SnackbarResult
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.launch
import mega.android.core.ui.extensions.LaunchedOnceEffect
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.MegaApplication.Companion.getPushNotificationSettingManagement
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.contract.SendToChatActivityContract
import mega.privacy.android.app.extensions.navigateToAppSettings
import mega.privacy.android.app.meeting.activity.MeetingActivity
import mega.privacy.android.app.presentation.chat.archived.ArchivedChatsActivity
import mega.privacy.android.app.presentation.chat.list.dialog.ChatRoomItemBottomSheet
import mega.privacy.android.app.presentation.chat.list.dialog.ChatStatusDialog
import mega.privacy.android.app.presentation.chat.list.dialog.MeetingBottomSheet
import mega.privacy.android.app.presentation.chat.list.dialog.MeetingShareLinkBottomSheet
import mega.privacy.android.app.presentation.chat.list.dialog.MuteChatDialog
import mega.privacy.android.app.presentation.chat.list.dialog.MuteTarget
import mega.privacy.android.app.presentation.chat.list.dialog.OpenLinkDialog
import mega.privacy.android.app.presentation.chat.list.model.ChatTab
import mega.privacy.android.app.presentation.chat.list.view.ChatTabsView
import mega.privacy.android.app.presentation.contact.invite.InviteContactActivity
import mega.privacy.android.app.presentation.data.SnackBarItem
import mega.privacy.android.app.presentation.meeting.ChatInfoActivity
import mega.privacy.android.app.presentation.meeting.CreateScheduledMeetingActivity
import mega.privacy.android.app.presentation.meeting.NoteToSelfChatViewModel
import mega.privacy.android.app.presentation.meeting.ScheduledMeetingManagementViewModel
import mega.privacy.android.app.presentation.meeting.WaitingRoomActivity
import mega.privacy.android.app.presentation.meeting.chat.extension.toInfoText
import mega.privacy.android.app.presentation.meeting.model.ShareLinkOption
import mega.privacy.android.app.presentation.startconversation.StartConversationActivity
import mega.privacy.android.app.presentation.startconversation.StartConversationActivity.Companion.EXTRA_JOIN_MEETING
import mega.privacy.android.app.presentation.startconversation.StartConversationActivity.Companion.EXTRA_NEW_CHAT_ID
import mega.privacy.android.app.presentation.startconversation.StartConversationActivity.Companion.EXTRA_NEW_MEETING
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.ChatUtil
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.ScheduledMeetingDateUtil
import mega.privacy.android.domain.entity.chat.ChatRoomItem
import mega.privacy.android.navigation.destination.ChatNavKey
import mega.privacy.android.shared.original.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.shared.original.core.ui.utils.showAutoDurationSnackbar
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.ArchivedChatsMenuItemEvent
import mega.privacy.mobile.analytics.event.ChatRoomDNDMenuItemEvent
import mega.privacy.mobile.analytics.event.ChatScreenEvent
import mega.privacy.mobile.analytics.event.ChatTabFABPressedEvent
import mega.privacy.mobile.analytics.event.ChatsTabEvent
import mega.privacy.mobile.analytics.event.InviteFriendsPressedEvent
import mega.privacy.mobile.analytics.event.JoinMeetingPressedEvent
import mega.privacy.mobile.analytics.event.MeetingsTabEvent
import mega.privacy.mobile.analytics.event.OpenLinkMenuItemEvent
import mega.privacy.mobile.analytics.event.OpenNoteToSelfButtonPressedEvent
import mega.privacy.mobile.analytics.event.ScheduleMeetingMenuItemEvent
import mega.privacy.mobile.analytics.event.ScheduleMeetingPressedEvent
import mega.privacy.mobile.analytics.event.ScheduledMeetingShareMeetingLinkButtonEvent
import mega.privacy.mobile.analytics.event.SendMeetingLinkToChatScheduledMeetingEvent
import mega.privacy.mobile.analytics.event.StartMeetingNowPressedEvent
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import timber.log.Timber

/**
 * Compose screen wrapping [ChatTabsView] for use in ChatActivity (Nav3).
 * Replaces [ChatTabsFragment] when hosted in the new single-activity architecture.
 */
@Composable
internal fun ChatTabsScreen(
    showMeetingTab: Boolean,
    createNewChat: Boolean,
    onNavigateToChat: (chatId: Long) -> Unit,
    onFinish: () -> Unit,
) {
    val viewModel: ChatTabsViewModel = hiltViewModel()
    val scheduledMeetingManagementViewModel: ScheduledMeetingManagementViewModel = hiltViewModel()
    val noteToSelfChatViewModel: NoteToSelfChatViewModel = hiltViewModel()

    val chatsTabState by viewModel.getState().collectAsStateWithLifecycle()
    val managementState by scheduledMeetingManagementViewModel.state.collectAsStateWithLifecycle()
    val noteToSelfChatState by noteToSelfChatViewModel.state.collectAsStateWithLifecycle()
    val scaffoldState = rememberScaffoldState()
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    val context = LocalContext.current
    val resources = LocalResources.current
    val activity = LocalActivity.current as FragmentActivity

    var showChatStatusDialog by rememberSaveable { mutableStateOf(false) }
    var showShareLinkSheet by rememberSaveable { mutableStateOf(false) }
    var showMeetingSheet by rememberSaveable { mutableStateOf(false) }
    var moreOptionsChatId by rememberSaveable { mutableStateOf<Long?>(null) }
    var openLinkVisible by rememberSaveable { mutableStateOf(false) }
    var openLinkIsJoinMeeting by rememberSaveable { mutableStateOf(false) }
    var leaveConfirmation by remember { mutableStateOf<LeaveConfirmation?>(null) }
    var muteTarget by remember { mutableStateOf<MuteTarget?>(null) }

    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGrant ->
        if (isGrant) {
            openMeetingToCreate(context)
        } else {
            viewModel.updateSnackBar(
                SnackBarItem(
                    type = Constants.PERMISSIONS_TYPE,
                    stringRes = R.string.meeting_bluetooth_connect_required_permissions_warning
                )
            )
        }
    }

    val startConversationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val intentData = result.data
        if (result.resultCode == Activity.RESULT_OK && intentData != null) {
            val isNewMeeting = intentData.getBooleanExtra(EXTRA_NEW_MEETING, false)
            val isJoinMeeting = intentData.getBooleanExtra(EXTRA_JOIN_MEETING, false)
            when {
                isNewMeeting -> {
                    onCreateMeetingClick(viewModel)
                }

                isJoinMeeting -> {
                    onJoinMeetingClick(viewModel, context)
                }

                else -> {
                    val chatId = intentData.getLongExtra(
                        EXTRA_NEW_CHAT_ID,
                        MEGACHAT_INVALID_HANDLE
                    )
                    if (chatId != MEGACHAT_INVALID_HANDLE) {
                        onNavigateToChat(chatId)
                    }
                }
            }
        } else {
            Timber.w("StartConversationActivity invalid result: $result")
        }
    }

    val sendToChatLauncher = rememberLauncherForActivityResult(SendToChatActivityContract()) {
        if (it != null) {
            scheduledMeetingManagementViewModel.sendToChat(
                data = it,
                link = scheduledMeetingManagementViewModel.state.value.meetingLink
            )
        }
    }

    val scheduleResultLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Timber.d("Schedule meeting result: ${result.resultCode}")
        if (result.resultCode == Activity.RESULT_OK) {
            val isLinkCreated = result.data?.getBooleanExtra(
                CreateScheduledMeetingActivity.MEETING_LINK_CREATED_TAG,
                false
            ) == true
            if (isLinkCreated) {
                val chatId = result.data?.getLongExtra(
                    ChatNavKey.LEGACY_CHAT_ID,
                    -1L
                ) ?: -1L
                if (chatId != -1L) {
                    val link = result.data?.getStringExtra(
                        CreateScheduledMeetingActivity.MEETING_LINK_TAG
                    )
                    val title = result.data?.getStringExtra(
                        CreateScheduledMeetingActivity.MEETING_TITLE_TAG
                    ) ?: ""
                    scheduledMeetingManagementViewModel.setMeetingLink(chatId, link, title)
                }
            }
        }
    }

    val editScheduledMeetLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val isLinkCreated = result.data?.getBooleanExtra(
                CreateScheduledMeetingActivity.MEETING_LINK_CREATED_TAG,
                false
            ) == true
            if (isLinkCreated) {
                val resultChatId = result.data?.getLongExtra(
                    ChatNavKey.LEGACY_CHAT_ID,
                    -1L
                ) ?: -1L
                if (resultChatId != -1L) {
                    val link = result.data?.getStringExtra(
                        CreateScheduledMeetingActivity.MEETING_LINK_TAG
                    )
                    val title = result.data?.getStringExtra(
                        CreateScheduledMeetingActivity.MEETING_TITLE_TAG
                    ) ?: ""
                    scheduledMeetingManagementViewModel.setMeetingLink(
                        resultChatId, link, title
                    )
                }
            }
            scheduledMeetingManagementViewModel.scheduledMeetingUpdated()
        }
    }

    val callPermissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        val targetChatId = moreOptionsChatId
        if (targetChatId != null) {
            if (mega.privacy.android.app.utils.permission.PermissionUtils.checkMandatoryCallPermissions(
                    activity
                )
            ) {
                viewModel.startMeetingCall(
                    targetChatId,
                    {
                        (activity as? mega.privacy.android.app.activities.PasscodeActivity)
                            ?.passcodeFacade?.enablePassCode()
                    },
                )
            } else {
                viewModel.updateSnackBar(
                    SnackBarItem(
                        type = Constants.NOT_CALL_PERMISSIONS_SNACKBAR_TYPE,
                        stringRes = R.string.allow_acces_calls_subtitle_microphone,
                    )
                )
            }
        }
    }

    LaunchedOnceEffect {
        Analytics.tracker.trackEvent(ChatScreenEvent)
        Firebase.crashlytics.log("Screen: ${ChatScreenEvent.eventName}")
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                viewModel.clearSearchQuery()
            } else if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkHasArchivedChats()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            scheduledMeetingManagementViewModel.stopMonitoringLoadMessages()
        }
    }

    var hasLaunchedCreateChat by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (createNewChat && !hasLaunchedCreateChat) {
            hasLaunchedCreateChat = true
            startConversationLauncher.launch(
                Intent(context, StartConversationActivity::class.java)
            )
        }
    }

    LaunchedEffect(chatsTabState.snackBar) {
        val snackBar = chatsTabState.snackBar ?: return@LaunchedEffect
        when (snackBar.type) {
            Constants.NOT_CALL_PERMISSIONS_SNACKBAR_TYPE -> {
                val result = scaffoldState.snackbarHostState.showAutoDurationSnackbar(
                    message = snackBar.getMessage(resources),
                    actionLabel = resources.getString(R.string.general_allow)
                )
                if (result == SnackbarResult.ActionPerformed) {
                    context.navigateToAppSettings()
                }
            }

            Constants.PERMISSIONS_TYPE -> {
                val result = scaffoldState.snackbarHostState.showAutoDurationSnackbar(
                    message = snackBar.getMessage(resources),
                    actionLabel = resources.getString(R.string.action_settings)
                )
                if (result == SnackbarResult.ActionPerformed) {
                    context.navigateToAppSettings()
                }
            }

            else -> {
                scaffoldState.snackbarHostState.showAutoDurationSnackbar(
                    message = snackBar.getMessage(resources),
                )
            }
        }
        viewModel.updateSnackBar(null)
    }

    EventEffect(
        managementState.meetingLinkCreated,
        scheduledMeetingManagementViewModel::onMeetingLinkShareShown
    ) {
        showShareLinkSheet = true
    }

    EventEffect(
        chatsTabState.openLinkEvent,
        viewModel::onOpenLinkConsumed,
    ) { isJoinMeeting ->
        openLinkIsJoinMeeting = isJoinMeeting
        openLinkVisible = true
    }

    EventEffect(
        managementState.meetingLinkAction,
        scheduledMeetingManagementViewModel::onMeetingLinkShareConsumed
    ) {
        when (it) {
            ShareLinkOption.SendLinkToChat -> {
                Analytics.tracker.trackEvent(SendMeetingLinkToChatScheduledMeetingEvent)
                sendToChatLauncher.launch(longArrayOf())
            }

            ShareLinkOption.ShareLink -> {
                Analytics.tracker.trackEvent(ScheduledMeetingShareMeetingLinkButtonEvent)
                showMeetingShareOptions(context, scheduledMeetingManagementViewModel)
            }
        }
    }

    LaunchedEffect(chatsTabState.currentCallChatId) {
        chatsTabState.currentCallChatId?.let { chatId ->
            context.startActivity(
                Intent(context, MeetingActivity::class.java).apply {
                    action = MeetingActivity.MEETING_ACTION_IN
                    putExtra(MeetingActivity.MEETING_CHAT_ID, chatId)
                    putExtra(MeetingActivity.MEETING_AUDIO_ENABLE, true)
                    putExtra(MeetingActivity.MEETING_VIDEO_ENABLE, false)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
            viewModel.removeCurrentCallAndWaitingRoom()
        }
    }

    LaunchedEffect(chatsTabState.currentWaitingRoom) {
        chatsTabState.currentWaitingRoom?.let { chatId ->
            context.startActivity(
                Intent(context, WaitingRoomActivity::class.java).apply {
                    putExtra(WaitingRoomActivity.EXTRA_CHAT_ID, chatId)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
            viewModel.removeCurrentCallAndWaitingRoom()
        }
    }

    LaunchedEffect(chatsTabState.isParticipatingInChatCallResult) {
        chatsTabState.isParticipatingInChatCallResult?.let { isInCall ->
            if (isInCall) {
                CallUtil.showConfirmationInACall(
                    context,
                    resources.getString(R.string.ongoing_call_content),
                )
            } else {
                if (hasBluetoothPermission(context)) {
                    openMeetingToCreate(context)
                } else {
                    bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                }
            }
            viewModel.markHandleIsParticipatingInChatCall()
        }
    }

    val onScheduleMeeting: () -> Unit = {
        Analytics.tracker.trackEvent(ScheduleMeetingPressedEvent)
        scheduleResultLauncher.launch(
            Intent(context, CreateScheduledMeetingActivity::class.java)
        )
    }

    ChatTabsView(
        scaffoldState = scaffoldState,
        isNewSingleActivity = true,
        state = chatsTabState,
        managementState = managementState,
        noteToSelfChatState = noteToSelfChatState,
        showMeetingTab = showMeetingTab,
        onTabSelected = { selectedTab ->
            viewModel.setTabSelected(selectedTab)
            viewModel.clearSelection()
            viewModel.clearSearchQuery()
            keyboardController?.hide()
            if (selectedTab == ChatTab.CHATS) {
                Analytics.tracker.trackEvent(ChatsTabEvent)
            } else {
                viewModel.requestMeetings()
                Analytics.tracker.trackEvent(MeetingsTabEvent)
            }
        },
        onItemClick = { chatId, isNoteToSelfChat ->
            if (isNoteToSelfChat) {
                Analytics.tracker.trackEvent(OpenNoteToSelfButtonPressedEvent)
            }
            viewModel.signalChatPresence()
            viewModel.cancelCallUpdate()
            onNavigateToChat(chatId)
        },
        onItemMoreClick = { chatRoomItem ->
            scheduledMeetingManagementViewModel.setChatId(chatRoomItem.chatId)
            moreOptionsChatId = chatRoomItem.chatId
        },
        onItemSelected = viewModel::onItemSelected,
        onResetStateSnackbarMessage = viewModel::onSnackbarMessageConsumed,
        onResetManagementStateSnackbarMessage = scheduledMeetingManagementViewModel::onSnackbarMessageConsumed,
        onCancelScheduledMeeting = {
            scheduledMeetingManagementViewModel.onCancelScheduledMeeting()
            dismissScheduledMeetingDialog(scheduledMeetingManagementViewModel)
        },
        onDismissDialog = {
            dismissScheduledMeetingDialog(scheduledMeetingManagementViewModel)
        },
        onStartChatClick = { isFabClicked ->
            if (viewModel.isMeetingTabShown()) {
                if (isFabClicked) {
                    Analytics.tracker.trackEvent(ChatTabFABPressedEvent)
                    showMeetingSheet = true
                } else {
                    onCreateMeetingClick(viewModel)
                }
            } else {
                if (isFabClicked || chatsTabState.hasAnyContact) {
                    Analytics.tracker.trackEvent(ChatTabFABPressedEvent)
                    startConversationLauncher.launch(
                        StartConversationActivity.getChatIntent(context)
                    )
                } else {
                    Analytics.tracker.trackEvent(InviteFriendsPressedEvent)
                    context.startActivity(
                        Intent(context, InviteContactActivity::class.java)
                    )
                }
            }
        },
        onShowNextTooltip = viewModel::setNextMeetingTooltip,
        onDismissForceAppUpdateDialog = viewModel::onForceUpdateDialogDismissed,
        onScheduleMeeting = onScheduleMeeting,
        onSearchTextChange = viewModel::setSearchQuery,
        onSearchCloseClicked = viewModel::clearSearchQuery,
        onNavigationClick = onFinish,
        onChangeUserStatus = { showChatStatusDialog = true },
        onDoNotDisturbActionClick = {
            Analytics.tracker.trackEvent(ChatRoomDNDMenuItemEvent)
            if (ChatUtil.getGeneralNotification() == Constants.NOTIFICATIONS_ENABLED) {
                muteTarget = MuteTarget.Global
            } else {
                coroutineScope.launch {
                    val result = scaffoldState.snackbarHostState.showAutoDurationSnackbar(
                        message = resources.getString(R.string.notifications_are_already_muted),
                        actionLabel = resources.getString(R.string.general_unmute)
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        getPushNotificationSettingManagement().controlMuteNotifications(
                            context,
                            Constants.NOTIFICATIONS_ENABLED,
                            null
                        )
                    }
                }
            }
        },
        onOpenLinkActionClick = {
            Analytics.tracker.trackEvent(OpenLinkMenuItemEvent)
            openLinkIsJoinMeeting = false
            openLinkVisible = true
        },
        onArchivedActionClick = {
            Analytics.tracker.trackEvent(ArchivedChatsMenuItemEvent)
            context.startActivity(Intent(context, ArchivedChatsActivity::class.java))
        },
        onTitleChatArchivedEventConsumed = viewModel::onTitleChatArchivedEventConsumed,
        onClearSelection = viewModel::clearSelection,
        onSelectAll = {
            val allItems = if (viewModel.isMeetingTabShown()) {
                chatsTabState.meetings
            } else {
                chatsTabState.chats
            }
            viewModel.onItemsSelected(allItems.map(ChatRoomItem::chatId))
        },
        onMuteSelected = {
            muteTarget = MuteTarget.Multiple(
                chatIds = chatsTabState.selectedIds,
                isMeeting = viewModel.isMeetingTabShown(),
            )
            viewModel.clearSelection()
        },
        onUnmuteSelected = {
            chatsTabState.selectedIds.forEach { id ->
                MegaApplication.getPushNotificationSettingManagement()
                    .controlMuteNotificationsOfAChat(
                        activity,
                        Constants.NOTIFICATIONS_ENABLED,
                        id
                    )
            }
            viewModel.clearSelection()
        },
        onArchiveSelected = {
            viewModel.archiveChats(*chatsTabState.selectedIds.toLongArray())
            viewModel.clearSelection()
        },
        onLeaveSelected = {
            leaveConfirmation = LeaveConfirmation(chatIds = chatsTabState.selectedIds)
            viewModel.clearSelection()
        },
    )

    if (showChatStatusDialog) {
        ChatStatusDialog(
            onDismissRequest = { showChatStatusDialog = false },
            onError = {
                viewModel.updateSnackBar(
                    SnackBarItem(stringRes = R.string.changing_status_error)
                )
            },
        )
    }

    if (showShareLinkSheet) {
        MeetingShareLinkBottomSheet(
            onSendLinkToChat = {
                scheduledMeetingManagementViewModel.onMeetingLinkShare(ShareLinkOption.SendLinkToChat)
            },
            onShareLink = {
                scheduledMeetingManagementViewModel.onMeetingLinkShare(ShareLinkOption.ShareLink)
            },
            onDismissRequest = { showShareLinkSheet = false },
        )
    }

    if (showMeetingSheet) {
        MeetingBottomSheet(
            onStartMeeting = {
                onCreateMeetingClick(viewModel)
            },
            onJoinMeeting = {
                onJoinMeetingClick(viewModel, context)
            },
            onScheduleMeeting = {
                Analytics.tracker.trackEvent(ScheduleMeetingMenuItemEvent)
                onScheduleMeeting()
            },
            onDismissRequest = { showMeetingSheet = false },
        )
    }

    moreOptionsChatId?.let { id ->
        ChatRoomItemBottomSheet(
            chatId = id,
            viewModel = viewModel,
            scheduledMeetingManagementViewModel = scheduledMeetingManagementViewModel,
            onDismissRequest = { moreOptionsChatId = null },
            onStartMeetingPressed = {
                mega.privacy.android.app.utils.permission.PermissionUtils
                    .requestCallPermissions(callPermissionsLauncher)
            },
            onEditScheduledMeeting = { editChatId ->
                editScheduledMeetLauncher.launch(
                    Intent(context, CreateScheduledMeetingActivity::class.java)
                        .putExtra(ChatNavKey.LEGACY_CHAT_ID, editChatId)
                )
            },
            onPendingMeetingInfo = { infoChatId, schedId ->
                editScheduledMeetLauncher.launch(
                    Intent(context, ChatInfoActivity::class.java).apply {
                        putExtra(ChatNavKey.LEGACY_CHAT_ID, infoChatId)
                        putExtra(Constants.SCHEDULED_MEETING_ID, schedId)
                    }
                )
            },
            onMutePressed = { muteChatId, isMeeting ->
                muteTarget = MuteTarget.Single(muteChatId, isMeeting)
            },
            onLeavePressed = { leaveChatId, isMeeting ->
                leaveConfirmation = LeaveConfirmation(
                    chatIds = listOf(leaveChatId),
                    titleRes = if (isMeeting) {
                        R.string.meetings_leave_meeting_confirmation_dialog_title
                    } else {
                        R.string.title_confirmation_leave_group_chat
                    },
                )
            },
        )
    }

    muteTarget?.let { target ->
        MuteChatDialog(
            target = target,
            onDismissRequest = { muteTarget = null },
            onMuteResult = { option ->
                coroutineScope.launch {
                    scaffoldState.snackbarHostState.showAutoDurationSnackbar(
                        message = option.toInfoText(context),
                    )
                }
            },
        )
    }

    if (openLinkVisible) {
        OpenLinkDialog(
            isChatScreen = true,
            isJoinMeeting = openLinkIsJoinMeeting,
            onDismissRequest = { openLinkVisible = false },
        )
    }

    leaveConfirmation?.let { confirmation ->
        MegaAlertDialog(
            title = stringResource(confirmation.titleRes),
            text = stringResource(R.string.confirmation_leave_group_chat),
            confirmButtonText = stringResource(R.string.general_leave),
            cancelButtonText = stringResource(sharedR.string.general_dialog_cancel_button),
            onConfirm = {
                viewModel.leaveChats(confirmation.chatIds)
                leaveConfirmation = null
            },
            onDismiss = { leaveConfirmation = null },
        )
    }

    // endregion
}

/**
 * Start the "create meeting" flow: track the event then check whether the user is
 * already in a call before the meeting is opened.
 */
private fun onCreateMeetingClick(viewModel: ChatTabsViewModel) {
    Analytics.tracker.trackEvent(StartMeetingNowPressedEvent)
    viewModel.checkParticipatingInChatCall()
}

/**
 * Start the "join meeting" flow: track the event, block while already in a call,
 * otherwise open the join-link dialog.
 */
private fun onJoinMeetingClick(
    viewModel: ChatTabsViewModel,
    context: Context,
) {
    Analytics.tracker.trackEvent(JoinMeetingPressedEvent)
    if (CallUtil.participatingInACall()) {
        CallUtil.showConfirmationInACall(
            context,
            context.getString(sharedR.string.can_only_join_one_call_error_message),
        )
    } else {
        viewModel.triggerOpenLink(isJoinMeeting = true)
    }
}

private fun openMeetingToCreate(context: Context) {
    context.startActivity(
        Intent(context, MeetingActivity::class.java).apply {
            action = MeetingActivity.MEETING_ACTION_CREATE
        }
    )
}

private fun hasBluetoothPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.S || ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.BLUETOOTH_CONNECT
    ) == PackageManager.PERMISSION_GRANTED

private fun dismissScheduledMeetingDialog(
    scheduledMeetingManagementViewModel: ScheduledMeetingManagementViewModel,
) {
    scheduledMeetingManagementViewModel.apply {
        setOnChatHistoryEmptyConsumed()
        onResetSelectedOccurrence()
        setOnChatIdConsumed()
        setOnChatRoomItemConsumed()
    }
}

private fun showMeetingShareOptions(
    context: Context,
    scheduledMeetingManagementViewModel: ScheduledMeetingManagementViewModel,
) {
    val state = scheduledMeetingManagementViewModel.state.value
    val subject = context.getString(R.string.meetings_sharing_meeting_link_meeting_invite_subject)
    val message = context.getString(
        R.string.meetings_sharing_meeting_link_title,
        state.myFullName
    )
    val meetingName = context.getString(
        R.string.meetings_sharing_meeting_link_meeting_name,
        state.title
    )
    val meetingLink = context.getString(
        R.string.meetings_sharing_meeting_link_meeting_link,
        state.meetingLink
    )

    val body = StringBuilder()
    body.append("\n")
        .append(message)
        .append("\n\n")
        .append(meetingName)

    scheduledMeetingManagementViewModel.chatScheduledMeeting?.let {
        val meetingDateAndTime = context.getString(
            R.string.meetings_sharing_meeting_link_meeting_date_and_time,
            ScheduledMeetingDateUtil.getAppropriateStringForScheduledMeetingDate(
                context,
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
    context.startActivity(Intent.createChooser(intent, " "))
}
