package mega.privacy.android.feature.chat.list.model

import androidx.compose.runtime.Stable
import mega.privacy.android.domain.entity.chat.ChatStatus
import mega.privacy.android.domain.entity.chat.MeetingTooltipItem

/**
 * UI state for the chat list screen.
 */
@Stable
sealed interface ChatListUiState {

    /**
     * Chats and meetings are still loading for the first time.
     */
    data object Loading : ChatListUiState

    /**
     * Chats and meetings are loaded.
     *
     * @property chats Content of the Chats tab, already reflecting the active search query.
     * @property meetings Content of the Meetings tab, already reflecting the active search query.
     * @property chatsHaveUnread Whether the Chats tab has any unread chat room, driving its tab
     * badge. Independent of the active search query.
     * @property meetingsHaveUnread Whether the Meetings tab has any unread meeting, driving its tab
     * badge. Independent of the active search query.
     * @property hasAnyContact Whether the user has at least one MEGA contact, driving the call to
     * action offered on the empty Chats tab.
     * @property hasArchivedChats Whether the user has any archived chat, driving the archived
     * chats toolbar action.
     * @property isSearchAvailable Whether there is anything worth searching, driving the search
     * toolbar action. Independent of the active search query.
     * @property chatStatus Presence the user is currently publishing, shown as the toolbar
     * subtitle; null while it is still being resolved.
     * @property meetingTooltip Onboarding tooltip still owed to the user on the Meetings tab.
     */
    data class Data(
        val chats: ChatListTabState,
        val meetings: ChatListTabState,
        val chatsHaveUnread: Boolean,
        val meetingsHaveUnread: Boolean,
        val hasAnyContact: Boolean,
        val hasArchivedChats: Boolean,
        val isSearchAvailable: Boolean,
        val chatStatus: ChatStatus?,
        val meetingTooltip: MeetingTooltipItem,
    ) : ChatListUiState
}
