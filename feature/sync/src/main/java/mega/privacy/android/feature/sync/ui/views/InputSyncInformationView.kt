package mega.privacy.android.feature.sync.ui.views

import mega.privacy.android.core.R as CoreUiR
import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mega.privacy.android.shared.theme.MegaAppTheme
import mega.privacy.android.core.ui.controls.textfields.GenericTextField
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_012_white_alpha_012
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_087_white_alpha_087
import mega.privacy.android.core.ui.theme.extensions.teal_300_teal_200
import mega.privacy.android.core.ui.theme.extensions.textColorSecondary
import mega.privacy.android.feature.sync.R

@Composable
internal fun InputSyncInformationView(
    modifier: Modifier = Modifier,
    selectDeviceFolderClicked: () -> Unit,
    selectMEGAFolderClicked: () -> Unit,
    onFolderPairNameChanged: (String) -> Unit,
    folderPairName: String,
    selectedDeviceFolder: String = "",
    selectedMEGAFolder: String = "",
) {
    Column(
        modifier = modifier
            .border(
                1.dp,
                MaterialTheme.colors.grey_alpha_012_white_alpha_012,
                RoundedCornerShape(12.dp)
            )
    ) {
        GenericTextField(
            text = folderPairName,
            onTextChange = { onFolderPairNameChanged(it) },
            modifier = Modifier.padding(start = 24.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
            placeholder = stringResource(id = R.string.sync_folders_choose_folder_pair),
            imeAction = ImeAction.Done,
            keyboardActions = KeyboardActions.Default,
            colors = TextFieldDefaults.textFieldColors(
                textColor = MaterialTheme.colors.onPrimary,
                backgroundColor = Color.Transparent,
                cursorColor = MaterialTheme.colors.secondary,
                errorCursorColor = MaterialTheme.colors.error,
                errorIndicatorColor = MaterialTheme.colors.error,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
            ),
            singleLine = false
        )

        Divider(
            color = MaterialTheme.colors.grey_alpha_012_white_alpha_012,
            thickness = 1.dp
        )

        TwoLinesItem(
            CoreUiR.drawable.ic_smartphone,
            stringResource(id = R.string.sync_folder_choose_device_folder_title),
            selectedDeviceFolder,
            stringResource(R.string.sync_general_select),
            Modifier.clickable { selectDeviceFolderClicked() }
        )

        Divider(
            color = MaterialTheme.colors.grey_alpha_012_white_alpha_012,
            thickness = 1.dp,
            modifier = Modifier.padding(start = 72.dp)
        )

        TwoLinesItem(
            CoreUiR.drawable.ic_mega,
            stringResource(id = R.string.sync_folders_choose_mega_folder_title),
            selectedMEGAFolder,
            stringResource(R.string.sync_general_select),
            Modifier.clickable {
                selectMEGAFolderClicked()
            }
        )

        Divider(
            color = MaterialTheme.colors.grey_alpha_012_white_alpha_012,
            thickness = 1.dp,
            modifier = Modifier.padding(start = 72.dp)
        )
        TwoLinesItem(
            CoreUiR.drawable.ic_sync,
            stringResource(id = R.string.sync_folders_method),
            stringResource(id = R.string.sync_two_way),
            ""
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

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SyncEmptyScreenPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        InputSyncInformationView(
            Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp),
            {},
            {},
            { },
            ""
        )
    }
}