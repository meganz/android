package mega.privacy.android.app.presentation.transfers.starttransfer.view.dialog

import mega.privacy.android.shared.resources.R as sharedR
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.shared.original.core.ui.controls.dialogs.ConfirmationDialog
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemeComponentPreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme

/**
 * [ConfirmationDialog] for resuming chat upload transfers.
 */
@Composable
fun ResumeChatTransfersDialog(
    onResume: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ResumeTransfersDialog(
        title = stringResource(R.string.warning_resume_transfers),
        text = stringResource(R.string.warning_message_resume_transfers),
        onResume = onResume,
        onDismiss = onDismiss,
        modifier = modifier
    )
}

/**
 * [ConfirmationDialog] for resuming preview download transfers.
 */
@Composable
fun ResumePreviewTransfersDialog(
    fileName: String,
    onResume: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ResumeTransfersDialog(
        title = stringResource(sharedR.string.transfers_preview_paused_dialog_title),
        text = stringResource(sharedR.string.transfers_preview_paused_dialog_text, fileName),
        onResume = onResume,
        onDismiss = onDismiss,
        modifier = modifier
    )
}

@Composable
private fun ResumeTransfersDialog(
    title: String,
    text: String,
    onResume: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ConfirmationDialog(
        title = title,
        text = text,
        confirmButtonText = stringResource(R.string.button_resume_individual_transfer),
        cancelButtonText = stringResource(sharedR.string.general_dialog_cancel_button),
        onConfirm = onResume,
        onDismiss = onDismiss,
        modifier = modifier.testTag(RESUME_TRANSFERS_DIALOG_TAG),
    )
}


@CombinedThemeComponentPreviews
@Composable
private fun ResumeTransfersDialogPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        ResumeChatTransfersDialog(
            onResume = {},
            onDismiss = {},
        )
    }
}

@CombinedThemeComponentPreviews
@Composable
private fun ResumePreviewTransfersDialogPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        ResumePreviewTransfersDialog(
            fileName = "document.docx",
            onResume = {},
            onDismiss = {},
        )
    }
}

internal const val RESUME_TRANSFERS_DIALOG_TAG = "resume_transfers_dialog"