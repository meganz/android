package mega.privacy.android.feature.chat.list.model

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver

/**
 * Mutually-exclusive overlay state driving the chat list item actions bottom sheet and its
 * destructive-action confirmation dialog. Only one overlay can be shown at a time.
 */
sealed interface ChatRoomActionsUiState {

    /**
     * No overlay is shown.
     */
    data object Hidden : ChatRoomActionsUiState

    /**
     * The actions bottom sheet is shown for the chat with [chatId].
     */
    data class ShowingActions(val chatId: Long) : ChatRoomActionsUiState

    /**
     * The confirmation dialog for the action [actionId] is shown for the chat with [chatId],
     * driven by [confirmation]. A new destructive action needs no new state variant.
     */
    data class Confirming(
        val chatId: Long,
        val actionId: String,
        val confirmation: ChatRoomMenuItemConfirmation,
    ) : ChatRoomActionsUiState

    companion object {

        /**
         * [Saver] persisting the overlay across configuration changes and process death.
         */
        val Saver: Saver<ChatRoomActionsUiState, *> = listSaver(
            save = { state ->
                when (state) {
                    Hidden -> listOf(HIDDEN_TAG)
                    is ShowingActions -> listOf(SHOWING_TAG, state.chatId)
                    is Confirming -> listOf(
                        CONFIRMING_TAG,
                        state.chatId,
                        state.actionId,
                        state.confirmation.title,
                        state.confirmation.message,
                        state.confirmation.confirmButtonText,
                        state.confirmation.testTag,
                    )
                }
            },
            restore = { saved ->
                when (saved.first()) {
                    SHOWING_TAG -> ShowingActions(saved[1] as Long)
                    CONFIRMING_TAG -> Confirming(
                        chatId = saved[1] as Long,
                        actionId = saved[2] as String,
                        confirmation = ChatRoomMenuItemConfirmation(
                            title = saved[3] as Int,
                            message = saved[4] as Int,
                            confirmButtonText = saved[5] as Int,
                            testTag = saved[6] as String,
                        ),
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
