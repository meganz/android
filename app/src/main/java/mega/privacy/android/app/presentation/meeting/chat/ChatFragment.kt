package mega.privacy.android.app.presentation.meeting.chat

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
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.meeting.chat.model.ChatViewModel
import mega.privacy.android.app.presentation.meeting.chat.view.ChatView
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import javax.inject.Inject

@AndroidEntryPoint
internal class ChatFragment : Fragment() {

    @Inject
    lateinit var getThemeMode: GetThemeMode

    private val viewModel by viewModels<ChatViewModel>()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            val mode by getThemeMode().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            val uiState by viewModel.state.collectAsStateWithLifecycle()

            AndroidTheme(isDark = mode.isDarkMode()) {
                ChatView(
                    uiState = uiState,
                    onBackPressed = { activity?.finish() },
                    onMenuActionPressed = viewModel::handleActionPress,
                )
            }
        }
    }
}