package mega.privacy.android.feature.chat.list.view

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.components.contact.state.ContactItemStatus
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.feature.chat.list.model.ChatRoomUiItem

/**
 * Screenshot tests for the online-status indicator on a peer chat list row.
 */
class ChatRoomItemViewScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ChatRoomItemOnline() {
        RowWithStatus(ContactItemStatus.Online)
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ChatRoomItemAway() {
        RowWithStatus(ContactItemStatus.Away)
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ChatRoomItemBusy() {
        RowWithStatus(ContactItemStatus.Busy)
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ChatRoomItemOffline() {
        RowWithStatus(ContactItemStatus.Offline)
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ChatRoomItemOngoingCall() {
        RowWithStatus(ContactItemStatus.Online, hasOngoingCall = true)
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ChatRoomItemRecurringMeeting() {
        AndroidThemeForPreviews {
            ChatRoomItemView(
                item = meetingRow(
                    pendingMeeting = ChatRoomUiItem.PendingMeeting.Recurring(
                        scheduledTime = "10:00am - 11:00am",
                        recurrence = ChatRoomUiItem.PendingMeeting.Recurrence.Weekly,
                    ),
                    showRecurringIcon = true,
                ),
                onItemClick = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ChatRoomItemUpcomingMeeting() {
        AndroidThemeForPreviews {
            ChatRoomItemView(
                item = meetingRow(
                    pendingMeeting = ChatRoomUiItem.PendingMeeting.OneOff(
                        scheduledTime = "10:00am - 11:00am",
                    ),
                    showRecurringIcon = false,
                ),
                onItemClick = {},
            )
        }
    }

    @Composable
    private fun RowWithStatus(status: ContactItemStatus, hasOngoingCall: Boolean = false) {
        AndroidThemeForPreviews {
            ChatRoomItemView(
                item = ChatRoomUiItem(
                    chatId = 1L,
                    title = "Mieko Kawakami",
                    lastMessage = "See you tomorrow!",
                    lastTimestampFormatted = "Today 14:25",
                    header = null,
                    pendingMeeting = null,
                    showRecurringIcon = false,
                    unreadCount = 5,
                    isMuted = false,
                    highlight = true,
                    isNoteToSelf = false,
                    avatar = ChatRoomUiItem.ChatRoomUiAvatar.Peer(
                        placeholderText = "M",
                        filePath = null,
                        color = 0xFFFEBC00.toInt(),
                    ),
                    status = status,
                    hasOngoingCall = hasOngoingCall,
                ),
                onItemClick = {},
            )
        }
    }

    private fun meetingRow(
        pendingMeeting: ChatRoomUiItem.PendingMeeting,
        showRecurringIcon: Boolean,
    ) = ChatRoomUiItem(
        chatId = 2L,
        title = "Weekly sync",
        lastMessage = null,
        lastTimestampFormatted = "1 May 2022 17:53",
        header = null,
        pendingMeeting = pendingMeeting,
        showRecurringIcon = showRecurringIcon,
        unreadCount = 0,
        isMuted = false,
        highlight = false,
        isNoteToSelf = false,
        avatar = ChatRoomUiItem.ChatRoomUiAvatar.Meeting,
        status = ContactItemStatus.Unknown,
        hasOngoingCall = false,
    )
}
