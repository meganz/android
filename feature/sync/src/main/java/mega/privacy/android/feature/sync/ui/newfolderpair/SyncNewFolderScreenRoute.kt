package mega.privacy.android.feature.sync.ui.newfolderpair

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.feature.sync.ui.newfolderpair.SyncNewFolderAction.LocalFolderSelected
import mega.privacy.android.feature.sync.ui.newfolderpair.SyncNewFolderAction.FolderNameChanged
import mega.privacy.android.feature.sync.ui.newfolderpair.SyncNewFolderAction.NextClicked
import mega.privacy.android.feature.sync.ui.permissions.SyncPermissionsManager
import mega.privacy.mobile.analytics.event.AndroidSyncStartSyncButtonEvent

@Composable
internal fun SyncNewFolderScreenRoute(
    viewModel: SyncNewFolderViewModel,
    syncPermissionsManager: SyncPermissionsManager,
    openSelectMegaFolderScreen: () -> Unit,
    openNextScreen: (SyncNewFolderState) -> Unit,
    onBackClicked: () -> Unit,
) {
    val state = viewModel.state.collectAsStateWithLifecycle()

    SyncNewFolderScreen(
        folderPairName = state.value.folderPairName,
        selectedLocalFolder = state.value.selectedLocalFolder,
        selectedMegaFolder = state.value.selectedMegaFolder,
        localFolderSelected = { viewModel.handleAction(LocalFolderSelected(it)) },
        folderNameChanged = { viewModel.handleAction(FolderNameChanged(it)) },
        selectMegaFolderClicked = openSelectMegaFolderScreen,
        syncClicked = {
            Analytics.tracker.trackEvent(AndroidSyncStartSyncButtonEvent)
            viewModel.handleAction(NextClicked)
            openNextScreen(state.value)
        },
        syncPermissionsManager = syncPermissionsManager,
        onBackClicked = onBackClicked,
    )

    val onBack = {
        onBackClicked()
    }

    BackHandler(onBack = onBack)
}