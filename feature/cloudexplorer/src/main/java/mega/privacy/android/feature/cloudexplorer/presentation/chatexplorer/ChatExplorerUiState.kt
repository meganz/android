package mega.privacy.android.feature.cloudexplorer.presentation.chatexplorer

import androidx.compose.runtime.Stable
import de.palm.composestateevents.StateEventWithContent
import mega.privacy.android.feature.cloudexplorer.presentation.explorer.TabSignal
import mega.privacy.android.shared.chats.model.ChatExplorerUiItem

@Stable
sealed interface ChatExplorerUiState {

    data object Loading : ChatExplorerUiState

    data class Data(
        val items: Items,
        val newChatCreatedEvent: StateEventWithContent<Long>,
        val chatsReadyToShareEvent: StateEventWithContent<List<Long>>,
        val searchResults: Items,
    ) : ChatExplorerUiState {

        val isEmpty: Boolean
            get() = items.isEmpty
    }

    data class Items(
        val noteToSelf: ChatExplorerUiItem?,
        val recents: List<ChatExplorerUiItem>,
        val others: List<ChatExplorerUiItem>,
    ) {
        val isEmpty: Boolean
            get() = recents.isEmpty() && others.isEmpty()

        companion object {
            val Empty = Items(noteToSelf = null, recents = emptyList(), others = emptyList())
        }
    }
}

internal fun ChatExplorerUiState.toTabSignal(): TabSignal =
    when (this) {
        ChatExplorerUiState.Loading -> TabSignal(isLoading = true)
        is ChatExplorerUiState.Data -> TabSignal(
            isLoading = false,
            hasContent = !isEmpty,
        )
    }

internal fun ChatExplorerUiItem.withSelected(isSelected: Boolean): ChatExplorerUiItem =
    when (this) {
        is ChatExplorerUiItem.NoteToSelf -> copy(isSelected = isSelected)
        is ChatExplorerUiItem.GroupChat -> copy(isSelected = isSelected)
        is ChatExplorerUiItem.Meeting -> copy(isSelected = isSelected)
        is ChatExplorerUiItem.OneToOneChat -> copy(isSelected = isSelected)
        is ChatExplorerUiItem.Contact -> copy(isSelected = isSelected)
    }
