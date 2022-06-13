package mega.privacy.android.app.presentation.settings.chat.imagequality

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.domain.entity.ThemeMode
import mega.privacy.android.app.domain.usecase.GetThemeMode
import mega.privacy.android.app.presentation.theme.AndroidTheme
import javax.inject.Inject

/**
 * Fragment of [SettingsChatImageQualityActivity] which allows to change the chat image quality setting.
 */
@AndroidEntryPoint
class SettingsChatImageQualityFragment : Fragment() {

    private val viewModel: SettingsChatImageQualityViewModel by viewModels()

    @Inject
    lateinit var getThemeMode: GetThemeMode

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setContent {
            AndroidTheme(getThemeMode().collectAsState(initial = ThemeMode.System).value) {
                ChatImageQualitySettingBody()
            }
        }
    }

    @Composable
    private fun ChatImageQualitySettingBody() {
        val uiState by viewModel.state.collectAsState()
        ChatImageQualityView(
            settingsChatImageQualityState = uiState,
            onOptionChanged = viewModel::setNewChatImageQuality
        )
    }
}