package mega.privacy.android.app.presentation.transfers.starttransfer.view.dialog

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.shared.original.core.ui.controls.dialogs.ConfirmationDialog
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme

/**
 * [ConfirmationDialog] for resuming transfers.
 */
@Composable
fun ResumeTransfersDialog(
    onResume: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ConfirmationDialog(
        title = stringResource(R.string.warning_resume_transfers),
        text = stringResource(R.string.warning_message_resume_transfers),
        confirmButtonText = stringResource(R.string.button_resume_individual_transfer),
        cancelButtonText = stringResource(R.string.general_cancel),
        onConfirm = onResume,
        onDismiss = onDismiss,
        modifier = modifier.testTag(RESUME_TRANSFERS_DIALOG_TAG),
    )
}


@CombinedThemePreviews
@Composable
private fun ResumeTransfersDialogPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        ResumeTransfersDialog(
            onResume = {},
            onDismiss = {},
        )
    }
}

internal const val RESUME_TRANSFERS_DIALOG_TAG = "resume_transfers_dialog"