package mega.privacy.android.app.presentation.settings.startscreen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.settings.startscreen.model.StartScreenSettingsState
import mega.privacy.android.app.presentation.settings.startscreen.view.StartScreenOptionView
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.presentation.theme.AndroidTheme
import javax.inject.Inject

/**
 * Settings fragment to choose the preferred start screen.
 */
@AndroidEntryPoint
class StartScreenSettingsFragment : Fragment() {

    private val viewModel by activityViewModels<StartScreenViewModel>()

    /**
     * Get theme mode
     */
    @Inject
    lateinit var getThemeMode: GetThemeMode

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        setContent {
            val themeMode by getThemeMode()
                .collectAsState(initial = ThemeMode.System)
            val uiState by viewModel.state.collectAsState()
            AndroidTheme(isDark = themeMode.isDarkMode()) {
                StartScreenSettingsView(uiState)
            }
        }
    }

    @Composable
    private fun StartScreenSettingsView(uiState: StartScreenSettingsState) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Top
        ) {
            uiState.options.forEach {
                StartScreenOptionView(icon = it.icon,
                    text = stringResource(id = it.title),
                    isSelected = it.startScreen == uiState.selectedScreen,
                    onClick = { viewModel.newScreenClicked(it.startScreen) })
            }
        }
    }

}