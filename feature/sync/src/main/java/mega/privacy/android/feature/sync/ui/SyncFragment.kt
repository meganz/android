package mega.privacy.android.feature.sync.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.feature.sync.navigation.syncNavGraph
import mega.privacy.android.feature.sync.navigation.syncRoute
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

    private val viewModel by viewModels<SyncViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        (requireActivity() as AppCompatActivity).supportActionBar?.title = "Sync"
        return ComposeView(requireContext()).apply {
            setContent {
                val animatedNavController = rememberAnimatedNavController()
                val themeMode by getThemeMode().collectAsStateWithLifecycle(initialValue = ThemeMode.System)

                val state by viewModel.state.collectAsStateWithLifecycle()

                state.showOnboarding?.let { showOnboarding ->
                    AndroidTheme(isDark = themeMode.isDarkMode()) {
                        AnimatedNavHost(
                            navController = animatedNavController,
                            startDestination = syncRoute,
                            enterTransition = { EnterTransition.None },
                            exitTransition = { ExitTransition.None },
                            popEnterTransition = { EnterTransition.None },
                            popExitTransition = { ExitTransition.None },
                        ) {
                            syncNavGraph(
                                showOnboarding, animatedNavController
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