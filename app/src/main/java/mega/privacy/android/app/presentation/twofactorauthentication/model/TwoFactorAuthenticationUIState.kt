package mega.privacy.android.app.presentation.twofactorauthentication.model


/**
 * UI State for Two Factor authentication activity
 * @param is2FAFetchCompleted UI state to decide if fetching the 2FA code was completed or not
 * @param seed The seed that show in the UI as 13 unique codes
 * @param isPinSubmitted UI state to determine if the pins got submitted or not
 * @param authenticationState UI state for enabling the two factor authentication
 * @param isMasterKeyExported UI state to change the visibility of some related views in the activity
 * @param dismissRecoveryKey UI state to change the visibility of the recovery key button
 */
data class TwoFactorAuthenticationUIState(
    val is2FAFetchCompleted: Boolean = false,
    val seed: String? = null,
    val isPinSubmitted: Boolean = false,
    val authenticationState: AuthenticationState? = null,
    val isMasterKeyExported: Boolean = false,
    val dismissRecoveryKey: Boolean = false,
)


