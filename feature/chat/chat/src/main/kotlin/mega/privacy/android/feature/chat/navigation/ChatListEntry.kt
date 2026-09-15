package mega.privacy.android.feature.chat.navigation

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.filterNotNull
import mega.privacy.android.feature.chat.list.ChatListViewModel
import mega.privacy.android.feature.chat.list.model.MeetingActionEvent
import mega.privacy.android.feature.chat.list.view.BluetoothPermissionRequiredDialog
import mega.privacy.android.feature.chat.list.view.ChatListScreen
import mega.privacy.android.feature.chat.list.view.MeetingBlockedByCallDialog
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.destination.ChatListNavKey
import mega.privacy.android.navigation.destination.CreateMeetingNavKey
import mega.privacy.android.navigation.destination.CreateScheduledMeetingNavKey
import mega.privacy.android.navigation.destination.NewChatNavKey
import mega.privacy.android.navigation.destination.OpenLinkDialogNavKey
import mega.privacy.android.navigation.destination.ShowChatMessagesNavKey
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Chat list entry rendering the Compose [ChatListScreen] with Chats and Meetings tabs.
 *
 * Hosted by the app module's gated `ChatListNavKey` destination.
 *
 * @param navigationHandler
 * @param showMeetingTab Whether the Meetings tab should be initially selected.
 */
@SuppressLint("ComposeViewModelInjection")
@Composable
fun ChatListEntry(
    navigationHandler: NavigationHandler,
    showMeetingTab: Boolean,
) {
    val viewModel = hiltViewModel<ChatListViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var blockedMessage by rememberSaveable { mutableStateOf<Int?>(null) }
    var showBluetoothWarning by rememberSaveable { mutableStateOf(false) }

    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            navigationHandler.navigate(CreateMeetingNavKey)
        } else {
            showBluetoothWarning = true
        }
    }

    LaunchedEffect(Unit) {
        navigationHandler.monitorResult<NewChatNavKey.NewChatResult>(NewChatNavKey.KEY)
            .filterNotNull()
            .collect { result ->
                navigationHandler.clearResult(NewChatNavKey.KEY)
                viewModel.onNewChatResult(result)
            }
    }

    LaunchedEffect(Unit) {
        viewModel.openChatEvents.collect { chatId ->
            navigationHandler.navigate(ShowChatMessagesNavKey(chatId))
        }
    }

    LaunchedEffect(Unit) {
        viewModel.meetingActionEvents.collect { event ->
            when (event) {
                MeetingActionEvent.StartMeeting ->
                    if (context.hasBluetoothConnectPermission()) {
                        navigationHandler.navigate(CreateMeetingNavKey)
                    } else {
                        bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                    }

                MeetingActionEvent.JoinMeeting ->
                    navigationHandler.navigate(OpenLinkDialogNavKey(isJoinMeeting = true))

                MeetingActionEvent.ScheduleMeeting ->
                    navigationHandler.navigate(CreateScheduledMeetingNavKey)

                MeetingActionEvent.StartBlockedByOngoingCall ->
                    blockedMessage = sharedR.string.chat_start_meeting_already_in_call_error

                MeetingActionEvent.JoinBlockedByOngoingCall ->
                    blockedMessage = sharedR.string.can_only_join_one_call_error_message
            }
        }
    }

    ChatListScreen(
        uiState = uiState,
        showMeetingTab = showMeetingTab,
        onBackClick = navigationHandler::back,
        onItemClick = { chatId ->
            navigationHandler.navigate(ShowChatMessagesNavKey(chatId))
        },
        onNewChatClick = { navigationHandler.navigate(NewChatNavKey) },
        onMeetingActionClick = viewModel::onMeetingActionSelected,
        onArchivedClick = { navigationHandler.navigate(ArchivedChatsNavKey) },
        onSearchQueryChange = viewModel::onSearchQueryChange,
        resolveBulkActions = viewModel::getBulkChatRoomActions,
        onBulkActionSelected = viewModel::onBulkChatRoomActionSelected,
    )

    blockedMessage?.let { message ->
        MeetingBlockedByCallDialog(
            message = message,
            onDismiss = { blockedMessage = null },
        )
    }

    if (showBluetoothWarning) {
        BluetoothPermissionRequiredDialog(onDismiss = { showBluetoothWarning = false })
    }
}

private fun Context.hasBluetoothConnectPermission(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.S || ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.BLUETOOTH_CONNECT,
    ) == PackageManager.PERMISSION_GRANTED

/**
 * Registers the [ChatListNavKey] entry that renders [ChatListEntry] within a
 * navigation graph. Used by the Chat main navigation item to host the chat list
 * inside the bottom-navigation bar.
 *
 * @param navigationHandler
 */
fun EntryProviderScope<NavKey>.chatListScreen(
    navigationHandler: NavigationHandler,
) {
    entry<ChatListNavKey> { key ->
        ChatListEntry(
            navigationHandler = navigationHandler,
            showMeetingTab = key.showMeetingTab,
        )
    }
}
