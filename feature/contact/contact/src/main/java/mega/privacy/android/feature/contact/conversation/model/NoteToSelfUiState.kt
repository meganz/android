package mega.privacy.android.feature.contact.conversation.model

import androidx.compose.runtime.Stable

/**
 * Note-to-self row state for the start-conversation screen.
 */
@Stable
sealed interface NoteToSelfUiState {
    /**
     * The note-to-self chat is not available yet, so no row is shown.
     */
    data object Loading : NoteToSelfUiState

    /**
     * Data
     *
     * @property chatId Id of the note-to-self chat to open.
     * @property isHint True while the chat has no messages, which renders the row as a hint.
     */
    data class Data(
        val chatId: Long,
        val isHint: Boolean,
    ) : NoteToSelfUiState
}
