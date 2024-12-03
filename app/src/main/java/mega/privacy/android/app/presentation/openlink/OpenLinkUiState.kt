package mega.privacy.android.app.presentation.openlink

/**
 * Open link state
 *
 * @property isLoggedIn checks if app is logged in or not
 * @property accountInvitationEmail generated email for a new sign up account
 * @property needsRefreshSession checks if session needs to be refreshed
 * @property decodedUrl url to open
 * @property logoutCompletedEvent true if logout is completed
 * @property urlRedirectionEvent true if url redirection is needed
 */
data class OpenLinkUiState(
    val isLoggedIn: Boolean? = null,
    val accountInvitationEmail: String? = null,
    val needsRefreshSession: Boolean = false,
    val decodedUrl: String? = null,
    val logoutCompletedEvent: Boolean = false,
    val urlRedirectionEvent: Boolean = false
)
