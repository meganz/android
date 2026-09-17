package mega.privacy.android.feature.chat.list.model

/**
 * Mutually-exclusive overlay state driving the chat room actions bottom sheet and its
 * destructive-action confirmation dialog. Only one overlay can be shown at a time, for the chats
 * it was opened for: the whole selection in multi-select mode, or the single chat whose row
 * actions affordance was pressed.
 *
 * Held with `retain` rather than `rememberSaveable`, so an overlay outlives an Activity
 * recreation but not the process. The chat rooms its actions were resolved from live in the
 * ViewModel for exactly as long, so an overlay can never come back referring to a resolution
 * that is gone.
 */
sealed interface ChatListBulkActionsUiState {

    /**
     * No overlay is shown.
     */
    data object Hidden : ChatListBulkActionsUiState

    /**
     * The bulk actions bottom sheet is shown for the chats in [chatIds].
     */
    data class ShowingActions(val chatIds: Set<Long>) : ChatListBulkActionsUiState

    /**
     * The confirmation dialog for the action [actionId] is shown for the chats in [chatIds],
     * driven by [confirmation]. A new destructive action needs no new state variant.
     */
    data class Confirming(
        val chatIds: Set<Long>,
        val actionId: String,
        val confirmation: ChatRoomMenuItemConfirmation,
    ) : ChatListBulkActionsUiState
}
