package mega.privacy.android.app.presentation.login.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.android.core.ui.theme.AndroidTheme
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.login.onboarding.view.NewTourRoute
import mega.privacy.android.app.presentation.login.onboarding.view.TourRoute
import mega.privacy.android.app.presentation.login.onboarding.view.TourViewModel
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import javax.inject.Inject

/**
 * Tour/Onboarding Fragment.
 */
@AndroidEntryPoint
class TourFragment : Fragment() {

    /**
     * Current theme
     */
    @Inject
    lateinit var getThemeMode: GetThemeMode

    internal var onLoginClick: (() -> Unit)? = null
    internal var onCreateAccountClick: (() -> Unit)? = null
    internal var onOpenLink: ((meetingLink: String) -> Unit)? = null

    private val viewModel: TourViewModel by viewModels()

    /**
     * Called to have the fragment instantiate its user interface view.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            val themeMode by getThemeMode().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            if (uiState.isNewRegistrationUiEnabled == true) {
                AndroidTheme(isDark = themeMode.isDarkMode()) {
                    NewTourRoute(
                        modifier = Modifier.fillMaxSize(),
                        onLoginClick = {
                            onLoginClick?.invoke()
                        },
                        onCreateAccountClick = {
                            onCreateAccountClick?.invoke()
                        }
                    )
                }
            } else if (uiState.isNewRegistrationUiEnabled == false) {
                OriginalTheme(isDark = themeMode.isDarkMode()) {
                    TourRoute(
                        uiState = uiState,
                        modifier = Modifier.fillMaxSize(),
                        onLoginClick = {
                            onLoginClick?.invoke()
                        },
                        onCreateAccountClick = {
                            onCreateAccountClick?.invoke()
                        },
                        onOpenLink = {
                            onOpenLink?.invoke(it)
                            viewModel.resetOpenLink()
                        },
                        onMeetingLinkChange = viewModel::onMeetingLinkChange,
                        onConfirmMeetingLinkClick = viewModel::onConfirmMeetingLinkClick,
                        onClearLogoutProgressFlag = viewModel::clearLogoutProgressFlag
                    )
                }
            }
        }
    }
}
