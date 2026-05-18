package mega.privacy.android.feature.cloudexplorer.presentation.chatexplorer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

@Stable
class ChatExplorerSelectionState(
    initialSelectedIds: Set<Long> = emptySet(),
) {
    var selectedChatIds: Set<Long> by mutableStateOf(initialSelectedIds)
        private set

    val isInSelectionMode: Boolean
        get() = selectedChatIds.isNotEmpty()

    val selectedItemsCount: Int
        get() = selectedChatIds.size

    fun toggleSelection(chatId: Long) {
        selectedChatIds = if (chatId in selectedChatIds) {
            selectedChatIds - chatId
        } else {
            selectedChatIds + chatId
        }
    }

    fun deselectAll() {
        selectedChatIds = emptySet()
    }

    companion object {
        val Saver: Saver<ChatExplorerSelectionState, List<Long>> = Saver(
            save = { state -> state.selectedChatIds.toList() },
            restore = { longs -> ChatExplorerSelectionState(initialSelectedIds = longs.toSet()) },
        )
    }
}

@Composable
fun rememberChatExplorerSelectionState(
    initialSelectedIds: Set<Long> = emptySet(),
): ChatExplorerSelectionState =
    rememberSaveable(saver = ChatExplorerSelectionState.Saver) {
        ChatExplorerSelectionState(initialSelectedIds = initialSelectedIds)
    }
