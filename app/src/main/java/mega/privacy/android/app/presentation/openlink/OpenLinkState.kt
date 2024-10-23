package mega.privacy.android.app.presentation.openlink

/**
 * Open link state
 *
 * @property isLoggedIn checks if app is logged in or not
 * @property isLogoutCompleted checks if logout is completed
 * @property accountInvitationEmail generated email for a new sign up account
 * @property needsRefreshSession checks if session needs to be refreshed
 * @property decodedUrl url to open
 */
data class OpenLinkState(
    val isLoggedIn: Boolean? = null,
    val isLogoutCompleted: Boolean = false,
    val accountInvitationEmail: String? = null,
    val needsRefreshSession: Boolean = false,
    val decodedUrl: String? = null,
)
