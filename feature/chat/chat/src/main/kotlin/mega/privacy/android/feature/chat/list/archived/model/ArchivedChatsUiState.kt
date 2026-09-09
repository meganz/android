package mega.privacy.android.feature.chat.list.archived.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import mega.privacy.android.feature.chat.list.model.ChatRoomUiItem

/**
 * UI state for the archived chats screen.
 */
@Immutable
sealed interface ArchivedChatsUiState {

    /**
     * Archived chats are still loading for the first time.
     */
    data object Loading : ArchivedChatsUiState

    /**
     * There are no archived chats.
     */
    data object Empty : ArchivedChatsUiState

    /**
     * Archived chats are loaded.
     *
     * @property items Archived chat rooms to show.
     */
    data class Data(
        val items: ImmutableList<ChatRoomUiItem>,
    ) : ArchivedChatsUiState
}
