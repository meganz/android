package mega.privacy.android.feature.sync.ui.newfolderpair

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.palm.composestateevents.EventEffect
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.feature.sync.ui.newfolderpair.SyncNewFolderAction.LocalFolderSelected
import mega.privacy.android.feature.sync.ui.newfolderpair.SyncNewFolderAction.NextClicked
import mega.privacy.android.feature.sync.ui.permissions.SyncPermissionsManager
import mega.privacy.mobile.analytics.event.AndroidSyncStartSyncButtonEvent
import mega.privacy.mobile.analytics.event.SyncNewFolderScreenBackNavigationEvent

@Composable
internal fun SyncNewFolderScreenRoute(
    viewModel: SyncNewFolderViewModel,
    syncPermissionsManager: SyncPermissionsManager,
    openSelectMegaFolderScreen: () -> Unit,
    openNextScreen: (SyncNewFolderState) -> Unit,
    openUpgradeAccount: () -> Unit,
    onBackClicked: () -> Unit,
) {
    val state = viewModel.state.collectAsStateWithLifecycle()

    SyncNewFolderScreen(
        selectedLocalFolder = state.value.selectedLocalFolder,
        selectedMegaFolder = state.value.selectedMegaFolder,
        localFolderSelected = { viewModel.handleAction(LocalFolderSelected(it)) },
        selectMegaFolderClicked = openSelectMegaFolderScreen,
        syncClicked = {
            Analytics.tracker.trackEvent(AndroidSyncStartSyncButtonEvent)
            viewModel.handleAction(NextClicked)
        },
        syncPermissionsManager = syncPermissionsManager,
        onBackClicked = {
            Analytics.tracker.trackEvent(SyncNewFolderScreenBackNavigationEvent)
            onBackClicked()
        },
        showStorageOverQuota = state.value.showStorageOverQuota,
        onDismissStorageOverQuota = { viewModel.handleAction(SyncNewFolderAction.StorageOverquotaShown) },
        onOpenUpgradeAccount = { openUpgradeAccount() },
        viewModel = viewModel,
    )

    EventEffect(event = state.value.openSyncListScreen, onConsumed = {
        viewModel.handleAction(SyncNewFolderAction.SyncListScreenOpened)
    }) {
        openNextScreen(state.value)
    }

    val onBack = {
        onBackClicked()
    }

    BackHandler(onBack = onBack)
}