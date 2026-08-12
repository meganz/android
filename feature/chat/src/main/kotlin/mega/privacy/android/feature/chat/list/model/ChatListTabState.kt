package mega.privacy.android.feature.chat.list.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList

/**
 * Content state of a single chat list tab (Chats or Meetings).
 */
@Immutable
sealed interface ChatListTabState {

    /**
     * The tab has chat rooms to display.
     *
     * @property items Chat rooms to show, already filtered by the active search query.
     */
    data class Results(
        val items: ImmutableList<ChatRoomUiItem>,
    ) : ChatListTabState

    /**
     * A search query is active but no chat room in the tab matches it.
     */
    data object NoSearchResults : ChatListTabState

    /**
     * The tab has no chat rooms at all, with no active search.
     */
    data object Empty : ChatListTabState
}
