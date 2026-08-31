package mega.privacy.android.feature.chat.list.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/**
 * Compose-owned selection state for the chat list multi-select mode. Chats are keyed by their
 * stable chat id, so a selected chat stays selected across tab switches and search/filter changes
 * and reappears selected when the filter clears.
 *
 * Selection lives in the UI layer rather than the ViewModel so it is naturally scoped to the
 * screen and survives configuration changes and process death through its [Saver].
 *
 * @param initialSelectedChatIds Chat ids to pre-select.
 */
@Stable
class ChatListSelectionState(
    initialSelectedChatIds: Set<Long> = emptySet(),
) {
    var selectedChatIds: Set<Long> by mutableStateOf(initialSelectedChatIds)
        private set

    /**
     * Whether the screen is currently in multi-select mode.
     */
    val isInSelectionMode: Boolean
        get() = selectedChatIds.isNotEmpty()

    /**
     * Number of currently selected chats.
     */
    val selectedCount: Int
        get() = selectedChatIds.size

    /**
     * Whether the chat with [chatId] is currently selected.
     */
    fun isSelected(chatId: Long): Boolean = chatId in selectedChatIds

    /**
     * Toggle the selection of the chat with the given [chatId]; the first toggle enters selection
     * mode and clearing the last selection leaves it.
     */
    fun toggleSelection(chatId: Long) {
        selectedChatIds = if (chatId in selectedChatIds) {
            selectedChatIds - chatId
        } else {
            selectedChatIds + chatId
        }
    }

    /**
     * Select every chat in [chatIds], replacing the current selection.
     */
    fun selectAll(chatIds: Set<Long>) {
        selectedChatIds = chatIds
    }

    /**
     * Clear the current selection, leaving selection mode.
     */
    fun deselectAll() {
        selectedChatIds = emptySet()
    }

    companion object {

        /**
         * [Saver] persisting the selection across configuration changes and process death.
         */
        val Saver: Saver<ChatListSelectionState, List<Long>> = Saver(
            save = { state -> state.selectedChatIds.toList() },
            restore = { saved -> ChatListSelectionState(initialSelectedChatIds = saved.toSet()) },
        )
    }
}

/**
 * Remember a [ChatListSelectionState] that survives recomposition and configuration changes.
 *
 * @param initialSelectedChatIds Chat ids to pre-select.
 */
@Composable
fun rememberChatListSelectionState(
    initialSelectedChatIds: Set<Long> = emptySet(),
): ChatListSelectionState =
    rememberSaveable(saver = ChatListSelectionState.Saver) {
        ChatListSelectionState(initialSelectedChatIds = initialSelectedChatIds)
    }
