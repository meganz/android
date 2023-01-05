package mega.privacy.android.app.presentation.settings.calls

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.core.ui.theme.AndroidTheme
import javax.inject.Inject

/**
 * Fragment of [SettingsCallsActivity] which allows to change the calls settings.
 */
@AndroidEntryPoint
class SettingsCallsFragment : Fragment() {

    private val viewModel: SettingsCallsViewModel by viewModels()

    @Inject
    lateinit var getThemeMode: GetThemeMode

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setContent {
            val themeMode by getThemeMode()
                .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            AndroidTheme(isDark = themeMode.isDarkMode()) {
                CallsSettingBody()
            }
        }
    }

    @Composable
    private fun CallsSettingBody() {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        SettingsCallsView(
            settingsCallsState = uiState,
            onCheckedChange = viewModel::setNewCallsSoundNotifications
        )
    }
}
