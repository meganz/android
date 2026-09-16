package mega.privacy.android.feature.chat.list.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import mega.privacy.android.feature.chat.list.model.ChatListBulkActionsUiState
import mega.privacy.android.feature.chat.list.model.ChatRoomActionUiItem

/**
 * Renders whichever chat room actions overlay [actionsState] asks for: the actions bottom sheet
 * for the chats it names, or the confirmation dialog of a destructive action picked from it.
 *
 * The actions themselves are resolved asynchronously through [resolveActions] once the sheet is
 * asked for, and the overlay closes itself when no action applies to those chats.
 *
 * @param actionsState Overlay to render.
 * @param resolveActions Resolves the ordered actions common to the given chats.
 * @param onStateChange Called with the overlay that should be shown next.
 * @param onActionSelected Called with the chats and the picked action, once confirmed.
 */
@Composable
internal fun ChatRoomActionsOverlay(
    actionsState: ChatListBulkActionsUiState,
    resolveActions: suspend (Set<Long>) -> List<ChatRoomActionUiItem>,
    onStateChange: (ChatListBulkActionsUiState) -> Unit,
    onActionSelected: (Set<Long>, String) -> Unit,
) {
    when (actionsState) {
        ChatListBulkActionsUiState.Hidden -> Unit

        is ChatListBulkActionsUiState.ShowingActions -> {
            val chatIds = actionsState.chatIds
            var actions by remember(chatIds) {
                mutableStateOf<List<ChatRoomActionUiItem>?>(null)
            }
            LaunchedEffect(chatIds) {
                val resolved = resolveActions(chatIds)
                if (resolved.isEmpty()) {
                    onStateChange(ChatListBulkActionsUiState.Hidden)
                } else {
                    actions = resolved
                }
            }
            actions?.let { resolved ->
                ChatRoomActionsBottomSheet(
                    actions = resolved,
                    onDismiss = { onStateChange(ChatListBulkActionsUiState.Hidden) },
                    onActionClick = { action ->
                        val confirmation = action.confirmation
                        if (confirmation != null) {
                            onStateChange(
                                ChatListBulkActionsUiState.Confirming(
                                    chatIds = chatIds,
                                    actionId = action.testTag,
                                    confirmation = confirmation,
                                )
                            )
                        } else {
                            onActionSelected(chatIds, action.testTag)
                            onStateChange(ChatListBulkActionsUiState.Hidden)
                        }
                    },
                )
            }
        }

        is ChatListBulkActionsUiState.Confirming -> {
            val chatIds = actionsState.chatIds
            val actionId = actionsState.actionId
            ChatRoomActionConfirmationDialog(
                confirmation = actionsState.confirmation,
                onConfirm = {
                    onActionSelected(chatIds, actionId)
                    onStateChange(ChatListBulkActionsUiState.Hidden)
                },
                onDismiss = { onStateChange(ChatListBulkActionsUiState.Hidden) },
            )
        }
    }
}
