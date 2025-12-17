package mega.privacy.android.app.components.session

/**
 * Session state
 *
 * @property doesRootNodeExist whether root node exists
 * @property isSingleActivityEnabled whether single activity feature is enabled
 */
internal data class SessionState(
    val doesRootNodeExist: Boolean? = null,
    val isSingleActivityEnabled: Boolean = false
)