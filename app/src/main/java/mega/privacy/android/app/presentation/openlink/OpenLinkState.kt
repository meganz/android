package mega.privacy.android.app.presentation.openlink

/**
 * Open link state
 *
 * @property isLoggedOut checks if app is logged in or not
 * @property accountInvitationEmail generated email for a new sign up account
 */
data class OpenLinkState(
    val isLoggedOut: Boolean = false,
    val accountInvitationEmail: String? = null,
)
