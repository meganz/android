package mega.privacy.android.feature.sync.ui.views

import mega.privacy.android.core.R as CoreUiR
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.ui.controls.cards.MegaCardWithHeader
import mega.privacy.android.core.ui.controls.dividers.DividerType
import mega.privacy.android.core.ui.controls.dividers.MegaDivider
import mega.privacy.android.core.ui.controls.text.MegaText
import mega.privacy.android.shared.theme.MegaAppTheme
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_087_white_alpha_087
import mega.privacy.android.core.ui.theme.extensions.teal_300_teal_200
import mega.privacy.android.core.ui.theme.extensions.textColorSecondary
import mega.privacy.android.core.ui.theme.tokens.TextColor
import mega.privacy.android.feature.sync.R

@Composable
internal fun InputSyncInformationView(
    selectDeviceFolderClicked: () -> Unit,
    selectMegaFolderClicked: () -> Unit,
    selectedDeviceFolder: String = "",
    selectedMegaFolder: String = "",
) {
    Column(modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 48.dp)) {
        MegaCardWithHeader(
            header = {
                Header(
                    imageResource = CoreUiR.drawable.ic_sync,
                    text = stringResource(id = R.string.sync_two_way),
                )
            },
            body = {
                TwoLinesItem(
                    CoreUiR.drawable.ic_smartphone,
                    stringResource(id = R.string.sync_folder_choose_device_folder_title),
                    selectedDeviceFolder,
                    stringResource(R.string.sync_general_select),
                    Modifier.clickable { selectDeviceFolderClicked() }
                )

                MegaDivider(dividerType = DividerType.Centered)

                TwoLinesItem(
                    CoreUiR.drawable.ic_mega,
                    stringResource(id = R.string.sync_folders_choose_mega_folder_title),
                    selectedMegaFolder,
                    stringResource(R.string.sync_general_select),
                    Modifier.clickable {
                        selectMegaFolderClicked()
                    }
                )
            },
        )
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
        Image(
            painter = painterResource(imageResource),
            contentDescription = null,
            modifier = modifier.size(20.dp).padding(end = 8.dp),
            colorFilter = ColorFilter.tint(MaterialTheme.colors.textColorSecondary)
        )
        MegaText(
            text = text,
            textColor = TextColor.Secondary,
            style = MaterialTheme.typography.caption
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
) {
    Row(
        modifier
            .padding(top = 16.dp, bottom = 16.dp)
            .fillMaxWidth()
    ) {
        Image(
            painter = painterResource(imageResource),
            contentDescription = null,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp),
            colorFilter = ColorFilter.tint(MaterialTheme.colors.textColorSecondary)
        )
        Column {
            Text(
                text = topText,
                style = if (bottomText.isEmpty()) {
                    MaterialTheme.typography.body1.copy(color = MaterialTheme.colors.textColorSecondary)
                } else {
                    MaterialTheme.typography.body1.copy(color = MaterialTheme.colors.grey_alpha_087_white_alpha_087)
                },
            )
            Text(
                text = bottomText.ifEmpty {
                    bottomDefaultText
                },
                style = if (bottomText.isEmpty()) {
                    MaterialTheme.typography.body2.copy(color = MaterialTheme.colors.teal_300_teal_200)
                } else {
                    MaterialTheme.typography.body2.copy(color = MaterialTheme.colors.textColorSecondary)
                },
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@CombinedThemePreviews
@Composable
private fun SyncEmptyScreenPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        InputSyncInformationView(
            selectDeviceFolderClicked = {},
            selectMegaFolderClicked = {},
        )
    }
}