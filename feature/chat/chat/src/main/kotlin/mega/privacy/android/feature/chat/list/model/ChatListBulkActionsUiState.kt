package mega.privacy.android.feature.chat.list.model

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver

/**
 * Mutually-exclusive overlay state driving the chat list bulk actions bottom sheet and its
 * destructive-action confirmation dialog while in selection mode. Only one overlay can be shown
 * at a time, for the set of currently selected chats.
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

    companion object {

        /**
         * [Saver] persisting the overlay across configuration changes and process death.
         */
        val Saver: Saver<ChatListBulkActionsUiState, *> = listSaver(
            save = { state ->
                when (state) {
                    Hidden -> listOf(HIDDEN_TAG)
                    is ShowingActions -> listOf(SHOWING_TAG) + state.chatIds.toList()
                    is Confirming -> listOf(
                        CONFIRMING_TAG,
                        state.actionId,
                        state.confirmation.title,
                        state.confirmation.message,
                        state.confirmation.confirmButtonText,
                        state.confirmation.testTag,
                    ) + state.chatIds.toList()
                }
            },
            restore = { saved ->
                when (saved.first()) {
                    SHOWING_TAG -> ShowingActions(
                        chatIds = saved.drop(1).map { it as Long }.toSet(),
                    )

                    CONFIRMING_TAG -> Confirming(
                        actionId = saved[1] as String,
                        confirmation = ChatRoomMenuItemConfirmation(
                            title = saved[2] as Int,
                            message = saved[3] as Int,
                            confirmButtonText = saved[4] as Int,
                            testTag = saved[5] as String,
                        ),
                        chatIds = saved.drop(6).map { it as Long }.toSet(),
                    )

                    else -> Hidden
                }
            },
        )

        private const val HIDDEN_TAG = "hidden"
        private const val SHOWING_TAG = "showing"
        private const val CONFIRMING_TAG = "confirming"
    }
}
