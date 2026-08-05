package mega.privacy.android.feature.chat.list.view

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import kotlinx.collections.immutable.persistentListOf
import mega.android.core.ui.components.contact.state.ContactItemStatus
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.feature.chat.list.model.ChatListUiState
import mega.privacy.android.feature.chat.list.model.ChatRoomUiItem

/**
 * Screenshot tests for [ChatListScreen], covering the Chats and Meetings tabs,
 * the empty state, and the loading skeleton.
 */
class ChatListScreenScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ChatListScreenChatsTab() {
        AndroidThemeForPreviews {
            ChatListScreen(
                uiState = populatedState(),
                showMeetingTab = false,
                onBackClick = {},
                onItemClick = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ChatListScreenMeetingsTab() {
        AndroidThemeForPreviews {
            ChatListScreen(
                uiState = populatedState(),
                showMeetingTab = true,
                onBackClick = {},
                onItemClick = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ChatListScreenEmpty() {
        AndroidThemeForPreviews {
            ChatListScreen(
                uiState = ChatListUiState.Data(
                    chats = persistentListOf(),
                    meetings = persistentListOf(),
                ),
                showMeetingTab = false,
                onBackClick = {},
                onItemClick = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ChatListScreenMeetingsEmpty() {
        AndroidThemeForPreviews {
            ChatListScreen(
                uiState = ChatListUiState.Data(
                    chats = persistentListOf(),
                    meetings = persistentListOf(),
                ),
                showMeetingTab = true,
                onBackClick = {},
                onItemClick = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ChatListScreenLoading() {
        AndroidThemeForPreviews {
            ChatListScreen(
                uiState = ChatListUiState.Loading,
                showMeetingTab = false,
                onBackClick = {},
                onItemClick = {},
            )
        }
    }

    private fun populatedState() = ChatListUiState.Data(
        chats = persistentListOf(
            ChatRoomUiItem(
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
                status = ContactItemStatus.Online,
            ),
            ChatRoomUiItem(
                chatId = 2L,
                title = "Recipe test #14",
                lastMessage = "Anna: Seeya all soon!",
                lastTimestampFormatted = "1 May 2022 17:53",
                scheduledTimestampFormatted = null,
                unreadCount = 0,
                isMuted = true,
                highlight = false,
                isNoteToSelf = false,
                avatar = ChatRoomUiItem.ChatRoomUiAvatar.Group,
                status = ContactItemStatus.Unknown,
            ),
        ),
        meetings = persistentListOf(
            ChatRoomUiItem(
                chatId = 3L,
                title = "Weekly sync",
                lastMessage = null,
                lastTimestampFormatted = "1 May 2022 17:53",
                scheduledTimestampFormatted = "10:00am - 11:00am",
                unreadCount = 0,
                isMuted = false,
                highlight = false,
                isNoteToSelf = false,
                avatar = ChatRoomUiItem.ChatRoomUiAvatar.Meeting,
                status = ContactItemStatus.Unknown,
            ),
            ChatRoomUiItem(
                chatId = 4L,
                title = "Monthly retro",
                lastMessage = "Bob: The meeting has started",
                lastTimestampFormatted = "Today 09:00",
                scheduledTimestampFormatted = null,
                unreadCount = 2,
                isMuted = false,
                highlight = true,
                isNoteToSelf = false,
                avatar = ChatRoomUiItem.ChatRoomUiAvatar.Meeting,
                status = ContactItemStatus.Unknown,
            ),
        ),
    )
}
