package mega.privacy.android.app.presentation.verifytwofactor.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.verifytwofactor.model.VerifyTwoFactorResult
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import mega.privacy.android.shared.resources.R as sharedR

internal const val VERIFY_2FA_RESULT_DIALOG_TAG = "verify_two_factor_screen:result_dialog"

/**
 * Maps a [VerifyTwoFactorResult] to the matching [BasicDialog].
 *
 * @param result The result the ViewModel emitted after the verification request finished.
 * @param onDismiss Called when the positive button is tapped; the activity finishes after.
 */
@Composable
fun VerifyTwoFactorResultDialog(
    result: VerifyTwoFactorResult,
    onDismiss: () -> Unit,
) {
    val title = titleResFor(result)?.let { stringResource(it) }.orEmpty()
    val description = stringResource(descriptionResFor(result))
    BasicDialog(
        modifier = Modifier.testTag(VERIFY_2FA_RESULT_DIALOG_TAG),
        title = title,
        description = description,
        positiveButtonText = stringResource(sharedR.string.general_ok),
        onPositiveButtonClicked = onDismiss,
        dismissOnClickOutside = false,
        dismissOnBackPress = false,
    )
}

private fun titleResFor(result: VerifyTwoFactorResult): Int? = when (result) {
    VerifyTwoFactorResult.EmailChangeLinkSent,
    VerifyTwoFactorResult.EmailAlreadyInUse,
    VerifyTwoFactorResult.EmailChangeAlreadyRequested,
    VerifyTwoFactorResult.CancelAccountLinkSent -> R.string.email_verification_title

    VerifyTwoFactorResult.MultiFactorAuthDisabled -> null
    is VerifyTwoFactorResult.GenericError -> result.titleResId.takeIf { it != INVALID_VALUE }
}

private fun descriptionResFor(result: VerifyTwoFactorResult): Int = when (result) {
    VerifyTwoFactorResult.EmailChangeLinkSent -> R.string.email_verification_text_change_mail
    VerifyTwoFactorResult.EmailAlreadyInUse -> R.string.mail_already_used
    VerifyTwoFactorResult.EmailChangeAlreadyRequested -> R.string.mail_changed_confirm_requested
    VerifyTwoFactorResult.CancelAccountLinkSent -> R.string.email_verification_text
    VerifyTwoFactorResult.MultiFactorAuthDisabled -> R.string.label_2fa_disabled
    is VerifyTwoFactorResult.GenericError -> sharedR.string.general_text_error
}
