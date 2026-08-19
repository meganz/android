package mega.privacy.android.feature.chat.list.model

import androidx.compose.runtime.Stable

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
     */
    data class Data(
        val chats: ChatListTabState,
        val meetings: ChatListTabState,
    ) : ChatListUiState
}
