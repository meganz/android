package mega.privacy.android.app.presentation.twofactorauthentication.model


/**
 * UI State for Two Factor authentication activity
 * @param isPinSubmitted UI state to determine if the pins got submitted or not
 * @param authenticationState UI State for enabling/disabling the two factor authentication
 */
data class TwoFactorAuthenticationUIState(
    val isPinSubmitted: Boolean = false,
    val authenticationState: AuthenticationState? = null,
)


