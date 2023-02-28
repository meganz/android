package mega.privacy.android.app.presentation.chat.model

/**
 * Chat UI state
 *
 * @property error              String resource id for showing an error.
 * @property isCallAnswered     Handle when a call is answered.
 * @property isChatInitialised  True, if the chat is initialised. False, if not.
 */
data class ChatState(
    val error: Int? = null,
    val isCallAnswered: Boolean = false,
    val isChatInitialised: Boolean = false,
)