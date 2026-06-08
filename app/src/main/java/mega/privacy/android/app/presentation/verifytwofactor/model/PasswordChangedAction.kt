package mega.privacy.android.app.presentation.verifytwofactor.model

/**
 * Follow-up action after a successful 2FA-gated password change.
 */
sealed interface PasswordChangedAction {
    data object Logout : PasswordChangedAction
    data class NavigateToMyAccount(val resultCode: Int) : PasswordChangedAction
}
