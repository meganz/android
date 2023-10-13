package mega.privacy.android.feature.sync.ui.views

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mega.privacy.android.feature.sync.ui.model.SyncUiItem

@Composable
internal fun SyncItemView(
    modifier: Modifier,
    syncUiItems: List<SyncUiItem>,
    itemIndex: Int,
    cardExpanded: (SyncUiItem, Boolean) -> Unit,
    pauseRunClicked: (SyncUiItem) -> Unit,
    removeFolderClicked: (folderPairId: Long) -> Unit,
) {
    val sync = syncUiItems[itemIndex]
    SyncCard(
        modifier = modifier,
        folderPairName = sync.folderPairName,
        status = sync.status,
        hasStalledIssues = sync.hasStalledIssues,
        deviceStoragePath = sync.deviceStoragePath,
        megaStoragePath = sync.megaStoragePath,
        method = sync.method,
        expanded = sync.expanded,
        expandClicked = {
            cardExpanded(sync, !sync.expanded)
        },
        pauseRunClicked = {
            pauseRunClicked(sync)
        },
        removeFolderClicked = {
            removeFolderClicked(sync.id)
        })
}
