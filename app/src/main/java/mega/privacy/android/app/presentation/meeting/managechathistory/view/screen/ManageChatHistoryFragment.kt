package mega.privacy.android.app.presentation.meeting.managechathistory.view.screen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.components.chatsession.ChatSessionContainer
import mega.privacy.android.app.components.session.SessionContainer
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.passcode.model.PasscodeCryptObjectFactory
import mega.privacy.android.app.presentation.psa.PsaContainer
import mega.privacy.android.app.presentation.security.check.PasscodeContainer
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import javax.inject.Inject

/**
 * Fragment for displaying the [ManageChatHistoryRoute]
 */
@AndroidEntryPoint
class ManageChatHistoryFragment : Fragment() {

    /**
     * Current theme
     */
    @Inject
    lateinit var monitorThemeModeUseCase: MonitorThemeModeUseCase

    /**
     * Passcode crypt object factory
     */
    @Inject
    lateinit var passcodeCryptObjectFactory: PasscodeCryptObjectFactory

    /**
     * Called to have this fragment instantiate its user interface view.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            val themeMode by monitorThemeModeUseCase().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            SessionContainer {
                ChatSessionContainer {
                    OriginalTheme(isDark = themeMode.isDarkMode()) {
                        PasscodeContainer(
                            passcodeCryptObjectFactory = passcodeCryptObjectFactory,
                            content = {
                                PsaContainer {
                                    ManageChatHistoryRoute(
                                        modifier = Modifier.fillMaxSize(),
                                        onNavigateUp = { activity?.finish() }
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
