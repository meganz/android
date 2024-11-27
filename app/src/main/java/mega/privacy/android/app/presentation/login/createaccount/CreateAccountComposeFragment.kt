package mega.privacy.android.app.presentation.login.createaccount

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.Fragment
import androidx.fragment.compose.content
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.login.createaccount.view.CreateAccountRoute
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import javax.inject.Inject

/**
 * Create Account Compose Fragment.
 */
@AndroidEntryPoint
class CreateAccountComposeFragment : Fragment() {

    /**
     * Current theme
     */
    @Inject
    lateinit var getThemeMode: GetThemeMode

    /**
     * Called to have the fragment instantiate its user interface view.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        val themeMode by getThemeMode().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
        OriginalTempTheme(isDark = themeMode.isDarkMode()) {
            CreateAccountRoute(
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
