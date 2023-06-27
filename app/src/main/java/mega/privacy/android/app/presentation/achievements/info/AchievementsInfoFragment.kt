package mega.privacy.android.app.presentation.achievements.info

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.presentation.achievements.info.view.AchievementsInfoScreen
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import javax.inject.Inject

/**
 * AchievementsInfoFragment
 */
@AndroidEntryPoint
class AchievementsInfoFragment : Fragment() {
    /**
     * Get system's default theme mode
     */
    @Inject
    lateinit var getThemeMode: GetThemeMode

    private val viewModel: AchievementsInfoViewModel by viewModels()

    /**
     * onCreateView
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            val themeMode by getThemeMode()
                .collectAsStateWithLifecycle(initialValue = ThemeMode.System)

            AndroidTheme(isDark = themeMode.isDarkMode()) {
                AchievementsInfoScreen(viewModel = viewModel)
            }
        }
    }
}
