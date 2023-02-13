package mega.privacy.android.feature.sync.ui

import android.content.res.Configuration
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder

/**
 * Composable Sync screen
 */
@Composable
fun SyncScreen(
    syncViewModel: SyncViewModel,
    isDark: Boolean,
) {
    val state by syncViewModel.state.collectAsState()
    val folderPicker = getFolderPicker {
        syncViewModel.handleAction(SyncAction.LocalFolderSelected(it))
    }

    AndroidTheme(isDark = isDark) {
        SyncView(
            state = state,
            syncClicked = {
                syncViewModel.handleAction(SyncAction.SyncClicked)
            },
            chooseLocalFolderClicked = {
                folderPicker.launch(null)
            },
            remoteFolderSelected = {
                syncViewModel.handleAction(SyncAction.RemoteFolderSelected(it))
            },
            autoSyncChecked = { syncChecked ->
                syncViewModel.handleAction(SyncAction.AutoSyncChecked(syncChecked))
            },
        )
    }
}

@Composable
private fun getFolderPicker(
    onFolderSelected: (Uri) -> Unit,
): ActivityResultLauncher<Uri?> =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { directoryUri ->
        directoryUri?.let {
            onFolderSelected(it)
        }
    }

/**
 * UI of Sync screen
 */
@Composable
fun SyncView(
    state: SyncState,
    syncClicked: () -> Unit,
    chooseLocalFolderClicked: () -> Unit,
    remoteFolderSelected: (RemoteFolder) -> Unit,
    autoSyncChecked: (Boolean) -> Unit,
) {
    Box(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "Device local storage path:",
                Modifier.padding(bottom = 4.dp),
                fontWeight = FontWeight.Bold,
            )
            Text(
                state.selectedLocalFolder.ifBlank {
                    "Click to choose"
                },
                Modifier
                    .clickable { chooseLocalFolderClicked() }
                    .background(Color.White),
            )

            Spacer(modifier = Modifier.padding(16.dp))

            Text(text = "MEGA storage path:",
                Modifier.padding(bottom = 4.dp),
                fontWeight = FontWeight.Bold)
            RemoteFoldersDropDownMenu(state.selectedMegaFolder,
                state.rootMegaRemoteFolders,
                onFolderSelected = {
                    remoteFolderSelected(it)
                })

            Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Auto-sync on changes", Modifier.padding(end = 8.dp))
                AutoSyncCheckbox {
                    autoSyncChecked(it)
                }
            }

            if (state.isSyncing) {
                CircularProgressIndicator(color = Color.Black)
            } else {
                Button(onClick = syncClicked) {
                    Text(text = "Sync now")
                }
            }

        }
    }
}

/**
 * Dropdown menu for selecting a remote folder.
 */
@Composable
fun RemoteFoldersDropDownMenu(
    currentFolder: RemoteFolder?,
    folders: List<RemoteFolder>,
    onFolderSelected: (RemoteFolder) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectorText = currentFolder?.name ?: "Click to choose"

    Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
        Text(selectorText, modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = {
                expanded = true
            }))
        DropdownMenu(expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)) {
            folders.forEach { folder ->
                DropdownMenuItem(onClick = {
                    onFolderSelected(folder)
                    expanded = false
                }) {
                    Text(text = folder.name)
                }
            }
        }
    }
}

@Composable
fun AutoSyncCheckbox(
    checkedListener: (Boolean) -> Unit,
) {
    var checkedState by remember { mutableStateOf(false) }
    Checkbox(checked = checkedState, onCheckedChange = {
        checkedListener(it)
        checkedState = it
    })
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SyncViewPreview() {
    AndroidTheme(isDark = false) {
        SyncView(
            state = SyncState(),
            syncClicked = {},
            remoteFolderSelected = {},
            chooseLocalFolderClicked = { },
            autoSyncChecked = { },
        )
    }
}
