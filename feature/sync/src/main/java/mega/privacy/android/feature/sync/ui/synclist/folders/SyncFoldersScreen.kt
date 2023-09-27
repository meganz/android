package mega.privacy.android.feature.sync.ui.synclist.folders

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
import mega.privacy.android.feature.sync.ui.model.SyncUiItem
import mega.privacy.android.feature.sync.ui.synclist.NoItemsPlaceholder
import mega.privacy.android.feature.sync.ui.views.SyncItemView
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersAction.CardExpanded

@Composable
internal fun SyncFoldersScreen(
    syncUiItems: List<SyncUiItem>,
    cardExpanded: (CardExpanded) -> Unit,
    pauseRunClicked: (SyncUiItem) -> Unit,
    removeFolderClicked: (folderPairId: Long) -> Unit,
    addFolderClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(state = LazyListState(), modifier = modifier) {
        val itemsCount = syncUiItems.size

        if (itemsCount == 0) {
            item {
                NoItemsPlaceholder(
                    modifier
                        .fillParentMaxHeight(0.8f)
                        .fillParentMaxWidth()
                )
            }
        } else {
            items(count = itemsCount, key = {
                syncUiItems[it].id
            }) { itemIndex ->
                SyncItemView(
                    Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                    syncUiItems,
                    itemIndex,
                    cardExpanded = { syncUiItem, expanded ->
                        cardExpanded(CardExpanded(syncUiItem, expanded))
                    },
                    pauseRunClicked,
                    removeFolderClicked
                )
            }
        }
    }
    Box(
        modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd
    ) {
        FloatingActionButton(
            onClick = { addFolderClicked() },
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add folder pair")
        }
    }
}
