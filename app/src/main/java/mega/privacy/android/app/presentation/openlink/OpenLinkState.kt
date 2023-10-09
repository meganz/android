package mega.privacy.android.app.presentation.openlink

/**
 * Open link state
 *
 * @property isLoggedOut checks if app is logged in or not
 */
data class OpenLinkState(
    val isLoggedOut: Boolean = false,
)