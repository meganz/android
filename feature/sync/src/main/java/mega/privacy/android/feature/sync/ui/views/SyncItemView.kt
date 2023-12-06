package mega.privacy.android.feature.sync.ui.views

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import mega.privacy.android.feature.sync.ui.model.SyncUiItem

@Composable
internal fun SyncItemView(
    modifier: Modifier,
    syncUiItems: List<SyncUiItem>,
    itemIndex: Int,
    cardExpanded: (SyncUiItem, Boolean) -> Unit,
    pauseRunClicked: (SyncUiItem) -> Unit,
    removeFolderClicked: (folderPairId: Long) -> Unit,
    issuesInfoClicked: () -> Unit
) {
    val sync = syncUiItems[itemIndex]
    SyncCard(
        modifier = modifier.testTag(TEST_TAG_SYNC_ITEM_VIEW),
        folderPairName = sync.folderPairName,
        status = sync.status,
        hasStalledIssues = sync.hasStalledIssues,
        deviceStoragePath = sync.deviceStoragePath,
        megaStoragePath = sync.megaStoragePath,
        method = stringResource(id = sync.method),
        expanded = sync.expanded,
        expandClicked = {
            cardExpanded(sync, !sync.expanded)
        },
        pauseRunClicked = {
            pauseRunClicked(sync)
        },
        removeFolderClicked = {
            removeFolderClicked(sync.id)
        },
        issuesInfoClicked = issuesInfoClicked
    )
}

internal const val TEST_TAG_SYNC_ITEM_VIEW = "sync_item_view:root"
