package mega.privacy.android.app.components.chatsession

/**
 * Session state
 */
internal sealed interface ChatSessionState {
    data object Pending : ChatSessionState
    data object Valid : ChatSessionState
    data object Invalid : ChatSessionState
}