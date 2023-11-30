package mega.privacy.android.app.presentation.transfers.view

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.shared.theme.MegaAppTheme
import mega.privacy.android.core.ui.controls.dialogs.ConfirmationDialog
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.legacy.core.ui.controls.dialogs.ProgressDialog

/**
 * [ProgressDialog] for showing transfer processing in progress, it handles cancel confirmation showing a [ConfirmationDialog]
 */
@Composable
fun TransferInProgressDialog(
    onCancelConfirmed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmCancel by remember { mutableStateOf(false) }
    if (confirmCancel) {
        ConfirmationDialog(
            title = stringResource(R.string.cancel_transfers),
            text = stringResource(R.string.warning_cancel_transfers),
            confirmButtonText = stringResource(R.string.button_proceed),
            cancelButtonText = stringResource(R.string.general_dismiss),
            onConfirm = onCancelConfirmed,
            onDismiss = { confirmCancel = false },
            modifier = modifier.testTag(CONFIRM_CANCEL_TAG),
        )
    } else {
        ProgressDialog(
            title = stringResource(R.string.scanning_transfers),
            subTitle = stringResource(id = R.string.warning_scanning_transfers),
            progress = null,
            cancelButtonText = stringResource(R.string.cancel_transfers),
            onCancel = { confirmCancel = true },
            modifier = modifier.testTag(PROGRESS_TAG),
        )
    }
}


@CombinedThemePreviews
@Composable
private fun PreviewTransferInProgressDialog() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        TransferInProgressDialog({ })
    }
}

internal const val PROGRESS_TAG = "transfer_in_progress_dialog:dialog_progress"
internal const val CONFIRM_CANCEL_TAG = "transfer_in_progress_dialog:dialog_confirm"