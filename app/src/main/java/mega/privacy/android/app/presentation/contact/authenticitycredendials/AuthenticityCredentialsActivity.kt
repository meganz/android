package mega.privacy.android.app.presentation.contact.authenticitycredendials

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.presentation.extensions.changeStatusBarColor
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.security.PasscodeCheck
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.core.ui.theme.AndroidTheme
import javax.inject.Inject

/**
 * Authenticity Credentials Activity.
 *
 * @property passCodeFacade [PasscodeCheck]
 * @property getThemeMode   [GetThemeMode]
 */
@AndroidEntryPoint
class AuthenticityCredentialsActivity : ComponentActivity() {

    @Inject
    lateinit var passCodeFacade: PasscodeCheck

    @Inject
    lateinit var getThemeMode: GetThemeMode

    private val viewModel by viewModels<AuthenticityCredentialsViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        intent.extras?.getString(Constants.EMAIL)?.let {
            viewModel.requestData(it)
        } ?: finish()

        setContent { AuthenticityCredentialsView() }
    }

    @Composable
    private fun AuthenticityCredentialsView() {
        val themeMode by getThemeMode().collectAsState(initial = ThemeMode.System)
        val isDark = themeMode.isDarkMode()
        val uiState by viewModel.state.collectAsState()

        AndroidTheme(isDark = themeMode.isDarkMode()) {
            mega.privacy.android.app.presentation.contact.authenticitycredendials.view.AuthenticityCredentialsView(
                state = uiState,
                onButtonClicked = viewModel::actionClicked,
                onBackPressed = { finish() },
                onScrollChange = { scrolled -> onScrollChange(scrolled, isDark) },
                onErrorShown = viewModel::errorShown)
        }
    }

    private fun onScrollChange(scrolled: Boolean, isDark: Boolean) =
        changeStatusBarColor(scrolled = scrolled, isDark = isDark)
}