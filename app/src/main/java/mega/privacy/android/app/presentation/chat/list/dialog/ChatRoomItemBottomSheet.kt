package mega.privacy.android.app.presentation.chat.list.dialog

import android.content.Intent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.android.core.ui.components.sheets.MegaModalBottomSheet
import mega.android.core.ui.components.sheets.MegaModalBottomSheetBackground
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.main.megachat.GroupChatInfoActivity
import mega.privacy.android.app.presentation.chat.dialog.view.ChatRoomItemBottomSheetView
import mega.privacy.android.app.presentation.chat.list.ChatTabsViewModel
import mega.privacy.android.app.presentation.contactinfo.ContactInfoActivity
import mega.privacy.android.app.presentation.meeting.ChatInfoActivity
import mega.privacy.android.app.presentation.meeting.RecurringMeetingInfoActivity
import mega.privacy.android.app.presentation.meeting.ScheduledMeetingManagementViewModel
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.chat.ChatAvatarItem
import mega.privacy.android.domain.entity.chat.ChatRoomItem
import mega.privacy.android.navigation.destination.ChatNavKey
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.mobile.analytics.event.ArchiveNoteToSelfButtonPressedEvent
import mega.privacy.mobile.analytics.event.ScheduledMeetingCancelMenuItemEvent
import mega.privacy.mobile.analytics.event.ScheduledMeetingEditMenuItemEvent

/**
 * Compose replacement for `ChatListBottomSheetDialogFragment`.
 *
 * Hosts [ChatRoomItemBottomSheetView] in a modal bottom sheet for a single chat row.
 * Side-effects that require activity-result launchers, permission requests, or shared
 * dialog state (mute/leave) are hoisted to the caller.
 *
 * @param chatId Chat to show options for.
 * @param viewModel Shared chat-tabs view model used for chat-room state and actions.
 * @param scheduledMeetingManagementViewModel Drives "cancel scheduled meeting" + monitors messages.
 * @param onDismissRequest Called when the bottom sheet should be dismissed.
 * @param onStartMeetingPressed Caller should request call permissions and start the meeting.
 * @param onEditScheduledMeeting Caller should launch CreateScheduledMeetingActivity for editing.
 * @param onPendingMeetingInfo Caller should launch ChatInfoActivity (with schedId) for a pending meeting.
 * @param onMutePressed Caller should open the mute dialog for this single chat.
 * @param onLeavePressed Caller should open the leave-confirmation dialog for this single chat.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomItemBottomSheet(
    chatId: Long,
    viewModel: ChatTabsViewModel,
    scheduledMeetingManagementViewModel: ScheduledMeetingManagementViewModel,
    onDismissRequest: () -> Unit,
    onStartMeetingPressed: () -> Unit,
    onEditScheduledMeeting: (chatId: Long) -> Unit,
    onPendingMeetingInfo: (chatId: Long, schedId: Long?) -> Unit,
    onMutePressed: (chatId: Long, isMeeting: Boolean) -> Unit,
    onLeavePressed: (chatId: Long, isMeeting: Boolean) -> Unit,
) {
    val context = LocalContext.current
    val item: ChatRoomItem? by viewModel.getChatRoom(chatId)
        .collectAsStateWithLifecycle(initialValue = null)

    DisposableEffect(chatId) {
        scheduledMeetingManagementViewModel.monitorLoadedMessages(chatId)
        onDispose {
            scheduledMeetingManagementViewModel.stopMonitoringLoadMessages()
        }
    }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.PartiallyExpanded },
    )

    MegaModalBottomSheet(
        sheetState = sheetState,
        bottomSheetBackground = MegaModalBottomSheetBackground.Surface1,
        onDismissRequest = onDismissRequest,
    ) {
        ChatRoomItemBottomSheetContent(
            item = item,
            onStartMeetingClick = {
                onStartMeetingPressed()
                onDismissRequest()
            },
            onOccurrencesClick = {
                context.startActivity(
                    Intent(context, RecurringMeetingInfoActivity::class.java).apply {
                        putExtra(ChatNavKey.LEGACY_CHAT_ID, chatId)
                    }
                )
                onDismissRequest()
            },
            onInfoClick = {
                val current = viewModel.getChatItem(chatId)
                when {
                    current is ChatRoomItem.NoteToSelfChatRoomItem -> {
                        context.startActivity(
                            Intent(context, ChatInfoActivity::class.java).apply {
                                putExtra(ChatNavKey.LEGACY_CHAT_ID, chatId)
                            }
                        )
                        onDismissRequest()
                    }

                    current is ChatRoomItem.IndividualChatRoomItem -> {
                        context.startActivity(
                            Intent(context, ContactInfoActivity::class.java).apply {
                                putExtra(Constants.NAME, current.peerEmail)
                            }
                        )
                        onDismissRequest()
                    }

                    current is ChatRoomItem.MeetingChatRoomItem
                            && current.isPending && current.isActive -> {
                        onPendingMeetingInfo(chatId, current.schedId)
                        onDismissRequest()
                    }

                    current != null -> {
                        context.startActivity(
                            Intent(context, GroupChatInfoActivity::class.java).apply {
                                putExtra(Constants.HANDLE, chatId)
                            }
                        )
                        onDismissRequest()
                    }
                }
            },
            onEditClick = {
                Analytics.tracker.trackEvent(ScheduledMeetingEditMenuItemEvent)
                onEditScheduledMeeting(chatId)
                onDismissRequest()
            },
            onClearChatClick = {
                viewModel.clearChatHistory(chatId)
                onDismissRequest()
            },
            onMuteClick = {
                onMutePressed(chatId, item is ChatRoomItem.MeetingChatRoomItem)
                onDismissRequest()
            },
            onUnmuteClick = {
                MegaApplication.getPushNotificationSettingManagement()
                    .controlMuteNotificationsOfAChat(
                        context,
                        Constants.NOTIFICATIONS_ENABLED,
                        chatId
                    )
                onDismissRequest()
            },
            onArchiveClick = { isNoteToSelfChat ->
                if (isNoteToSelfChat) {
                    Analytics.tracker.trackEvent(ArchiveNoteToSelfButtonPressedEvent)
                }
                viewModel.archiveChats(chatId)
                onDismissRequest()
            },
            onCancelClick = {
                Analytics.tracker.trackEvent(ScheduledMeetingCancelMenuItemEvent)
                viewModel.getChatItem(chatId)?.let { chatRoomItem ->
                    scheduledMeetingManagementViewModel.setChatRoomItem(chatRoomItem)
                }
                scheduledMeetingManagementViewModel.checkIfIsChatHistoryEmpty(chatId)
                onDismissRequest()
            },
            onLeaveClick = {
                onLeavePressed(chatId, item is ChatRoomItem.MeetingChatRoomItem)
                onDismissRequest()
            },
        )
    }
}

/**
 * Stateless content of [ChatRoomItemBottomSheet]. Renders [ChatRoomItemBottomSheetView]
 * with high-level callbacks so it can be exercised in `@Preview` and unit tests
 * without ViewModel or activity wiring.
 */
@Composable
internal fun ChatRoomItemBottomSheetContent(
    item: ChatRoomItem?,
    modifier: Modifier = Modifier,
    onStartMeetingClick: () -> Unit = {},
    onOccurrencesClick: () -> Unit = {},
    onInfoClick: () -> Unit = {},
    onEditClick: () -> Unit = {},
    onClearChatClick: () -> Unit = {},
    onMuteClick: () -> Unit = {},
    onUnmuteClick: () -> Unit = {},
    onArchiveClick: (Boolean) -> Unit = {},
    onCancelClick: () -> Unit = {},
    onLeaveClick: () -> Unit = {},
) {
    ChatRoomItemBottomSheetView(
        item = item,
        modifier = modifier,
        onStartMeetingClick = onStartMeetingClick,
        onOccurrencesClick = onOccurrencesClick,
        onInfoClick = onInfoClick,
        onEditClick = onEditClick,
        onClearChatClick = onClearChatClick,
        onMuteClick = onMuteClick,
        onUnmuteClick = onUnmuteClick,
        onArchiveClick = onArchiveClick,
        onCancelClick = onCancelClick,
        onLeaveClick = onLeaveClick,
    )
}

@PreviewLightDark
@Composable
private fun PreviewChatRoomItemBottomSheetContentIndividual() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        ChatRoomItemBottomSheetContent(
            item = ChatRoomItem.IndividualChatRoomItem(
                chatId = 1L,
                title = "Mieko Kawakami",
                peerEmail = "mieko@miekokawakami.jp",
                avatar = ChatAvatarItem("M"),
                hasPermissions = true,
            ),
        )
    }
}

@PreviewLightDark
@Composable
private fun PreviewChatRoomItemBottomSheetContentGroup() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        ChatRoomItemBottomSheetContent(
            item = ChatRoomItem.GroupChatRoomItem(
                chatId = 2L,
                title = "Vanuatu - Lakatoro&Lorsup (May)",
                avatars = listOf(ChatAvatarItem("L"), ChatAvatarItem("J")),
                hasPermissions = true,
                isActive = true,
            ),
        )
    }
}

@PreviewLightDark
@Composable
private fun PreviewChatRoomItemBottomSheetContentMeeting() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        ChatRoomItemBottomSheetContent(
            item = ChatRoomItem.MeetingChatRoomItem(
                chatId = 3L,
                schedId = 99L,
                title = "Photos Sprint #1",
                avatars = listOf(ChatAvatarItem("A"), ChatAvatarItem("J")),
                hasPermissions = true,
            ),
        )
    }
}

@PreviewLightDark
@Composable
private fun PreviewChatRoomItemBottomSheetContentArchived() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        ChatRoomItemBottomSheetContent(
            item = ChatRoomItem.IndividualChatRoomItem(
                chatId = 4L,
                title = "Mieko Kawakami",
                peerEmail = "mieko@miekokawakami.jp",
                avatar = ChatAvatarItem("M"),
                isArchived = true,
            ),
        )
    }
}

@PreviewLightDark
@Composable
private fun PreviewChatRoomItemBottomSheetContentEmpty() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        ChatRoomItemBottomSheetContent(item = null)
    }
}
