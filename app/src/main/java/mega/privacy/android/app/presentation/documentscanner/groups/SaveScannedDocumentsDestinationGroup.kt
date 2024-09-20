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
import mega.privacy.android.app.presentation.documentscanner.model.ScanDestination
import mega.privacy.android.shared.original.core.ui.controls.chip.MegaChip
import mega.privacy.android.shared.original.core.ui.controls.chip.RoundedChipStyle
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor

/**
 * A Composable Group allowing Users to select where to save the Scanned Document/s
 *
 * @param selectedScanDestination The previously selected Scan Destination
 * @param onScanDestinationSelected Lambda when a new Scan Destination is selected
 * @param modifier the default Modifier
 */
@Composable
internal fun SaveScannedDocumentsDestinationGroup(
    selectedScanDestination: ScanDestination,
    onScanDestinationSelected: (ScanDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        MegaText(
            modifier = Modifier
                .padding(
                    horizontal = 16.dp,
                    vertical = 8.dp,
                )
                .testTag(SAVE_SCANNED_DOCUMENTS_DESTINATION_GROUP_HEADER),
            text = stringResource(R.string.scan_destination),
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
                modifier = Modifier.testTag(
                    SAVE_SCANNED_DOCUMENTS_DESTINATION_GROUP_CHIP_CLOUD_DRIVE
                ),
                selected = selectedScanDestination == ScanDestination.CloudDrive,
                style = RoundedChipStyle,
                text = stringResource(SharedR.string.video_section_videos_location_option_cloud_drive),
                onClick = { onScanDestinationSelected(ScanDestination.CloudDrive) },
            )
            MegaChip(
                modifier = Modifier.testTag(SAVE_SCANNED_DOCUMENTS_DESTINATION_GROUP_CHIP_CHAT),
                selected = selectedScanDestination == ScanDestination.Chat,
                style = RoundedChipStyle,
                text = stringResource(SharedR.string.document_scanning_confirmation_destination_chat),
                onClick = { onScanDestinationSelected(ScanDestination.Chat) },
            )
        }
    }
}

/**
 * A Preview Composable for [SaveScannedDocumentsDestinationGroup]
 */
@CombinedThemePreviews
@Composable
private fun SaveScannedDocumentsDestinationGroupPreview(
    @PreviewParameter(ScanDestinationParameterProvider::class) scanDestination: ScanDestination,
) {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        SaveScannedDocumentsDestinationGroup(
            selectedScanDestination = scanDestination,
            onScanDestinationSelected = {},
        )
    }
}

private class ScanDestinationParameterProvider : PreviewParameterProvider<ScanDestination> {
    override val values: Sequence<ScanDestination>
        get() = ScanDestination.entries.asSequence()
}

internal const val SAVE_SCANNED_DOCUMENTS_DESTINATION_GROUP_HEADER =
    "save_scanned_documents_destination_group:mega_text_header"
internal const val SAVE_SCANNED_DOCUMENTS_DESTINATION_GROUP_CHIP_CLOUD_DRIVE =
    "save_scanned_documents_destination_group:mega_chip_cloud_drive"
internal const val SAVE_SCANNED_DOCUMENTS_DESTINATION_GROUP_CHIP_CHAT =
    "save_scanned_documents_destination_group:mega_chip_chat"