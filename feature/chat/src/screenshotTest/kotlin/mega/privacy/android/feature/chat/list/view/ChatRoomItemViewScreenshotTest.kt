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

    @Composable
    private fun RowWithStatus(status: ContactItemStatus) {
        AndroidThemeForPreviews {
            ChatRoomItemView(
                item = ChatRoomUiItem(
                    chatId = 1L,
                    title = "Mieko Kawakami",
                    lastMessage = "See you tomorrow!",
                    lastTimestampFormatted = "Today 14:25",
                    scheduledTimestampFormatted = null,
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
                ),
                onItemClick = {},
            )
        }
    }
}
