package mega.privacy.android.app.presentation.documentscanner.groups

import mega.privacy.android.core.R as CoreR
import mega.privacy.android.shared.resources.R as SharedR
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.documentscanner.model.ScanDestination
import mega.privacy.android.shared.original.core.ui.controls.chip.DefaultChipStyle
import mega.privacy.android.shared.original.core.ui.controls.chip.MegaChip
import mega.privacy.android.shared.original.core.ui.controls.chip.TransparentChipStyle
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.android.core.ui.theme.values.TextColor

/**
 * A Composable Group allowing Users to select where to save the Scanned Document/s
 *
 * @param originatedFromChat true if the Document Scanner was accessed from Chat
 * @param selectedScanDestination The previously selected Scan Destination
 * @param onScanDestinationSelected Lambda when a new Scan Destination is selected
 * @param modifier the default Modifier
 */
@Composable
internal fun SaveScannedDocumentsDestinationGroup(
    originatedFromChat: Boolean,
    selectedScanDestination: ScanDestination,
    onScanDestinationSelected: (ScanDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        MegaText(
            modifier = Modifier
                .padding(top = 16.dp, bottom = 8.dp, start = 16.dp, end = 16.dp)
                .testTag(SAVE_SCANNED_DOCUMENTS_DESTINATION_GROUP_HEADER),
            text = stringResource(R.string.scan_destination),
            textColor = TextColor.Secondary,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        ) {
            if (!originatedFromChat) {
                val isCloudDriveSelected = selectedScanDestination == ScanDestination.CloudDrive
                MegaChip(
                    selected = isCloudDriveSelected,
                    text = stringResource(SharedR.string.video_section_videos_location_option_cloud_drive),
                    modifier = Modifier.testTag(
                        SAVE_SCANNED_DOCUMENTS_DESTINATION_GROUP_CHIP_CLOUD_DRIVE
                    ),
                    style = if (isCloudDriveSelected) DefaultChipStyle else TransparentChipStyle,
                    leadingIcon = if (isCloudDriveSelected) CoreR.drawable.ic_filter_selected else null
                ) { onScanDestinationSelected(ScanDestination.CloudDrive) }
            }
            val isChatSelected = selectedScanDestination == ScanDestination.Chat
            MegaChip(
                selected = isChatSelected,
                text = stringResource(SharedR.string.general_chat),
                modifier = Modifier.testTag(SAVE_SCANNED_DOCUMENTS_DESTINATION_GROUP_CHIP_CHAT),
                style = if (isChatSelected) DefaultChipStyle else TransparentChipStyle,
                leadingIcon = if (isChatSelected) CoreR.drawable.ic_filter_selected else null
            ) { onScanDestinationSelected(ScanDestination.Chat) }
        }
    }
}

/**
 * A Preview Composable for [SaveScannedDocumentsDestinationGroup]
 */
@CombinedThemePreviews
@Composable
private fun SaveScannedDocumentsDestinationGroupPreview(
    @PreviewParameter(ScanDestinationPreviewParameterProvider::class) scanDestinationPreviewParameter: ScanDestinationPreviewParameter,
) {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        SaveScannedDocumentsDestinationGroup(
            originatedFromChat = scanDestinationPreviewParameter.originatedFromChat,
            selectedScanDestination = scanDestinationPreviewParameter.selectedScanDestination,
            onScanDestinationSelected = {},
        )
    }
}

private class ScanDestinationPreviewParameterProvider :
    PreviewParameterProvider<ScanDestinationPreviewParameter> {
    override val values: Sequence<ScanDestinationPreviewParameter>
        get() = sequenceOf(
            // Document Scanning is accessed anywhere other than Chat and the Scan Destination is Cloud Drive
            ScanDestinationPreviewParameter(
                originatedFromChat = false,
                selectedScanDestination = ScanDestination.CloudDrive,
            ),
            // Document Scanning is accessed anywhere other than Chat and the Scan Destination is Chat
            ScanDestinationPreviewParameter(
                originatedFromChat = false,
                selectedScanDestination = ScanDestination.Chat,
            ),
            // Document Scanning is accessed from Chat and the Scan Destination is Chat by default
            ScanDestinationPreviewParameter(
                originatedFromChat = true,
                selectedScanDestination = ScanDestination.Chat,
            ),
        )
}

private data class ScanDestinationPreviewParameter(
    val originatedFromChat: Boolean,
    val selectedScanDestination: ScanDestination,
)

internal const val SAVE_SCANNED_DOCUMENTS_DESTINATION_GROUP_HEADER =
    "save_scanned_documents_destination_group:mega_text_header"
internal const val SAVE_SCANNED_DOCUMENTS_DESTINATION_GROUP_CHIP_CLOUD_DRIVE =
    "save_scanned_documents_destination_group:mega_chip_cloud_drive"
internal const val SAVE_SCANNED_DOCUMENTS_DESTINATION_GROUP_CHIP_CHAT =
    "save_scanned_documents_destination_group:mega_chip_chat"