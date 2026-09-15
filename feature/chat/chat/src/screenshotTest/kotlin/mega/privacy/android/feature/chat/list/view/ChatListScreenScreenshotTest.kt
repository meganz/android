package mega.privacy.android.feature.chat.list.view

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import kotlinx.collections.immutable.persistentListOf
import mega.android.core.ui.components.contact.state.ContactItemStatus
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.feature.chat.list.model.ChatListTabState
import mega.privacy.android.feature.chat.list.model.ChatListUiState
import mega.privacy.android.feature.chat.list.model.ChatRoomUiItem

/**
 * Screenshot tests for [ChatListScreen], covering the Chats and Meetings tabs,
 * the empty state, the loading skeleton, active search, and no search results.
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
                onNewChatClick = {},
                onMeetingActionClick = {},
                onArchivedClick = {},
                onSearchQueryChange = {},
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
                onNewChatClick = {},
                onMeetingActionClick = {},
                onArchivedClick = {},
                onSearchQueryChange = {},
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
                    chats = ChatListTabState.Empty,
                    meetings = ChatListTabState.Empty,
                    chatsHaveUnread = false,
                    meetingsHaveUnread = false,
                ),
                showMeetingTab = false,
                onBackClick = {},
                onItemClick = {},
                onNewChatClick = {},
                onMeetingActionClick = {},
                onArchivedClick = {},
                onSearchQueryChange = {},
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
                    chats = ChatListTabState.Empty,
                    meetings = ChatListTabState.Empty,
                    chatsHaveUnread = false,
                    meetingsHaveUnread = false,
                ),
                showMeetingTab = true,
                onBackClick = {},
                onItemClick = {},
                onNewChatClick = {},
                onMeetingActionClick = {},
                onArchivedClick = {},
                onSearchQueryChange = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ChatListScreenSearchActive() {
        AndroidThemeForPreviews {
            ChatListScreenContent(
                uiState = populatedState(),
                showMeetingTab = false,
                searchMode = true,
                searchQuery = "Re",
                onBackClick = {},
                onItemClick = {},
                onNewChatClick = {},
                onMeetingFabClick = {},
                onArchivedClick = {},
                onSearchModeChange = {},
                onQueryChange = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ChatListScreenSearchNoResults() {
        AndroidThemeForPreviews {
            ChatListScreenContent(
                uiState = ChatListUiState.Data(
                    chats = ChatListTabState.NoSearchResults,
                    meetings = ChatListTabState.NoSearchResults,
                    chatsHaveUnread = false,
                    meetingsHaveUnread = false,
                ),
                showMeetingTab = false,
                searchMode = true,
                searchQuery = "zzz",
                onBackClick = {},
                onItemClick = {},
                onNewChatClick = {},
                onMeetingFabClick = {},
                onArchivedClick = {},
                onSearchModeChange = {},
                onQueryChange = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ChatListScreenSelectionMode() {
        AndroidThemeForPreviews {
            ChatListScreenContent(
                uiState = populatedState(),
                showMeetingTab = false,
                searchMode = false,
                searchQuery = "",
                onBackClick = {},
                onItemClick = {},
                onNewChatClick = {},
                onMeetingFabClick = {},
                onArchivedClick = {},
                onSearchModeChange = {},
                onQueryChange = {},
                selectionMode = true,
                selectedChatIds = setOf(1L),
                selectedCount = 1,
                isAllSelected = false,
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
                onNewChatClick = {},
                onMeetingActionClick = {},
                onArchivedClick = {},
                onSearchQueryChange = {},
            )
        }
    }

    private fun populatedState() = ChatListUiState.Data(
        chats = ChatListTabState.Results(
            persistentListOf(
                ChatRoomUiItem(
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
                    status = ContactItemStatus.Online,
                    hasOngoingCall = false,
                ),
                ChatRoomUiItem(
                    chatId = 2L,
                    title = "Recipe test #14",
                    lastMessage = "Anna: Seeya all soon!",
                    lastTimestampFormatted = "1 May 2022 17:53",
                    header = null,
                    pendingMeeting = null,
                    showRecurringIcon = false,
                    unreadCount = 0,
                    isMuted = true,
                    highlight = false,
                    isNoteToSelf = false,
                    avatar = ChatRoomUiItem.ChatRoomUiAvatar.Group,
                    status = ContactItemStatus.Unknown,
                    hasOngoingCall = false,
                ),
            ),
        ),
        meetings = ChatListTabState.Results(
            persistentListOf(
                ChatRoomUiItem(
                    chatId = 3L,
                    title = "Weekly sync",
                    lastMessage = null,
                    lastTimestampFormatted = "1 May 2022 17:53",
                    header = "Today",
                    pendingMeeting = ChatRoomUiItem.PendingMeeting.Recurring(
                        scheduledTime = "10:00am - 11:00am",
                        recurrence = ChatRoomUiItem.PendingMeeting.Recurrence.Weekly,
                    ),
                    showRecurringIcon = true,
                    unreadCount = 2,
                    isMuted = false,
                    highlight = false,
                    isNoteToSelf = false,
                    avatar = ChatRoomUiItem.ChatRoomUiAvatar.Meeting,
                    status = ContactItemStatus.Unknown,
                    hasOngoingCall = false,
                ),
                ChatRoomUiItem(
                    chatId = 4L,
                    title = "Design review",
                    lastMessage = null,
                    lastTimestampFormatted = "Friday 15:00",
                    header = null,
                    pendingMeeting = ChatRoomUiItem.PendingMeeting.OneOff(
                        scheduledTime = "2:00pm - 3:00pm",
                    ),
                    showRecurringIcon = false,
                    unreadCount = 0,
                    isMuted = false,
                    highlight = false,
                    isNoteToSelf = false,
                    avatar = ChatRoomUiItem.ChatRoomUiAvatar.Meeting,
                    status = ContactItemStatus.Unknown,
                    hasOngoingCall = false,
                ),
                ChatRoomUiItem(
                    chatId = 5L,
                    title = "Monthly retro",
                    lastMessage = "Bob: Thanks everyone!",
                    lastTimestampFormatted = "1 May 2022 17:53",
                    header = "Past meetings",
                    pendingMeeting = null,
                    showRecurringIcon = false,
                    unreadCount = 0,
                    isMuted = false,
                    highlight = false,
                    isNoteToSelf = false,
                    avatar = ChatRoomUiItem.ChatRoomUiAvatar.Meeting,
                    status = ContactItemStatus.Unknown,
                    hasOngoingCall = false,
                ),
            ),
        ),
        chatsHaveUnread = true,
        meetingsHaveUnread = true,
    )
}
