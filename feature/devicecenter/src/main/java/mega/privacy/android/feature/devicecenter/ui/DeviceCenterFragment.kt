package mega.privacy.android.feature.devicecenter.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.shared.theme.MegaAppTheme
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

                MegaAppTheme(isDark = themeMode.isDarkMode()) {
                    DeviceCenterScreen(
                        uiState = uiState,
                        snackbarHostState = snackbarHostState,
                        onDeviceClicked = viewModel::showDeviceFolders,
                        onDeviceMenuClicked = viewModel::setMenuClickedDevice,
                        onBackupFolderClicked = { backupFolderUINode ->
                            megaNavigator.openNodeInBackups(
                                activity = this@DeviceCenterFragment.activity
                                    ?: return@DeviceCenterScreen,
                                backupsHandle = backupFolderUINode.rootHandle,
                            )
                        },
                        onBackupFolderMenuClicked = { backupDeviceFolderUINode ->
                            megaNavigator.openDeviceCenterFolderNodeOptions(
                                activity = this@DeviceCenterFragment.activity
                                    ?: return@DeviceCenterScreen,
                                isBackupsFolder = true,
                                nodeName = backupDeviceFolderUINode.name,
                                nodeHandle = backupDeviceFolderUINode.rootHandle,
                                nodeStatus = getString(backupDeviceFolderUINode.status.name),
                                nodeStatusColorInt = backupDeviceFolderUINode.status.color?.toArgb(),
                                nodeIcon = backupDeviceFolderUINode.icon.iconRes,
                                nodeStatusIcon = backupDeviceFolderUINode.status.icon,
                            )
                        },
                        onNonBackupFolderMenuClicked = { nonBackupDeviceFolderUINode ->
                            megaNavigator.openDeviceCenterFolderNodeOptions(
                                activity = this@DeviceCenterFragment.activity
                                    ?: return@DeviceCenterScreen,
                                isBackupsFolder = false,
                                nodeName = nonBackupDeviceFolderUINode.name,
                                nodeHandle = nonBackupDeviceFolderUINode.rootHandle,
                                nodeStatus = getString(nonBackupDeviceFolderUINode.status.name),
                                nodeStatusColorInt = nonBackupDeviceFolderUINode.status.color?.toArgb(),
                                nodeIcon = nonBackupDeviceFolderUINode.icon.iconRes,
                                nodeStatusIcon = nonBackupDeviceFolderUINode.status.icon,
                            )
                        },
                        onCameraUploadsClicked = {
                            megaNavigator.openSettingsCameraUploads(requireActivity())
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
                    )
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