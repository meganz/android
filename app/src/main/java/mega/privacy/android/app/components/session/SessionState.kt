package mega.privacy.android.app.components.session

/**
 * Session state
 *
 * @property isRootNodeExists whether root node exists
 * @property isChatSessionValid whether chat session is valid
 */
internal data class SessionState(
    val isRootNodeExists: Boolean? = null,
    val isChatSessionValid: Boolean = false,
)