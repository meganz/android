package mega.privacy.android.feature.sync.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.core.ui.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.feature.sync.ui.permissions.SyncPermissionsManager
import mega.privacy.android.feature.sync.ui.views.SyncScreen
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import javax.inject.Inject

/**
 * Screen for syncing local folder with MEGA
 */
@AndroidEntryPoint
class SyncFragment : Fragment() {

    /**
     * Allows navigation to specific features in the monolith :app
     */
    @Inject
    lateinit var megaNavigator: MegaNavigator

    /**
     * Get Theme Mode
     */
    @Inject
    lateinit var monitorThemeModeUseCase: MonitorThemeModeUseCase

    /**
     * Get [FileTypeIconMapper]
     */
    @Inject
    lateinit var fileTypeIconMapper: FileTypeIconMapper

    /**
     * Get [SyncPermissionsManager]
     */
    @Inject
    lateinit var syncPermissionsManager: SyncPermissionsManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val animatedNavController = rememberNavController()
                val themeMode by monitorThemeModeUseCase().collectAsStateWithLifecycle(initialValue = ThemeMode.System)

                OriginalTheme(isDark = themeMode.isDarkMode()) {
                    val viewModel = hiltViewModel<SyncViewModel>()
                    val state by viewModel.state.collectAsStateWithLifecycle()
                    SyncScreen(
                        state = state,
                        navController = animatedNavController,
                        megaNavigator = megaNavigator,
                        fileTypeIconMapper = fileTypeIconMapper,
                        syncPermissionsManager = syncPermissionsManager,
                        onBackPressed = { requireActivity().onBackPressed() },
                        shouldNavigateToSyncList = activity?.intent?.getBooleanExtra(
                            SyncHostActivity.EXTRA_IS_FROM_CLOUD_DRIVE, false
                        ) == false,
                        newFolderDetail = activity?.intent?.getParcelableExtra(
                            SyncHostActivity.EXTRA_NEW_FOLDER_DETAIL,
                        ),
                        shouldOpenStopBackup = activity?.intent?.getBooleanExtra(
                            SyncHostActivity.EXTRA_OPEN_SELECT_STOP_BACKUP_DESTINATION, false
                        ) == true,
                    )
                }
            }
        }
    }
}


