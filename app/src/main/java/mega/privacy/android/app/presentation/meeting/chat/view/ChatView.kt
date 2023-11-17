package mega.privacy.android.app.presentation.meeting.chat.view

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.palm.composestateevents.EventEffect
import mega.privacy.android.app.R
import mega.privacy.android.app.main.AddContactActivity
import mega.privacy.android.app.main.InviteContactActivity
import mega.privacy.android.app.main.megachat.GroupChatInfoActivity
import mega.privacy.android.app.meeting.activity.MeetingActivity
import mega.privacy.android.app.presentation.contactinfo.ContactInfoActivity
import mega.privacy.android.app.presentation.meeting.ScheduledMeetingInfoActivity
import mega.privacy.android.app.presentation.meeting.chat.extension.toString
import mega.privacy.android.app.presentation.meeting.chat.model.ChatRoomMenuAction
import mega.privacy.android.app.presentation.meeting.chat.model.ChatUiState
import mega.privacy.android.app.presentation.meeting.chat.model.ChatViewModel
import mega.privacy.android.app.presentation.meeting.chat.view.appbar.ChatAppBar
import mega.privacy.android.app.presentation.meeting.chat.view.dialog.AllContactsAddedDialog
import mega.privacy.android.app.presentation.meeting.chat.view.dialog.ClearChatConfirmationDialog
import mega.privacy.android.app.presentation.meeting.chat.view.dialog.NoContactToAddDialog
import mega.privacy.android.app.presentation.meeting.chat.view.dialog.ParticipatingInACallDialog
import mega.privacy.android.app.presentation.meeting.chat.view.message.FirstMessageHeader
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.CONTACT_TYPE_MEGA
import mega.privacy.android.core.ui.controls.snackbars.MegaSnackbar
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import timber.log.Timber

@Composable
internal fun ChatView(
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    ChatView(
        uiState = uiState,
        onBackPressed = { onBackPressedDispatcher?.onBackPressed() },
        onMenuActionPressed = viewModel::handleActionPress,
        enablePasscodeCheck = viewModel::enablePasscodeCheck,
        inviteContactsToChat = viewModel::inviteContactsToChat,
        onInviteContactsResultConsumed = viewModel::onInviteContactsResultConsumed,
        onPushNotificationMuteOptionEventConsumed = viewModel::onPushNotificationMuteOptionEventConsumed,
        onClearChatHistory = viewModel::clearChatHistory,
        onInfoToShowConsumed = viewModel::onInfoToShowEventConsumed,
        endCallForAll = viewModel::endCall,
    )
}

/**
 * Chat view
 *
 * @param uiState [ChatUiState]
 * @param onBackPressed Action to perform for back button.
 */
@Composable
internal fun ChatView(
    uiState: ChatUiState,
    onBackPressed: () -> Unit,
    onMenuActionPressed: (ChatRoomMenuAction) -> Unit,
    inviteContactsToChat: (Long, List<String>) -> Unit = { _, _ -> },
    onInviteContactsResultConsumed: () -> Unit = {},
    onPushNotificationMuteOptionEventConsumed: () -> Unit = {},
    onClearChatHistory: () -> Unit = {},
    onInfoToShowConsumed: () -> Unit = {},
    enablePasscodeCheck: () -> Unit = {},
    endCallForAll: () -> Unit = {},
) {
    val context = LocalContext.current
    val snackBarHostState = remember { SnackbarHostState() }
    var showParticipatingInACallDialog by rememberSaveable { mutableStateOf(false) }
    var showNoContactToAddDialog by rememberSaveable { mutableStateOf(false) }
    var showAllContactsParticipateInChat by rememberSaveable { mutableStateOf(false) }
    var showClearChat by rememberSaveable { mutableStateOf(false) }
    var showEndCallForAllDialog by rememberSaveable { mutableStateOf(false) }

    val addContactLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
            result.data?.let { intent ->
                intent.getStringArrayListExtra(AddContactActivity.EXTRA_CONTACTS)
                    ?.let { contactList ->
                        inviteContactsToChat(
                            uiState.chatId,
                            contactList
                        )
                    }
            }
        }

    Scaffold(
        topBar = {
            ChatAppBar(
                uiState = uiState,
                snackBarHostState = snackBarHostState,
                onBackPressed = onBackPressed,
                onMenuActionPressed = onMenuActionPressed,
                showParticipatingInACallDialog = {
                    showParticipatingInACallDialog = true
                },
                showAllContactsParticipateInChat = {
                    showAllContactsParticipateInChat = true
                },
                showGroupOrContactInfoActivity = {
                    showGroupOrContactInfoActivity(context, uiState)
                },
                showNoContactToAddDialog = {
                    showNoContactToAddDialog = true
                },
                onStartCall = { isVideoCall ->
                    // start a call here
                },
                openAddContactActivity = {
                    openAddContactActivity(
                        context = context,
                        chatId = uiState.chatId,
                        addContactLauncher = addContactLauncher
                    )
                },
                showClearChatConfirmationDialog = {
                    showClearChat = true
                },
                showEndCallForAllDialog = {
                    showEndCallForAllDialog = true
                },
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackBarHostState) { data ->
                MegaSnackbar(snackbarData = data)
            }
        }
    )
    { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            item("first_message_header") { FirstMessageHeader(uiState) }
        }

        if (showParticipatingInACallDialog) {
            ParticipatingInACallDialog(
                onDismiss = { showParticipatingInACallDialog = false },
                onConfirm = {
                    showParticipatingInACallDialog = false
                    // return to active call
                    uiState.currentCall?.let {
                        enablePasscodeCheck()
                        startMeetingActivity(context, it)
                    }
                }
            )
        }

        if (showNoContactToAddDialog) {
            NoContactToAddDialog(
                onDismiss = { showNoContactToAddDialog = false },
                onConfirm = {
                    showNoContactToAddDialog = false
                    context.startActivity(Intent(context, InviteContactActivity::class.java))
                }
            )
        }

        if (showAllContactsParticipateInChat) {
            AllContactsAddedDialog {
                showAllContactsParticipateInChat = false
            }
        }

        if (showClearChat) {
            ClearChatConfirmationDialog(
                isMeeting = uiState.isMeeting,
                onDismiss = { showClearChat = false },
                onConfirm = {
                    showClearChat = false
                    onClearChatHistory()
                })
        }
        EventEffect(
            event = uiState.pushNotificationMuteOptionEvent,
            onConsumed = onPushNotificationMuteOptionEventConsumed
        ) { muteOption ->
            snackBarHostState.showSnackbar(muteOption.toString(context))
        }
        if (showEndCallForAllDialog) {
            EndCallForAllDialog(
                onDismiss = { showEndCallForAllDialog = false },
                onConfirm = {
                    showEndCallForAllDialog = false
                    endCallForAll()
                }
            )
        }
    }

    EventEffect(
        event = uiState.inviteToChatResultEvent,
        onConsumed = onInviteContactsResultConsumed
    ) { inviteResult ->
        snackBarHostState.showSnackbar(inviteResult.toString(context))
    }

    EventEffect(
        event = uiState.infoToShowEvent,
        onConsumed = onInfoToShowConsumed
    ) { info ->
        snackBarHostState.showSnackbar(context.getString(info))
    }
}

private fun openAddContactActivity(
    context: Context,
    chatId: Long,
    addContactLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>,
) {
    val intent =
        Intent(context, AddContactActivity::class.java).apply {
            putExtra(
                AddContactActivity.EXTRA_CONTACT_TYPE,
                CONTACT_TYPE_MEGA
            )
            putExtra(Constants.INTENT_EXTRA_KEY_CHAT, true)
            putExtra(
                Constants.INTENT_EXTRA_KEY_CHAT_ID,
                chatId
            )
            putExtra(
                Constants.INTENT_EXTRA_KEY_TOOL_BAR_TITLE,
                context.getString(R.string.add_participants_menu_item)
            )
        }

    addContactLauncher.launch(intent)
}

private fun showGroupOrContactInfoActivity(context: Context, uiState: ChatUiState) {
    if (uiState.scheduledMeeting != null && uiState.schedIsPending && uiState.isMeeting && uiState.isActive) {
        Timber.d("show scheduled meeting info")
        Intent(context, ScheduledMeetingInfoActivity::class.java).apply {
            putExtra(Constants.CHAT_ID, uiState.scheduledMeeting.chatId)
            putExtra(Constants.SCHEDULED_MEETING_ID, uiState.scheduledMeeting.schedId)
        }.also {
            context.startActivity(it)
        }
    } else {
        val targetActivity =
            if (uiState.isGroup) GroupChatInfoActivity::class.java else ContactInfoActivity::class.java
        Intent(context, targetActivity).apply {
            putExtra(Constants.HANDLE, uiState.chatId)
            putExtra(Constants.ACTION_CHAT_OPEN, true)
        }.also {
            context.startActivity(it)
        }
    }
}

private fun startMeetingActivity(context: Context, chatId: Long) {
    context.startActivity(Intent(context, MeetingActivity::class.java).apply {
        action = MeetingActivity.MEETING_ACTION_IN
        putExtra(MeetingActivity.MEETING_CHAT_ID, chatId)
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
    })
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "ChatView")
@Composable
private fun ChatViewPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        val uiState = ChatUiState(
            title = "My Name",
            userChatStatus = UserChatStatus.Away,
            isChatNotificationMute = true,
            isPrivateChat = true,
            myPermission = ChatRoomPermission.Standard,
        )
        ChatView(
            uiState = uiState,
            onBackPressed = {},
            onMenuActionPressed = {},
            inviteContactsToChat = { _, _ -> },
        )
    }
}