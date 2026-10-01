package mega.privacy.android.feature.chat.managehistory.model

import androidx.compose.runtime.Stable
import de.palm.composestateevents.StateEventWithContent

/**
 * State of the manage chat history screen.
 */
@Stable
sealed interface ManageChatHistoryUiState {

    /**
     * The chat room is still being resolved.
     */
    data object Loading : ManageChatHistoryUiState

    /**
     * The chat room could not be resolved, so there is nothing to manage and the screen closes.
     */
    data object Unavailable : ManageChatHistoryUiState

    /**
     * The resolved chat room.
     *
     * @property chatId Id of the chat room whose history is being managed.
     * @property isMeeting True when the chat room is a meeting, which changes the wording.
     * @property retention How long the chat keeps its messages
     * is turned off.
     * @property historyClearedEvent Carries the outcome of a clear history request.
     */
    data class Data(
        val chatId: Long,
        val isMeeting: Boolean,
        val retention: ChatHistoryRetention,
        val historyClearedEvent: StateEventWithContent<Boolean>,
    ) : ManageChatHistoryUiState
}
