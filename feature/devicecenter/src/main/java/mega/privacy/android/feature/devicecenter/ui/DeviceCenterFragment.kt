package mega.privacy.android.feature.devicecenter.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.feature.devicecenter.navigation.deviceCenterInfoNavGraph
import mega.privacy.android.feature.devicecenter.navigation.deviceCenterRoute
import mega.privacy.android.feature.devicecenter.ui.model.DeviceCenterUINode
import mega.privacy.android.feature.devicecenter.ui.model.DeviceMenuAction
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.mobile.analytics.event.AndroidSyncNavigationItemEvent
import mega.privacy.mobile.analytics.event.DeviceCenterDeviceOptionsButtonEvent
import mega.privacy.mobile.analytics.event.DeviceCenterItemClicked
import mega.privacy.mobile.analytics.event.DeviceCenterItemClickedEvent
import timber.log.Timber
import javax.inject.Inject

/**
 * [Fragment] that is the entrypoint to the Device Center feature. The Device Center shows the list
 * of Devices that have uploaded content through Camera Uploads or Backups.
 *
 * Selecting a Device will display the list of Device Folders
 */
@AndroidEntryPoint
class DeviceCenterFragment : Fragment() {

    /**
     * Retrieves the Device Theme Mode
     */
    @Inject
    lateinit var getThemeMode: GetThemeMode

    /**
     * Allows navigation to specific features in the monolith :app
     */
    @Inject
    lateinit var megaNavigator: MegaNavigator

    /**
     * The View Model for the Device Center feature
     */
    private val viewModel by viewModels<DeviceCenterViewModel>()

    /**
     * Initialize the Device Center
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val uiState by viewModel.state.collectAsStateWithLifecycle()
                val themeMode by getThemeMode()
                    .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
                val snackbarHostState = remember { SnackbarHostState() }

                // Used to show a blank overlay when a folder is clicked
                // so two toolbars are not visible at the same time
                var showBlankOverlay by remember {
                    mutableStateOf(false)
                }

                LaunchedEffect(showBlankOverlay) {
                    // Automatically hide overlay after it's enabled
                    if (showBlankOverlay) {
                        delay(200)
                        showBlankOverlay = false
                    }
                }

                OriginalTempTheme(isDark = themeMode.isDarkMode()) {
                    DeviceCenterScreen(
                        uiState = uiState,
                        snackbarHostState = snackbarHostState,
                        onDeviceClicked = { device ->
                            Analytics.tracker.trackEvent(
                                DeviceCenterItemClickedEvent(
                                    DeviceCenterItemClicked.ItemType.Device
                                )
                            )
                            if (viewModel.shouldNavigateToSyncs(device)) {
                                Analytics.tracker.trackEvent(AndroidSyncNavigationItemEvent)
                                megaNavigator.openSyncs(
                                    context = this@DeviceCenterFragment.activity
                                        ?: return@DeviceCenterScreen,
                                    deviceName = device.name,
                                )
                            } else {
                                viewModel.showDeviceFolders(device)
                            }
                        },
                        onDeviceMenuClicked = {
                            Analytics.tracker.trackEvent(
                                DeviceCenterDeviceOptionsButtonEvent
                            )
                            viewModel.setMenuClickedDevice(it)
                        },
                        onBackupFolderClicked = { backupFolderUINode ->
                            Analytics.tracker.trackEvent(
                                DeviceCenterItemClickedEvent(
                                    DeviceCenterItemClicked.ItemType.Connection
                                )
                            )
                            showBlankOverlay = true
                            megaNavigator.openNodeInBackups(
                                activity = this@DeviceCenterFragment.activity
                                    ?: return@DeviceCenterScreen,
                                backupsHandle = backupFolderUINode.rootHandle,
                                errorMessage = backupFolderUINode.status.localizedErrorMessage,
                            )
                        },
                        onNonBackupFolderClicked = { nonBackupDeviceFolderUINode ->
                            Analytics.tracker.trackEvent(
                                DeviceCenterItemClickedEvent(
                                    DeviceCenterItemClicked.ItemType.Connection
                                )
                            )
                            showBlankOverlay = true
                            megaNavigator.openNodeInCloudDrive(
                                activity = this@DeviceCenterFragment.activity
                                    ?: return@DeviceCenterScreen,
                                nodeHandle = nonBackupDeviceFolderUINode.rootHandle,
                                errorMessage = nonBackupDeviceFolderUINode.status.localizedErrorMessage,
                            )
                        },
                        onInfoOptionClicked = viewModel::onInfoClicked,
                        onAddNewSyncOptionClicked = { device ->
                            megaNavigator.openNewSync(
                                context = this@DeviceCenterFragment.activity
                                    ?: return@DeviceCenterScreen,
                                deviceName = device.name,
                                syncType = SyncType.TYPE_TWOWAY,
                            )
                        },
                        onAddBackupOptionClicked = { device ->
                            megaNavigator.openNewSync(
                                context = this@DeviceCenterFragment.activity
                                    ?: return@DeviceCenterScreen,
                                deviceName = device.name,
                                syncType = SyncType.TYPE_BACKUP,
                            )
                        },
                        onRenameDeviceOptionClicked = viewModel::setDeviceToRename,
                        onRenameDeviceCancelled = viewModel::resetDeviceToRename,
                        onRenameDeviceSuccessful = {
                            viewModel.handleRenameDeviceSuccess()
                            viewModel.getBackupInfo()
                        },
                        onRenameDeviceSuccessfulSnackbarShown = viewModel::resetRenameDeviceSuccessEvent,
                        onBackPressHandled = viewModel::handleBackPress,
                        onFeatureExited = viewModel::resetExitFeature,
                        onSearchQueryChanged = viewModel::onSearchQueryChanged,
                        onSearchCloseClicked = viewModel::onSearchCloseClicked,
                        onSearchClicked = viewModel::onSearchClicked,
                        onActionPressed = { menuAction ->
                            when (menuAction) {
                                is DeviceMenuAction.Rename -> {
                                    uiState.selectedDevice?.let { device ->
                                        viewModel.setDeviceToRename(deviceToRename = device)
                                    }
                                }

                                is DeviceMenuAction.Info -> {
                                    uiState.selectedDevice?.let { device ->
                                        viewModel.onInfoClicked(selectedItem = device)
                                    }
                                }

                                is DeviceMenuAction.CameraUploads -> {
                                    megaNavigator.openSettingsCameraUploads(requireActivity())
                                }
                            }
                        },
                        onOpenUpgradeAccountClicked = {
                            megaNavigator.openUpgradeAccount(requireContext())
                        },
                    )
                    if (showBlankOverlay) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colors.background)
                        )
                    }
                    if (uiState.infoSelectedItem != null) {
                        val animatedNavController = rememberNavController()
                        NavHost(
                            navController = animatedNavController,
                            startDestination = deviceCenterRoute,
                        ) {
                            deviceCenterInfoNavGraph(
                                navController = animatedNavController,
                                selectedItem = uiState.infoSelectedItem as DeviceCenterUINode,
                                onBackPressHandled = viewModel::onInfoBackPressHandle
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Setup Lifecycle-aware Observers
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewLifecycleOwner.lifecycleScope.launch {
            // Only refresh the User's Backup Information when the app is in the Foreground
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.refreshBackupInfoPromptFlow.collect {
                    Timber.d("Refreshing the User's Backup Information")
                    viewModel.getBackupInfo()
                }
            }
        }
    }

    /**
     * Checks the Dark Mode Configuration
     *
     * @return true if the Device is currently in Dark Mode, and false if otherwise
     */
    @Composable
    fun ThemeMode.isDarkMode() = when (this) {
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
        ThemeMode.System -> isSystemInDarkTheme()
    }

    companion object {
        /**
         * Creates a new instance of the Device Center
         *
         * @return The Device Center
         */
        @JvmStatic
        fun newInstance() = DeviceCenterFragment()
    }
}