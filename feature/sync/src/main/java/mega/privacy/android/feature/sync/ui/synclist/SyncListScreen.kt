package mega.privacy.android.feature.sync.ui.synclist

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.ui.controls.appbar.TopAppBar
import mega.privacy.android.core.ui.controls.chips.PhotoChip
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.textColorPrimary
import mega.privacy.android.core.ui.theme.extensions.textColorSecondary
import mega.privacy.android.feature.sync.R
import mega.privacy.android.feature.sync.domain.entity.SyncStatus
import mega.privacy.android.feature.sync.ui.model.SyncUiItem
import mega.privacy.android.feature.sync.ui.views.SyncCard
import mega.privacy.android.feature.sync.ui.synclist.SyncChip.SYNC_FOLDERS
import mega.privacy.android.feature.sync.ui.synclist.SyncChip.SYNCING
import mega.privacy.android.feature.sync.ui.synclist.SyncChip.COMPLETED

@Composable
internal fun SyncListScreen(
    syncUiItems: List<SyncUiItem>,
    cardExpanded: (SyncUiItem, Boolean) -> Unit,
    pauseRunClicked: (SyncUiItem) -> Unit,
    removeFolderClicked: (folderPairId: Long) -> Unit,
    addFolderClicked: () -> Unit,
) {
    val onBackPressedDispatcher =
        LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    Scaffold(
        topBar = {
            TopAppBar(
                title = stringResource(R.string.sync_toolbar_title),
                subtitle = null,
                elevation = false,
                onBackPressed = {
                    onBackPressedDispatcher?.onBackPressed()
                }
            )
        }, content = { paddingValues ->
            SyncListScreenContent(
                Modifier
                    .padding(paddingValues)
                    .padding(16.dp),
                syncUiItems,
                cardExpanded,
                pauseRunClicked,
                removeFolderClicked,
                addFolderClicked
            )
        }
    )

}

@Composable
private fun SyncListScreenContent(
    modifier: Modifier,
    syncUiItems: List<SyncUiItem>,
    cardExpanded: (SyncUiItem, Boolean) -> Unit,
    pauseRunClicked: (SyncUiItem) -> Unit,
    removeFolderClicked: (folderPairId: Long) -> Unit,
    addFolderClicked: () -> Unit,
) {
    LazyColumn(state = LazyListState(), modifier = modifier) {
        item {
            Row(Modifier.padding(top = 8.dp, bottom = 16.dp)) {
                var checkedChip by remember { mutableStateOf(SYNC_FOLDERS) }

                PhotoChip(
                    text = "Sync folders",
                    onClick = { checkedChip = SYNC_FOLDERS },
                    isChecked = checkedChip == SYNC_FOLDERS
                )
                PhotoChip(
                    text = "Syncing",
                    onClick = { checkedChip = SYNCING },
                    Modifier.padding(horizontal = 8.dp),
                    isChecked = checkedChip == SYNCING,
                )
                PhotoChip(
                    text = "Completed",
                    onClick = { checkedChip = COMPLETED },
                    isChecked = checkedChip == COMPLETED,
                )
            }
        }
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
                SyncItem(
                    Modifier.padding(bottom = 8.dp),
                    syncUiItems,
                    itemIndex,
                    cardExpanded,
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
            onClick = { addFolderClicked() }, modifier = Modifier.padding(16.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add folder pair")
        }
    }
}

@Composable
private fun NoItemsPlaceholder(modifier: Modifier) {
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painterResource(R.drawable.no_syncs_placeholder),
            contentDescription = null,
            modifier = Modifier
                .testTag(TAG_SYNC_LIST_SCREEN_NO_ITEMS)
        )
        val annotatedString = buildAnnotatedString {
            append("No")
            withStyle(SpanStyle(color = MaterialTheme.colors.textColorPrimary)) {
                append(" Syncs")
            }
        }
        Text(
            text = annotatedString,
            modifier = Modifier
                .padding(top = 8.dp),
            style = MaterialTheme.typography.subtitle2.copy(color = MaterialTheme.colors.textColorSecondary),
        )
    }
}

@Composable
private fun SyncItem(
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

internal const val TAG_SYNC_LIST_SCREEN_NO_ITEMS = "sync_list_screen_no_items"

@CombinedThemePreviews
@Composable
private fun PreviewSyncListScreen() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        SyncListScreen(listOf(
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
                status = SyncStatus.SYNCED,
                deviceStoragePath = "deviceStoragePath",
                megaStoragePath = "megaStoragePath",
                method = "Two way sync",
                expanded = false,
            ),
        ),
            cardExpanded = { _, _ -> },
            pauseRunClicked = {},
            removeFolderClicked = {},
            addFolderClicked = {})
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewEmptyScreen() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        SyncListScreen(
            listOf(),
            cardExpanded = { _, _ -> },
            pauseRunClicked = {},
            removeFolderClicked = {},
            addFolderClicked = {}
        )
    }
}