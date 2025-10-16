package mega.privacy.android.feature.devicecenter.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.launch
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.feature.devicecenter.ui.DeviceCenterViewModel
import mega.privacy.android.feature.devicecenter.ui.model.DeviceMenuAction
import mega.privacy.android.feature.devicecenter.ui.view.DeviceCenterAppBarM3
import mega.privacy.android.feature.devicecenter.ui.view.DeviceCenterScreenM3
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.destination.DeviceCenterNavKey
import mega.privacy.mobile.analytics.event.AndroidSyncNavigationItemEvent
import mega.privacy.mobile.analytics.event.DeviceCenterDeviceOptionsButtonEvent
import mega.privacy.mobile.analytics.event.DeviceCenterItemClicked
import mega.privacy.mobile.analytics.event.DeviceCenterItemClickedEvent

@OptIn(ExperimentalMaterial3Api::class)
fun EntryProviderScope<NavKey>.deviceCenterScreen(
    navigationHandler: NavigationHandler,
    onNavigateToBackupFolder: (handle: Long, errorMessage: Int?) -> Unit,
    onNavigateToNonBackupFolder: (handle: Long, errorMessage: Int?) -> Unit,
    onNavigateToSyncs: () -> Unit,
    onNavigateToNewSync: (syncType: SyncType) -> Unit,
    onNavigateToCameraUploads: () -> Unit,
) {
    entry<DeviceCenterNavKey> {
        val viewModel = hiltViewModel<DeviceCenterViewModel>()
        val uiState by viewModel.state.collectAsStateWithLifecycle()
        val lifecycleOwner = LocalLifecycleOwner.current
        val coroutineScope = rememberCoroutineScope()

        BackHandler(enabled = uiState.selectedDevice != null) {
            viewModel.handleBackPress()
        }

        LifecycleResumeEffect(Unit, lifecycleOwner = lifecycleOwner) {
            val job = coroutineScope.launch {
                viewModel.refreshBackupInfoPromptFlow.collect {
                    viewModel.getBackupInfo()
                }
            }
            onPauseOrDispose {
                job.cancel()
            }
        }

        var isSearchMode by rememberSaveable { mutableStateOf(false) }

        MegaScaffoldWithTopAppBarScrollBehavior(
            modifier = Modifier
                .fillMaxSize()
                .semantics { testTagsAsResourceId = true }
                .navigationBarsPadding(),
            topBar = {
                DeviceCenterAppBarM3(
                    uiState = uiState,
                    selectedDevice = uiState.selectedDevice,
                    isSearchMode = isSearchMode,
                    onBackPressed = {
                        navigationHandler.back()
                    },
                    onActionPressed = { menuAction ->
                        when (menuAction) {
                            is DeviceMenuAction.Rename -> {
                                uiState.selectedDevice?.let { device ->
                                    viewModel.setDeviceToRename(device)
                                }
                            }

                            is DeviceMenuAction.Info -> {
                                uiState.selectedDevice?.let { device ->
                                    viewModel.onInfoClicked(device)
                                }
                            }

                            is DeviceMenuAction.CameraUploads -> {
                                onNavigateToCameraUploads()
                            }
                        }
                    },
                    onSearchQueryChanged = viewModel::onSearchQueryChanged,
                    onSearchModeChanged = { searching ->
                        isSearchMode = searching
                        if (!searching) {
                            viewModel.onSearchCloseClicked()
                        }
                    },
                )
            },
        ) { innerPadding ->
            DeviceCenterScreenM3(
                uiState = uiState,
                onDeviceClicked = { device ->
                    Analytics.tracker.trackEvent(
                        DeviceCenterItemClickedEvent(
                            DeviceCenterItemClicked.ItemType.Device
                        )
                    )
                    if (viewModel.shouldNavigateToSyncs(device)) {
                        Analytics.tracker.trackEvent(AndroidSyncNavigationItemEvent)
                        onNavigateToSyncs()
                    } else {
                        viewModel.showDeviceFolders(device)
                    }
                },
                onDeviceMenuClicked = {
                    Analytics.tracker.trackEvent(DeviceCenterDeviceOptionsButtonEvent)
                    viewModel.setMenuClickedDevice(it)
                },
                onBackupFolderClicked = { backupFolderUINode ->
                    Analytics.tracker.trackEvent(
                        DeviceCenterItemClickedEvent(
                            DeviceCenterItemClicked.ItemType.Connection
                        )
                    )
                    onNavigateToBackupFolder(
                        backupFolderUINode.rootHandle,
                        backupFolderUINode.status.localizedErrorMessage
                    )
                },
                onNonBackupFolderClicked = { nonBackupDeviceFolderUINode ->
                    Analytics.tracker.trackEvent(
                        DeviceCenterItemClickedEvent(
                            DeviceCenterItemClicked.ItemType.Connection
                        )
                    )
                    onNavigateToNonBackupFolder(
                        nonBackupDeviceFolderUINode.rootHandle,
                        nonBackupDeviceFolderUINode.status.localizedErrorMessage
                    )
                },
                onCameraUploadsClicked = onNavigateToCameraUploads,
                onInfoOptionClicked = viewModel::onInfoClicked,
                onAddNewSyncOptionClicked = { onNavigateToNewSync(SyncType.TYPE_TWOWAY) },
                onAddBackupOptionClicked = { onNavigateToNewSync(SyncType.TYPE_BACKUP) },
                onRenameDeviceOptionClicked = viewModel::setDeviceToRename,
                onRenameDeviceCancelled = viewModel::resetDeviceToRename,
                onRenameDeviceSuccessful = {
                    viewModel.handleRenameDeviceSuccess()
                    viewModel.getBackupInfo()
                },
                onRenameDeviceSuccessfulSnackbarShown = viewModel::resetRenameDeviceSuccessEvent,
                onBackPressHandled = {
                    if (uiState.selectedDevice != null) {
                        viewModel.handleBackPress()
                    } else {
                        navigationHandler.back()
                    }
                },
                onFeatureExited = navigationHandler::back,
                paddingValues = innerPadding,
            )
            uiState.infoSelectedItem?.let { selectedItem ->
                val animatedNavController = rememberNavController()
                NavHost(
                    navController = animatedNavController,
                    startDestination = DeviceCenter,
                ) {
                    deviceCenterInfoNavGraph(
                        navController = animatedNavController,
                        selectedItem = selectedItem,
                        onBackPressHandled = viewModel::onInfoBackPressHandle
                    )
                }
            }
        }
    }
}
