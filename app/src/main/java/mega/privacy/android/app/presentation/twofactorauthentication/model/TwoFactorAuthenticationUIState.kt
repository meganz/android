package mega.privacy.android.app.presentation.twofactorauthentication.model


/**
 * UI State for Two Factor authentication activity
 * @param isPinSubmitted UI state to determine if the pins got submitted or not
 * @param authenticationState UI state for enabling the two factor authentication
 * @param isMasterKeyExported UI state to change the visibility of some related views in the activity
 * @param dismissRecoveryKey UI state to change the visibility of the recovery key button
 */
data class TwoFactorAuthenticationUIState(
    val isPinSubmitted: Boolean = false,
    val authenticationState: AuthenticationState? = null,
    val isMasterKeyExported: Boolean = false,
    val dismissRecoveryKey: Boolean = false,
)


