package mega.privacy.android.feature.sync.ui.synclist

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.ui.controls.appbar.LegacyTopAppBar
import mega.privacy.android.core.ui.controls.chips.PhotoChip
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.feature.sync.R
import mega.privacy.android.feature.sync.ui.synclist.SyncChip.SOLVED_ISSUES
import mega.privacy.android.feature.sync.ui.synclist.SyncChip.STALLED_ISSUES
import mega.privacy.android.feature.sync.ui.synclist.SyncChip.SYNC_FOLDERS
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersRoute
import mega.privacy.android.feature.sync.ui.synclist.stalledissues.SyncStalledIssuesRoute

@Composable
internal fun SyncListScreen(
    stalledIssuesCount: Int,
    addFolderClicked: () -> Unit,
) {
    val onBackPressedDispatcher =
        LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    Scaffold(
        topBar = {
            LegacyTopAppBar(
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
                    .padding(paddingValues),
                stalledIssuesCount,
                addFolderClicked
            )
        }
    )
}

@Composable
private fun SyncListScreenContent(
    modifier: Modifier,
    stalledIssuesCount: Int,
    addFolderClicked: () -> Unit,
) {
    var checkedChip by remember { mutableStateOf(SYNC_FOLDERS) }

    Column(modifier) {
        HeaderChips(checkedChip, stalledIssuesCount, { checkedChip = it })
        SelectedChipScreen(addFolderClicked, checkedChip)
    }
}

@Composable
private fun HeaderChips(
    selectedChip: SyncChip,
    stalledIssuesCount: Int,
    onChipSelected: (SyncChip) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.padding(16.dp)) {
        PhotoChip(
            text = "Folders",
            onClick = { onChipSelected(SYNC_FOLDERS) },
            isChecked = selectedChip == SYNC_FOLDERS
        )
        PhotoChip(
            text = if (stalledIssuesCount > 0) {
                "Stalled Issues ($stalledIssuesCount)"
            } else {
                "Stalled Issues"
            },
            onClick = { onChipSelected(STALLED_ISSUES) },
            Modifier.padding(horizontal = 8.dp),
            isChecked = selectedChip == STALLED_ISSUES,
        )
        PhotoChip(
            text = "Solved Issues",
            onClick = { onChipSelected(SOLVED_ISSUES) },
            isChecked = selectedChip == SOLVED_ISSUES,
        )
    }
}

@Composable
private fun SelectedChipScreen(
    addFolderClicked: () -> Unit,
    checkedChip: SyncChip,
) {
    when (checkedChip) {
        SYNC_FOLDERS -> {
            SyncFoldersRoute(addFolderClicked = addFolderClicked)
        }

        STALLED_ISSUES -> {
            SyncStalledIssuesRoute()
        }

        SOLVED_ISSUES -> {
            Box(modifier = Modifier.fillMaxSize())
        }
    }
}

internal const val TAG_SYNC_LIST_SCREEN_NO_ITEMS = "sync_list_screen_no_items"

@CombinedThemePreviews
@Composable
private fun PreviewSyncListScreen() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        SyncListScreen(
            stalledIssuesCount = 3,
            addFolderClicked = {}
        )
    }
}