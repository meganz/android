package mega.privacy.android.feature.documentscanner.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.feature.documentscanner.domain.model.ScannerModelProvider
import mega.privacy.android.shared.resources.R as sharedR

/**
 * One-time download-confirmation dialog shown before the ~93 MB scanner model is
 * fetched. Shown on Wi-Fi too, because the artifact is large.
 *
 * Two variants, picked by [onCellular]:
 * - Wi-Fi (or cellular with consent already granted): "Download" / "Use old scanner".
 * - Cellular without consent: "Download over mobile data" (the caller persists the
 *   consent) / "Use old scanner".
 *
 * @param onCellular whether the device is on a metered connection without prior consent.
 * @param onConfirmDownload the positive action — start the download.
 * @param onUseOldScanner the negative action — fall back to the legacy ML Kit scanner.
 */
@Composable
internal fun ScannerDownloadConfirmationDialog(
    onCellular: Boolean,
    onConfirmDownload: () -> Unit,
    onUseOldScanner: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val confirmTextRes = if (onCellular) {
        sharedR.string.document_scanner_download_dialog_download_over_mobile_data_button
    } else {
        sharedR.string.document_scanner_download_dialog_download_button
    }
    val downloadSizeMb = (ScannerModelProvider.SIZE_BYTES / BYTES_PER_MB).toInt()
    BasicDialog(
        modifier = modifier.testTag(SCANNER_DOWNLOAD_CONFIRMATION_DIALOG_TAG),
        title = stringResource(
            if (onCellular) {
                sharedR.string.document_scanner_download_dialog_mobile_data_title
            } else {
                sharedR.string.document_scanner_download_dialog_title
            }
        ),
        description = stringResource(
            if (onCellular) {
                sharedR.string.document_scanner_download_dialog_mobile_data_message
            } else {
                sharedR.string.document_scanner_download_dialog_message
            },
            downloadSizeMb,
        ),
        positiveButtonText = stringResource(confirmTextRes),
        onPositiveButtonClicked = onConfirmDownload,
        negativeButtonText = stringResource(sharedR.string.document_scanner_download_dialog_use_old_scanner_button),
        onNegativeButtonClicked = onUseOldScanner,
        onDismiss = onUseOldScanner,
    )
}

@Preview
@Composable
private fun ScannerDownloadConfirmationDialogWifiPreview() {
    AndroidThemeForPreviews {
        ScannerDownloadConfirmationDialog(
            onCellular = false,
            onConfirmDownload = {},
            onUseOldScanner = {},
        )
    }
}

@Preview
@Composable
private fun ScannerDownloadConfirmationDialogCellularPreview() {
    AndroidThemeForPreviews {
        ScannerDownloadConfirmationDialog(
            onCellular = true,
            onConfirmDownload = {},
            onUseOldScanner = {},
        )
    }
}

private const val BYTES_PER_MB = 1024 * 1024

internal const val SCANNER_DOWNLOAD_CONFIRMATION_DIALOG_TAG =
    "scanner_download_confirmation:dialog"
