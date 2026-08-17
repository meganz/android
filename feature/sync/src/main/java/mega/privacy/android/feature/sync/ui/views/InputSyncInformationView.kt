package mega.privacy.android.feature.sync.ui.views

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.card.RoundCard
import mega.android.core.ui.components.divider.SubtleDivider
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.surface.CardSurface
import mega.android.core.ui.components.surface.SurfaceColor
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.core.R as CoreUiR
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.feature.sync.R
import mega.privacy.android.feature.sync.ui.newfolderpair.SyncNewFolderScreen
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.resources.R as sharedResR

@Composable
internal fun InputSyncInformationView(
    syncType: SyncType,
    deviceName: String,
    selectDeviceFolderClicked: () -> Unit,
    selectMegaFolderClicked: () -> Unit,
    selectedDeviceFolder: String = "",
    selectedMegaFolder: String = "",
) {
    Column(modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)) {
        RoundCard {
            Column {
                CardSurface(
                    surfaceColor = SurfaceColor.Surface1,
                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                ) {
                    Header(
                        imageResource = when (syncType) {
                            SyncType.TYPE_BACKUP -> iconPackR.drawable.ic_database_medium_thin_outline
                            else -> CoreUiR.drawable.ic_sync
                        },
                        text = when (syncType) {
                            SyncType.TYPE_BACKUP -> stringResource(id = sharedResR.string.sync_add_new_backup_card_sync_type_text)
                            else -> stringResource(id = R.string.sync_two_way)
                        },
                    )
                }

                TwoLinesItem(
                    imageResource = CoreUiR.drawable.ic_smartphone,
                    topText = when (syncType) {
                        SyncType.TYPE_BACKUP -> stringResource(id = sharedResR.string.sync_add_new_backup_choose_device_folder_title)
                        else -> stringResource(id = R.string.sync_folder_choose_device_folder_title)
                    },
                    bottomText = selectedDeviceFolder.substring(selectedDeviceFolder.lastIndexOf('/') + 1),
                    bottomDefaultText = stringResource(R.string.sync_general_select),
                    modifier = Modifier
                        .clickable { selectDeviceFolderClicked() }
                        .testTag(SELECT_DEVICE_FOLDER_OPTION_TEST_TAG)
                )

                SubtleDivider(modifier = Modifier.padding(horizontal = 16.dp))

                when (syncType) {
                    SyncType.TYPE_BACKUP -> {
                        TwoLinesItem(
                            imageResource = CoreUiR.drawable.ic_mega,
                            topText = stringResource(id = sharedResR.string.sync_add_new_backup_choose_mega_folder_title),
                            bottomText = "/Backups/$deviceName",
                            bottomDefaultText = "",
                            isBottomTextClickable = false,
                        )
                    }

                    else -> {
                        TwoLinesItem(
                            imageResource = CoreUiR.drawable.ic_mega,
                            topText = stringResource(id = R.string.sync_folders_choose_mega_folder_title),
                            bottomText = selectedMegaFolder,
                            bottomDefaultText = stringResource(R.string.sync_general_select),
                            modifier = Modifier.clickable { selectMegaFolderClicked() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(
    @DrawableRes imageResource: Int,
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MegaIcon(
            painter = painterResource(imageResource),
            contentDescription = null,
            modifier = modifier
                .size(20.dp)
                .padding(end = 8.dp),
            tint = IconColor.Secondary
        )
        MegaText(
            text = text,
            textColor = TextColor.Secondary,
            style = AppTheme.typography.bodySmall
        )
    }
}

@Composable
private fun TwoLinesItem(
    @DrawableRes imageResource: Int,
    topText: String,
    bottomText: String,
    bottomDefaultText: String,
    modifier: Modifier = Modifier,
    isBottomTextClickable: Boolean = true,
) {
    Row(
        modifier
            .padding(top = 16.dp, bottom = 16.dp)
            .fillMaxWidth()
    ) {
        MegaIcon(
            painter = painterResource(imageResource),
            contentDescription = null,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp),
            tint = IconColor.Secondary
        )
        Column {
            MegaText(
                text = topText,
                textColor = TextColor.Secondary
            )
            MegaText(
                text = bottomText.ifEmpty {
                    bottomDefaultText
                },
                textColor = if (isBottomTextClickable) {
                    TextColor.Accent
                } else {
                    TextColor.Secondary
                },
                modifier = Modifier.padding(top = 4.dp),
                style = if (isBottomTextClickable) {
                    AppTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                } else {
                    AppTheme.typography.bodyMedium
                },
            )
        }
    }
}

@CombinedThemePreviews
@Composable
private fun EmptyInputSyncInformationViewPreview(
    @PreviewParameter(SyncTypePreviewProvider::class) syncType: SyncType
) {
    AndroidThemeForPreviews {
        InputSyncInformationView(
            syncType = syncType,
            deviceName = "Device Name",
            selectDeviceFolderClicked = {},
            selectMegaFolderClicked = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun InputSyncInformationViewPreview(
    @PreviewParameter(SyncTypePreviewProvider::class) syncType: SyncType
) {
    AndroidThemeForPreviews {
        InputSyncInformationView(
            syncType = syncType,
            deviceName = "Device Name",
            selectDeviceFolderClicked = {},
            selectMegaFolderClicked = {},
            selectedDeviceFolder = "Device folder name",
            selectedMegaFolder = "MEGA folder name"
        )
    }
}

/**
 * A class that provides Preview Parameters for the [SyncNewFolderScreen]
 */
internal class SyncTypePreviewProvider : PreviewParameterProvider<SyncType> {
    override val values = sequenceOf(SyncType.TYPE_TWOWAY, SyncType.TYPE_BACKUP)
}

/**
 * Multi FAB main button's test tag
 */
const val SELECT_DEVICE_FOLDER_OPTION_TEST_TAG = "input_sync_info_view:select_device_folder_option"
