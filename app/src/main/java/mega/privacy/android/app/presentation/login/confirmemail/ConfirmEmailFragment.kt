package mega.privacy.android.app.presentation.login.confirmemail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.android.core.ui.theme.AndroidTheme
import mega.privacy.android.app.extensions.launchUrl
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.login.LoginViewModel
import mega.privacy.android.app.presentation.login.confirmemail.view.ConfirmEmailRoute
import mega.privacy.android.app.presentation.login.model.LoginFragmentType
import mega.privacy.android.app.utils.Constants.HELP_CENTRE_HOME_URL
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import timber.log.Timber
import javax.inject.Inject

/**
 * Confirm email fragment.
 */
@AndroidEntryPoint
class ConfirmEmailFragment : Fragment() {

    /**
     * Current theme
     */
    @Inject
    lateinit var monitorThemeModeUseCase: MonitorThemeModeUseCase

    private val viewModel: ConfirmEmailViewModel by viewModels()
    private val activityViewModel: LoginViewModel by activityViewModels()

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
            val themeMode by monitorThemeModeUseCase().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(uiState.isCreatingAccountCancelled) {
                if (uiState.isCreatingAccountCancelled) {
                    onCancelConfirmationAccount()
                    viewModel.onHandleCancelCreateAccount()
                }
            }

            LaunchedEffect(uiState.isAccountConfirmed) {
                if (uiState.isAccountConfirmed) {
                    onShowPendingFragment(LoginFragmentType.Login)
                    activityViewModel.checkTemporalCredentials()
                }
            }

            if (uiState.isNewRegistrationUiEnabled == true) {
                AndroidTheme(isDark = themeMode.isDarkMode()) {
                    NewConfirmEmailGraph(
                        fullName = uiState.firstName.orEmpty(),
                        viewModel = viewModel,
                        uiState = uiState,
                        onShowPendingFragment = ::onShowPendingFragment,
                        onSetTemporalEmail = ::onSetTemporalEmail,
                        onNavigateToHelpCentre = { context.launchUrl(HELP_CENTRE_HOME_URL) }
                    )
                }
            } else if (uiState.isNewRegistrationUiEnabled == false) {
                OriginalTheme(isDark = themeMode.isDarkMode()) {
                    ConfirmEmailRoute(
                        modifier = Modifier
                            .systemBarsPadding()
                            .fillMaxSize(),
                        email = uiState.registeredEmail.orEmpty(),
                        fullName = uiState.firstName.orEmpty(),
                        onShowPendingFragment = ::onShowPendingFragment,
                        onSetTemporalEmail = ::onSetTemporalEmail,
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    private fun onShowPendingFragment(fragmentType: LoginFragmentType) {
        activityViewModel.setPendingFragmentToShow(fragmentType)
    }

    private fun onSetTemporalEmail(email: String) {
        activityViewModel.setTemporalEmail(email)
    }

    private fun onCancelConfirmationAccount() {
        Timber.d("cancelConfirmationAccount")
        activityViewModel.cancelCreateAccount()
    }
}
