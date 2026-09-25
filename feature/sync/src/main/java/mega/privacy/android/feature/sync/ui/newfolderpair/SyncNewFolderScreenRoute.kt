package mega.privacy.android.feature.sync.ui.newfolderpair

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.feature.sync.domain.entity.ParsedMegaIgnore
import mega.privacy.android.feature.sync.navigation.ManageSyncExclusionsNavKey
import mega.privacy.android.feature.sync.navigation.ManageSyncExclusionsResult
import mega.privacy.android.feature.sync.navigation.NavigationResultEffect
import mega.privacy.android.feature.sync.ui.newfolderpair.SyncNewFolderAction.NextClicked
import mega.privacy.android.shared.sync.ui.permissions.SyncPermissionsManager
import mega.privacy.mobile.analytics.event.AndroidSyncLocalFolderSelectedEvent
import mega.privacy.mobile.analytics.event.AndroidSyncSetupManageExclusionsButtonPressedEvent
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
    onManageExclusions: (UriPath, ParsedMegaIgnore?) -> Unit,
    monitorResult: (String) -> Flow<Any?>,
    clearResult: (String) -> Unit,
    onSelectFolder: () -> Unit = {},
) {
    val state = viewModel.state.collectAsStateWithLifecycle()

    SyncNewFolderScreen(
        selectedLocalFolder = state.value.selectedFolderName,
        selectedLocalFolderUri = state.value.selectedLocalFolder,
        selectedMegaFolder = state.value.selectedMegaFolder,
        onSelectFolder = {
            Analytics.tracker.trackEvent(AndroidSyncLocalFolderSelectedEvent)
            onSelectFolder()
        },
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
        showExclusionsRow = state.value.isExclusionsEnabled &&
                state.value.selectedLocalFolder.isNotEmpty(),
        onManageExclusionsClicked = {
            Analytics.tracker.trackEvent(AndroidSyncSetupManageExclusionsButtonPressedEvent)
            onManageExclusions(
                UriPath(state.value.selectedLocalFolder),
                state.value.stagedExclusions,
            )
        },
        viewModel = viewModel,
    )

    EventEffect(event = state.value.openSyncListScreen, onConsumed = {
        viewModel.handleAction(SyncNewFolderAction.SyncListScreenOpened)
    }) {
        openNextScreen(state.value)
    }

    NavigationResultEffect(
        key = ManageSyncExclusionsNavKey.RESULT,
        monitorResult = monitorResult,
        clearResult = clearResult,
    ) { result ->
        (result as? ManageSyncExclusionsResult)?.let {
            viewModel.handleAction(SyncNewFolderAction.ExclusionsStaged(it.exclusions))
        }
    }

    val onBack = {
        onBackClicked()
    }

    BackHandler(onBack = onBack)
}
