package mega.privacy.android.app.presentation.account.business

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import mega.privacy.android.app.R
import mega.privacy.android.shared.original.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.shared.original.core.ui.preview.BooleanProvider
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme

/**
 * A Composable Dialog informing the Business User that his/her Account has been suspended. The
 * Dialog also explains the procedure to lift the Account suspension
 *
 * @param isBusinessAdministratorAccount true if the suspended Business Account belongs to an
 * Administrator
 * @param onAlertAcknowledged Lambda to execute when the Account suspension has been acknowledged
 * @param onAlertDismissed Lambda to execute when the Dialog is dismissed
 * @param modifier The [Modifier] class
 */
@Composable
fun BusinessAccountSuspendedDialog(
    isBusinessAdministratorAccount: Boolean,
    onAlertAcknowledged: () -> Unit,
    onAlertDismissed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MegaAlertDialog(
        modifier = modifier.testTag(BUSINESS_ACCOUNT_SUSPENDED_DIALOG),
        title = stringResource(R.string.account_business_account_deactivated_dialog_title),
        text = stringResource(
            if (isBusinessAdministratorAccount) {
                R.string.account_business_account_deactivated_dialog_admin_body
            } else {
                R.string.account_business_account_deactivated_dialog_sub_user_body
            }
        ),
        confirmButtonText = stringResource(R.string.account_business_account_deactivated_dialog_button),
        onConfirm = onAlertAcknowledged,
        cancelButtonText = null,
        onDismiss = onAlertDismissed,
    )
}

/**
 * A Preview [Composable] for [BusinessAccountSuspendedDialog]
 *
 * @param isBusinessAdministratorAccount true if the suspended Business Account belongs to an
 * Administrator
 */
@CombinedThemePreviews
@Composable
private fun BusinessAccountSuspendedDialogPreview(
    @PreviewParameter(BooleanProvider::class) isBusinessAdministratorAccount: Boolean,
) {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        BusinessAccountSuspendedDialog(
            isBusinessAdministratorAccount = isBusinessAdministratorAccount,
            onAlertAcknowledged = {},
            onAlertDismissed = {},
        )
    }
}

/**
 * Test Tag for the Business Account Suspended Dialog
 */
internal const val BUSINESS_ACCOUNT_SUSPENDED_DIALOG =
    "business_account_suspended_dialog:mega_alert_dialog"