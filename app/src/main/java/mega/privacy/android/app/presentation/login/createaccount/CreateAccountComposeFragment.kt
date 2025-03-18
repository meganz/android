package mega.privacy.android.app.presentation.login.createaccount

import android.content.Intent
import android.net.Uri
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
import mega.privacy.android.app.activities.WebViewActivity
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.login.LoginActivity
import mega.privacy.android.app.presentation.login.createaccount.view.CreateAccountRoute
import mega.privacy.android.app.presentation.login.model.LoginFragmentType
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.TERMS_OF_SERVICE_URL
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.login.EphemeralCredentials
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import timber.log.Timber
import javax.inject.Inject
import kotlin.getValue

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
            //Replace with New Design for create account screen
            OriginalTheme(isDark = themeMode.isDarkMode()) {
                CreateAccountRoute(
                    uiState = uiState,
                    onNavigateToLogin = ::navigateToLogin,
                    openTermsAndServiceLink = { openLink(TERMS_OF_SERVICE_URL) },
                    openEndToEndEncryptionLink = { openLink(Constants.URL_E2EE) },
                    setTemporalDataForAccountCreation = ::setTemporalDataForAccountCreation,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        } else if (uiState.isNewRegistrationUiEnabled == false) {
            OriginalTheme(isDark = themeMode.isDarkMode()) {
                CreateAccountRoute(
                    uiState = uiState,
                    onNavigateToLogin = ::navigateToLogin,
                    openTermsAndServiceLink = { openLink(TERMS_OF_SERVICE_URL) },
                    openEndToEndEncryptionLink = { openLink(Constants.URL_E2EE) },
                    setTemporalDataForAccountCreation = ::setTemporalDataForAccountCreation,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    private fun setTemporalDataForAccountCreation(credentials: EphemeralCredentials) {
        (requireActivity() as LoginActivity).setTemporalDataForAccountCreation(
            credentials.email ?: return,
            credentials.firstName ?: return,
            credentials.lastName ?: return,
            credentials.password ?: return,
        )
    }

    private fun navigateToLogin() {
        (requireActivity() as LoginActivity).showFragment(LoginFragmentType.Login)
    }

    private fun openLink(url: String) {
        try {
            startActivity(Intent(requireContext(), WebViewActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                data = Uri.parse(url)
            })
        } catch (_: Exception) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW).setData(Uri.parse(url)))
            } catch (e: Exception) {
                Timber.e(e, "Exception trying to open installed browser apps")
            }
        }
    }
}
