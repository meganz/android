package mega.privacy.android.app.presentation.verifytwofactor.model

import androidx.annotation.StringRes

/**
 * Terminal result emitted when a verification request finishes.
 * Drives the result-dialog shown on top of the screen before the activity finishes.
 */
sealed interface VerifyTwoFactorResult {
    data object EmailChangeLinkSent : VerifyTwoFactorResult
    data object EmailAlreadyInUse : VerifyTwoFactorResult
    data object EmailChangeAlreadyRequested : VerifyTwoFactorResult
    data object CancelAccountLinkSent : VerifyTwoFactorResult
    data object MultiFactorAuthDisabled : VerifyTwoFactorResult
    data class GenericError(@StringRes val titleResId: Int) : VerifyTwoFactorResult
}
