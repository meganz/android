package mega.privacy.android.app.presentation.openlink

/**
 * Open link state
 *
 * @property isLoggedIn checks if app is logged in or not
 * @property userEmail email of the user
 * @property accountInvitationEmail generated email for a new sign up account
 * @property needsRefreshSession checks if session needs to be refreshed
 * @property decodedUrl url to open
 * @property logoutCompletedEvent true if logout is completed
 * @property urlRedirectionEvent true if url redirection is needed
 * @property resetPasswordLinkResult result of reset password link
 */
data class OpenLinkUiState(
    val isLoggedIn: Boolean? = null,
    val userEmail: String? = null,
    val accountInvitationEmail: String? = null,
    val needsRefreshSession: Boolean = false,
    val decodedUrl: String? = null,
    val logoutCompletedEvent: Boolean = false,
    val urlRedirectionEvent: Boolean = false,
    val resetPasswordLinkResult: Result<String>? = null,
)
