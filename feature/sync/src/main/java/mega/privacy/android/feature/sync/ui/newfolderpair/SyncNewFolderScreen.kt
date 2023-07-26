package mega.privacy.android.feature.sync.ui.newfolderpair

import android.content.res.Configuration
import android.net.Uri
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.ui.controls.buttons.RaisedDefaultMegaButton
import mega.privacy.android.core.ui.navigation.launchFolderPicker
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.feature.sync.R
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import mega.privacy.android.core.ui.controls.banners.TwoActionsBanner
import mega.privacy.android.feature.sync.ui.views.InputSyncInformationView

@Composable
internal fun SyncNewFolderScreen(
    folderPairName: String,
    selectedLocalFolder: String,
    selectedMegaFolder: RemoteFolder?,
    showPermissionBanner: Boolean,
    permissionAllowButtonClicked: () -> Unit,
    permissionLearnMoreButtonClicked: () -> Unit,
    localFolderSelected: (Uri) -> Unit,
    folderNameChanged: (String) -> Unit,
    selectMegaFolderClicked: () -> Unit,
    syncClicked: () -> Unit,
) {

    Column {
        val folderPicker = launchFolderPicker {
            localFolderSelected(it)
        }

        if (showPermissionBanner) {
            TwoActionsBanner(
                modifier = Modifier.padding(top = 20.dp),
                mainText = "Battery optimisation permission allows MEGA to run " +
                        "in the background. You can change this any time by going to " +
                        "Settings -> Apps.",
                leftActionText = "Learn more",
                rightActionText = "Allow",
                leftActionClicked = permissionLearnMoreButtonClicked,
                rightActionClicked = permissionAllowButtonClicked,
            )
            Divider(modifier = Modifier.padding(vertical = 8.dp))
        }

        InputSyncInformationView(
            Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp, bottom = 48.dp),
            selectDeviceFolderClicked = {
                folderPicker.launch(null)
            },
            selectMEGAFolderClicked = {
                selectMegaFolderClicked()
            },
            onFolderPairNameChanged = { folderPairName ->
                folderNameChanged(folderPairName)
            },
            folderPairName,
            selectedLocalFolder,
            selectedMegaFolder?.name ?: ""
        )

        Box(
            Modifier.fillMaxWidth(), contentAlignment = Alignment.Center
        ) {
            val buttonEnabled =
                selectedLocalFolder.isNotBlank() && selectedMegaFolder != null

            RaisedDefaultMegaButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
                textId = R.string.sync_button_label,
                onClick = syncClicked,
                enabled = buttonEnabled
            )
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewSyncNewFolderScreen() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        SyncNewFolderScreen(
            folderPairName = "",
            selectedLocalFolder = "",
            selectedMegaFolder = null,
            showPermissionBanner = true,
            permissionAllowButtonClicked = {},
            permissionLearnMoreButtonClicked = {},
            localFolderSelected = {},
            folderNameChanged = {},
            selectMegaFolderClicked = {},
            syncClicked = {}
        )
    }
}

