package mega.privacy.android.feature.devicecenter.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
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

                AndroidTheme(isDark = themeMode.isDarkMode()) {
                    DeviceCenterScreen(
                        uiState = uiState,
                        onDeviceClicked = viewModel::showDeviceFolders,
                        onBackPressed = viewModel::handleBackPress,
                        onFeatureExited = viewModel::resetExitFeature,
                    )
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