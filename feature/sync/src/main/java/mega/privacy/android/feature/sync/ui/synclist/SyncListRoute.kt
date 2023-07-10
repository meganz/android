package mega.privacy.android.feature.sync.ui.synclist

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mega.privacy.android.feature.sync.ui.model.SyncStatus
import mega.privacy.android.feature.sync.ui.model.SyncUiItem

@Composable
fun SyncListRoute(
    addFolderClicked: () -> Unit
) {
    var state by remember { mutableStateOf(SyncListState(fakeData())) }

    SyncListScreen(
        Modifier.padding(16.dp),
        syncUiItems = state.syncUiItems,
        cardExpanded = { syncUiItem, expanded ->
            state = state.copy(
                syncUiItems = state.syncUiItems.map {
                    if (it.id == syncUiItem.id) {
                        it.copy(expanded = expanded)
                    } else {
                        it
                    }
                }
            )
        },
        removeFolderClicked = {

        },
        addFolderClicked = addFolderClicked
    )
}

private fun fakeData() = listOf(
    SyncUiItem(
        1,
        folderPairName = "folderPair 1",
        status = SyncStatus.SYNCING,
        deviceStoragePath = "Sync",
        megaStoragePath = "papers",
        method = "Two way sync",
        expanded = false,
    )
)