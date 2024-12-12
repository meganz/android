package mega.privacy.android.feature.chat.settings.calls

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import mega.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.feature.chat.settings.navigation.CallSettingsGraph
import mega.privacy.android.feature.chat.settings.navigation.callSettingsNavigationGraph
import javax.inject.Inject

@AndroidEntryPoint
class CallSettingsFragment : Fragment() {
    @Inject
    lateinit var getThemeMode: GetThemeMode

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            val mode by getThemeMode().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            val onBackPressed = { requireActivity().supportFinishAfterTransition() }
            AndroidTheme(mode.isDarkMode()) {
                NavHost(
                    navController = rememberNavController(),
                    startDestination = CallSettingsGraph
                ) {
                    callSettingsNavigationGraph(onBackPressed = onBackPressed)
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
}