package mega.privacy.android.feature.sync.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.feature.sync.navigation.syncNavGraph
import mega.privacy.android.feature.sync.navigation.syncRoute
import mega.privacy.android.feature.sync.ui.mapper.FileTypeIconMapper
import javax.inject.Inject

/**
 * Screen for syncing local folder with MEGA
 */
@OptIn(ExperimentalAnimationApi::class)
@AndroidEntryPoint
class SyncFragment : Fragment() {

    companion object {
        /**
         * Returns the instance of SyncFragment
         */
        @JvmStatic
        fun newInstance(): SyncFragment = SyncFragment()
    }

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

    private val viewModel by viewModels<SyncViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val animatedNavController = rememberNavController()
                val themeMode by getThemeMode().collectAsStateWithLifecycle(initialValue = ThemeMode.System)

                val state by viewModel.state.collectAsStateWithLifecycle()

                state.showOnboarding?.let { showOnboarding ->
                    AndroidTheme(isDark = themeMode.isDarkMode()) {
                        NavHost(
                            navController = animatedNavController,
                            startDestination = syncRoute,
                        ) {
                            syncNavGraph(
                                showOnboarding, animatedNavController, fileTypeIconMapper
                            )
                        }
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