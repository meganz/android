package mega.privacy.android.app.components.chatsession

/**
 * Session state
 */
internal sealed interface ChatSessionState {
    data object Pending : ChatSessionState
    data object Valid : ChatSessionState
    data object Invalid : ChatSessionState
}

/**
 * Chat session container state
 *
 * @property sessionState the chat session state
 * @property isSingleActivityEnabled whether single activity feature is enabled
 */
internal data class ChatSessionUiState(
    val sessionState: ChatSessionState = ChatSessionState.Pending,
    val isSingleActivityEnabled: Boolean = false
)