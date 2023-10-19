package mega.privacy.android.feature.sync.ui.synclist

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import mega.privacy.android.core.ui.controls.appbar.AppBarType
import mega.privacy.android.core.ui.controls.appbar.MegaAppBar
import mega.privacy.android.core.ui.controls.chips.PhotoChip
import mega.privacy.android.core.ui.controls.sheets.BottomSheet
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.feature.sync.R
import mega.privacy.android.feature.sync.ui.model.StalledIssueDetailedInfo
import mega.privacy.android.feature.sync.ui.model.StalledIssueUiItem
import mega.privacy.android.feature.sync.ui.synclist.SyncChip.SOLVED_ISSUES
import mega.privacy.android.feature.sync.ui.synclist.SyncChip.STALLED_ISSUES
import mega.privacy.android.feature.sync.ui.synclist.SyncChip.SYNC_FOLDERS
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersRoute
import mega.privacy.android.feature.sync.ui.synclist.stalledissues.SyncStalledIssuesRoute
import mega.privacy.android.feature.sync.ui.views.ConflictDetailsDialog

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun SyncListScreen(
    stalledIssuesCount: Int,
    addFolderClicked: () -> Unit,
) {
    val onBackPressedDispatcher =
        LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    val coroutineScope = rememberCoroutineScope()
    val modalSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        confirmValueChange = { _ ->
            true
        }
    )

    BottomSheet(
        modalSheetState = modalSheetState,
        scrimColor = Color.Black.copy(alpha = 0.32f),
        sheetBody = {
            ConflictDetailsDialog(
                getMockStalledIssueDetailedInfo().title,
                getMockStalledIssueDetailedInfo().explanation
            )
        }
    ) {
        Scaffold(
            topBar = {
                MegaAppBar(
                    title = stringResource(R.string.sync_toolbar_title),
                    subtitle = null,
                    appBarType = AppBarType.BACK_NAVIGATION,
                    elevation = 0.dp,
                    onNavigationPressed = {
                        onBackPressedDispatcher?.onBackPressed()
                    }
                )
            }, content = { paddingValues ->
                SyncListScreenContent(
                    Modifier
                        .padding(paddingValues),
                    stalledIssuesCount,
                    stalledIssuesDetailsClicked = {
                        coroutineScope.launch {
                            modalSheetState.show()
                        }
                    },
                    addFolderClicked
                )
            }
        )
    }
}

@Composable
private fun SyncListScreenContent(
    modifier: Modifier,
    stalledIssuesCount: Int,
    stalledIssuesDetailsClicked: (StalledIssueUiItem) -> Unit,
    addFolderClicked: () -> Unit,
) {
    var checkedChip by remember { mutableStateOf(SYNC_FOLDERS) }

    Column(modifier) {
        HeaderChips(checkedChip, stalledIssuesCount, { checkedChip = it })
        SelectedChipScreen(addFolderClicked, stalledIssuesDetailsClicked, checkedChip)
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
    stalledIssueDetailsClicked: (StalledIssueUiItem) -> Unit,
    checkedChip: SyncChip,
) {
    when (checkedChip) {
        SYNC_FOLDERS -> {
            SyncFoldersRoute(addFolderClicked = addFolderClicked)
        }

        STALLED_ISSUES -> {
            SyncStalledIssuesRoute(stalledIssueDetailsClicked)
        }

        SOLVED_ISSUES -> {
            Box(modifier = Modifier.fillMaxSize())
        }
    }
}

private fun getMockStalledIssueDetailedInfo(): StalledIssueDetailedInfo =
    StalledIssueDetailedInfo(
        "Conflict A", "This folders contain multiple names " +
                "on one side, that would all become the same single name on the other side. This may" +
                " be due to syncing to case sensitive local filesystem, or the effects os " +
                "escaped characters."
    )

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