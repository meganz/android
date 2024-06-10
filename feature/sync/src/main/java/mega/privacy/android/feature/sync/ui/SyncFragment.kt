package mega.privacy.android.feature.sync.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.core.ui.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.feature.sync.navigation.syncNavGraph
import mega.privacy.android.feature.sync.navigation.syncRoute
import mega.privacy.android.feature.sync.ui.permissions.SyncPermissionsManager
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import javax.inject.Inject

/**
 * Screen for syncing local folder with MEGA
 */
@AndroidEntryPoint
class SyncFragment : Fragment() {

    companion object {
        private const val TITLE_KEY = "titleKey"
        private const val OPEN_NEW_SYNC_KEY = "openNewSyncKey"

        /**
         * Returns the instance of SyncFragment
         */
        @JvmStatic
        fun newInstance(title: String? = null, openNewSync: Boolean = false): SyncFragment {
            val args = Bundle().apply {
                putString(TITLE_KEY, title)
                putBoolean(OPEN_NEW_SYNC_KEY, openNewSync)
            }
            return SyncFragment().apply { arguments = args }
        }
    }

    /**
     * Allows navigation to specific features in the monolith :app
     */
    @Inject
    lateinit var megaNavigator: MegaNavigator

    /**
     * Get Theme Mode
     */
    @Inject
    lateinit var getThemeMode: GetThemeMode

    /**
     * Get fileTypeIconMapper
     */
    @Inject
    lateinit var fileTypeIconMapper: FileTypeIconMapper

    @Inject
    lateinit var syncPermissionsManager: SyncPermissionsManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val title = arguments?.getString(TITLE_KEY)
                val openNewSync = arguments?.getBoolean(OPEN_NEW_SYNC_KEY) ?: false
                val animatedNavController = rememberNavController()
                val themeMode by getThemeMode().collectAsStateWithLifecycle(initialValue = ThemeMode.System)

                OriginalTempTheme(isDark = themeMode.isDarkMode()) {
                    NavHost(
                        navController = animatedNavController,
                        startDestination = syncRoute,
                    ) {
                        syncNavGraph(
                            navController = animatedNavController,
                            fileTypeIconMapper = fileTypeIconMapper,
                            syncPermissionsManager = syncPermissionsManager,
                            openUpgradeAccountPage = {
                                megaNavigator.openUpgradeAccount(requireContext())
                            },
                            title = title,
                            openNewSync = openNewSync,
                        )
                    }
                }
            }
        }
    }

    /**
     * Is current theme mode a dark theme
     */
    @Composable
    fun ThemeMode.isDarkMode() = when (this) {
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
        ThemeMode.System -> isSystemInDarkTheme()
    }
}