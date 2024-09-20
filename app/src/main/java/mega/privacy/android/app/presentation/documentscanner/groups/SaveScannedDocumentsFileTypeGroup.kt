package mega.privacy.android.app.presentation.documentscanner.groups

import mega.privacy.android.shared.resources.R as SharedR
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.documentscanner.model.ScanFileType
import mega.privacy.android.shared.original.core.ui.controls.chip.MegaChip
import mega.privacy.android.shared.original.core.ui.controls.chip.RoundedChipStyle
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor

/**
 * A Composable Group allowing Users to select the resulting File Type of the Scanned Document. This
 * Group is only shown when the User only scans one Document
 *
 * @param selectedScanFileType The previously selected Scan File Type
 * @param onScanFileTypeSelected Lambda when a new Scan File Type is selected
 * @param modifier The default Modifier
 */
@Composable
internal fun SaveScannedDocumentsFileTypeGroup(
    selectedScanFileType: ScanFileType,
    onScanFileTypeSelected: (ScanFileType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        MegaText(
            modifier = Modifier
                .padding(
                    horizontal = 16.dp,
                    vertical = 8.dp,
                )
                .testTag(SAVE_SCANNED_DOCUMENTS_FILE_TYPE_GROUP_HEADER),
            text = stringResource(R.string.scan_file_type),
            textColor = TextColor.Primary,
            style = MaterialTheme.typography.body2,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = 16.dp,
                    bottom = 16.dp,
                    start = 72.dp,
                    end = 16.dp,
                ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MegaChip(
                modifier = Modifier.testTag(SAVE_SCANNED_DOCUMENTS_FILE_TYPE_GROUP_CHIP_PDF),
                selected = selectedScanFileType == ScanFileType.Pdf,
                style = RoundedChipStyle,
                text = stringResource(SharedR.string.document_scanning_confirmation_file_type_pdf),
                onClick = { onScanFileTypeSelected(ScanFileType.Pdf) },
            )
            MegaChip(
                modifier = Modifier.testTag(SAVE_SCANNED_DOCUMENTS_FILE_TYPE_GROUP_CHIP_JPG),
                selected = selectedScanFileType == ScanFileType.Jpg,
                style = RoundedChipStyle,
                text = stringResource(SharedR.string.document_scanning_confirmation_file_type_jpg),
                onClick = { onScanFileTypeSelected(ScanFileType.Jpg) },
            )
        }
    }
}

/**
 * A Preview Composable for [SaveScannedDocumentsFileTypeGroup]
 */
@CombinedThemePreviews
@Composable
private fun SaveScannedDocumentsFileTypeGroupPreview(
    @PreviewParameter(ScanFileTypeParameterProvider::class) scanFileType: ScanFileType,
) {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        SaveScannedDocumentsFileTypeGroup(
            selectedScanFileType = scanFileType,
            onScanFileTypeSelected = {},
        )
    }
}

private class ScanFileTypeParameterProvider : PreviewParameterProvider<ScanFileType> {
    override val values: Sequence<ScanFileType>
        get() = ScanFileType.entries.asSequence()

}

internal const val SAVE_SCANNED_DOCUMENTS_FILE_TYPE_GROUP_HEADER =
    "saved_scanned_documents_file_type_group:mega_text_header"
internal const val SAVE_SCANNED_DOCUMENTS_FILE_TYPE_GROUP_CHIP_PDF =
    "saved_scanned_documents_file_type_group:mega_chip_pdf"
internal const val SAVE_SCANNED_DOCUMENTS_FILE_TYPE_GROUP_CHIP_JPG =
    "saved_scanned_documents_file_type_group:mega_chip_jpg"