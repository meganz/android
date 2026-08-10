package mega.privacy.android.app.presentation.verifytwofactor.model

/**
 * Follow-up navigation after a successful 2FA-gated password change.
 *
 * The logout branch is handled via [VerifyTwoFactorUiState.logoutEvent] directly,
 * so this sealed interface only carries the MyAccount navigation case.
 */
sealed interface PasswordChangedAction {
    data class NavigateToMyAccount(val resultCode: Int) : PasswordChangedAction
}
