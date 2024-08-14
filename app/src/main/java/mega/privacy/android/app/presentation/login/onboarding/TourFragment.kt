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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.login.onboarding.view.TourRoute
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import timber.log.Timber
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
            OriginalTempTheme(isDark = themeMode.isDarkMode()) {
                TourRoute(
                    modifier = Modifier.fillMaxSize(),
                    onLoginClick = {
                        Timber.d("onLoginClick")
                        onLoginClick?.invoke()
                    },
                    onCreateAccountClick = {
                        Timber.d("onRegisterClick")
                        onCreateAccountClick?.invoke()
                    },
                    onOpenLink = {
                        onOpenLink?.invoke(it)
                    }
                )
            }
        }
    }
}
