package mega.privacy.android.app.presentation.login.createaccount

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.fragment.compose.content
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.android.core.ui.theme.AndroidTheme
import mega.privacy.android.app.extensions.launchUrl
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.login.LoginActivity
import mega.privacy.android.app.presentation.login.createaccount.view.CreateAccountRoute
import mega.privacy.android.app.presentation.login.createaccount.view.NewCreateAccountRoute
import mega.privacy.android.app.presentation.login.model.LoginFragmentType
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.TERMS_OF_SERVICE_URL
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.login.EphemeralCredentials
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
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

    private val viewModel: CreateAccountViewModel by viewModels()

    /**
     * Called to have the fragment instantiate its user interface view.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        val themeMode by getThemeMode().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        if (uiState.isNewRegistrationUiEnabled == true) {
            AndroidTheme(isDark = themeMode.isDarkMode()) {
                NewCreateAccountRoute(
                    uiState = uiState,
                    onNavigateToLogin = ::navigateToLogin,
                    openLink = context::launchUrl,
                    setTemporalDataForAccountCreation = ::setTemporalDataForAccountCreation,
                    modifier = Modifier.fillMaxSize(),
                    viewModel = viewModel
                )
            }
        } else if (uiState.isNewRegistrationUiEnabled == false) {
            OriginalTheme(isDark = themeMode.isDarkMode()) {
                CreateAccountRoute(
                    uiState = uiState,
                    onNavigateToLogin = ::navigateToLogin,
                    openTermsAndServiceLink = { context.launchUrl(TERMS_OF_SERVICE_URL) },
                    openEndToEndEncryptionLink = { context.launchUrl(Constants.URL_E2EE) },
                    setTemporalDataForAccountCreation = ::setTemporalDataForAccountCreation,
                    modifier = Modifier.fillMaxSize(),
                    viewModel = viewModel
                )
            }
        }
    }

    private fun setTemporalDataForAccountCreation(credentials: EphemeralCredentials) {
        (requireActivity() as LoginActivity).setTemporalDataForAccountCreation(
            credentials.email ?: return,
        )
    }

    private fun navigateToLogin() {
        (requireActivity() as LoginActivity).showFragment(LoginFragmentType.Login)
    }
}
