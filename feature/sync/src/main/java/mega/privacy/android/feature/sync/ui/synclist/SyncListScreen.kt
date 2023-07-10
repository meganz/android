package mega.privacy.android.feature.sync.ui.synclist

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.feature.sync.ui.model.SyncStatus
import mega.privacy.android.feature.sync.ui.model.SyncUiItem
import mega.privacy.android.feature.sync.ui.views.SyncCard

@Composable
internal fun SyncListScreen(
    modifier: Modifier = Modifier,
    syncUiItems: List<SyncUiItem>,
    cardExpanded: (SyncUiItem, Boolean) -> Unit,
    removeFolderClicked: (folderPairId: Long) -> Unit,
    addFolderClicked: () -> Unit,
) {
    LazyColumn(state = LazyListState(), modifier = modifier) {
        items(count = syncUiItems.size, key = {
            syncUiItems[it].id
        }) { itemIndex ->
            val sync = syncUiItems[itemIndex]
            SyncCard(Modifier.padding(bottom = 8.dp),
                folderPairName = sync.folderPairName,
                status = sync.status,
                deviceStoragePath = sync.deviceStoragePath,
                megaStoragePath = sync.megaStoragePath,
                method = sync.method,
                expanded = sync.expanded,
                expandClicked = {
                    cardExpanded(sync, !sync.expanded)
                },
                infoClicked = {
                    // This button will be removed
                },
                removeFolderClicked = {
                    removeFolderClicked(sync.id)
                })
        }
    }
    Box(
        modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd
    ) {
        FloatingActionButton(
            onClick = { addFolderClicked() }, modifier = Modifier.padding(16.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add folder pair")
        }
    }
}

@CombinedThemePreviews
@Composable
fun PreviewSyncListScreen() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        SyncListScreen(Modifier.padding(16.dp), listOf(
            SyncUiItem(
                1,
                folderPairName = "folderPair 1",
                status = SyncStatus.SYNCING,
                deviceStoragePath = "photo/cat_pics",
                megaStoragePath = "cat pics backup",
                method = "Two way sync",
                expanded = false,
            ),
            SyncUiItem(
                2,
                folderPairName = "folderPair 2",
                status = SyncStatus.SYNCING,
                deviceStoragePath = "deviceStoragePath",
                megaStoragePath = "megaStoragePath",
                method = "Two way sync",
                expanded = false,
            ),
            SyncUiItem(
                3,
                folderPairName = "folderPair 3",
                status = SyncStatus.COMPLETED,
                deviceStoragePath = "deviceStoragePath",
                megaStoragePath = "megaStoragePath",
                method = "Two way sync",
                expanded = false,
            ),
        ), cardExpanded = { _, _ -> }, removeFolderClicked = {}, addFolderClicked = {})
    }
}