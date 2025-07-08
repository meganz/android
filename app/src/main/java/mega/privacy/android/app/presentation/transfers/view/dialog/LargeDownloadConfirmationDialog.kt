package mega.privacy.android.app.presentation.transfers.view.dialog

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.app.R
import mega.privacy.android.shared.original.core.ui.controls.dialogs.ConfirmationDialog
import mega.privacy.android.shared.original.core.ui.preview.BooleanProvider
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Confirmation dialog for large download
 */
@Composable
fun LargeDownloadConfirmationDialog(
    isPreviewDownload: Boolean,
    sizeString: String,
    onAllow: () -> Unit,
    onAlwaysAllow: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) = ConfirmationDialog(
    modifier = modifier.testTag(TEST_TAG_LARGE_DOWNLOAD_CONFIRMATION_DIALOG),
    title = stringResource(
        id = if (isPreviewDownload) {
            sharedR.string.alert_larger_file_preview_title
        } else {
            R.string.transfers_confirm_large_download_title
        }
    ),
    text = stringResource(
        id = if (isPreviewDownload) {
            sharedR.string.alert_larger_file_preview
        } else {
            R.string.alert_larger_file
        }, sizeString
    ),
    buttonOption1Text = stringResource(
        id = if (isPreviewDownload) {
            sharedR.string.alert_larger_file_preview_confirm_button
        } else {
            R.string.transfers_confirm_large_download_button_start
        }
    ),
    buttonOption2Text = stringResource(
        id = if (isPreviewDownload) {
            sharedR.string.alert_larger_file_preview_always_allow_button
        } else {
            R.string.transfers_confirm_large_download_button_start_always
        }
    ),
    cancelButtonText = stringResource(id = sharedR.string.general_dialog_cancel_button),
    onOption1 = onAllow,
    onOption2 = onAlwaysAllow,
    onDismiss = onDismiss,
)

@CombinedThemePreviews
@Composable
private fun LargeDownloadConfirmationDialogPreview(
    @PreviewParameter(BooleanProvider::class) isPreviewDownload: Boolean,
) {
    AndroidThemeForPreviews {
        Box(modifier = Modifier.fillMaxSize()) {
            LargeDownloadConfirmationDialog(
                isPreviewDownload = isPreviewDownload,
                sizeString = "15 GB",
                onAllow = {},
                onAlwaysAllow = {},
                onDismiss = {},
            )
        }
    }
}

internal const val TEST_TAG_LARGE_DOWNLOAD_CONFIRMATION_DIALOG =
    "transfers_view:large_download_confirmation_dialog"