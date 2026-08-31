package mega.privacy.android.feature.sync.ui.synclist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersUiState
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersViewModel
import mega.privacy.android.shared.original.core.ui.controls.buttons.MultiFloatingActionButtonState
import mega.privacy.android.shared.original.core.ui.controls.buttons.rememberMultiFloatingActionButtonState

/**
 * Expand state of the sync list FAB, shared between [SyncListTabFab] and the tab content that
 * collapses it on tap. The host screen owns it because the two live in different slots of the
 * same scaffold.
 */
@Stable
class SyncListFabState internal constructor(
    internal val multiFabState: MutableState<MultiFloatingActionButtonState>,
)

/**
 * Creates the [SyncListFabState] a host screen passes to both [SyncListTabFab] and
 * [SyncListRoute].
 */
@Composable
fun rememberSyncListFabState(): SyncListFabState {
    val multiFabState = rememberMultiFloatingActionButtonState()
    return remember(multiFabState) { SyncListFabState(multiFabState) }
}

/**
 * Sync list FAB for a host screen that shows the sync list as a tab.
 *
 * It belongs in the host scaffold's floating action button slot: a scaffold only lays its
 * snackbars out above a FAB that occupies that slot.
 *
 * @param fabState Shared with the [SyncListRoute] of the same tab.
 */
@Composable
fun SyncListTabFab(
    fabState: SyncListFabState,
    onSyncFolderClicked: () -> Unit,
    onBackupFolderClicked: () -> Unit,
    modifier: Modifier = Modifier,
    onFabExpanded: (Boolean) -> Unit = {},
) {
    val syncFoldersViewModel: SyncFoldersViewModel =
        hiltViewModel(viewModelStoreOwner = syncListViewModelStoreOwner())
    val syncFoldersUiState by syncFoldersViewModel.uiState.collectAsStateWithLifecycle()

    SyncListTabFab(
        fabState = fabState,
        syncFoldersUiState = syncFoldersUiState,
        onSyncFolderClicked = onSyncFolderClicked,
        onBackupFolderClicked = onBackupFolderClicked,
        modifier = modifier,
        onFabExpanded = onFabExpanded,
    )
}

@Composable
internal fun SyncListTabFab(
    fabState: SyncListFabState,
    syncFoldersUiState: SyncFoldersUiState,
    onSyncFolderClicked: () -> Unit,
    onBackupFolderClicked: () -> Unit,
    modifier: Modifier = Modifier,
    onFabExpanded: (Boolean) -> Unit = {},
) {
    FabExpandedEffect(fabState.multiFabState, onFabExpanded)

    SyncListFab(
        modifier = modifier,
        syncFoldersUiState = syncFoldersUiState,
        multiFabState = fabState.multiFabState,
        onSyncFolderClicked = onSyncFolderClicked,
        onBackupFolderClicked = onBackupFolderClicked,
        onFabExpanded = onFabExpanded,
    )
}
