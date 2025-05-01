package mega.privacy.android.app.presentation.login.confirmemail

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mega.android.core.ui.theme.AndroidTheme
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.login.LoginActivity
import mega.privacy.android.app.presentation.login.confirmemail.view.ConfirmEmailRoute
import mega.privacy.android.app.presentation.login.model.LoginFragmentType
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
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
    lateinit var getThemeMode: GetThemeMode

    private val viewModel: ConfirmEmailViewModel by viewModels()

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

            LaunchedEffect(uiState.isCreatingAccountCancelled) {
                if (uiState.isCreatingAccountCancelled) {
                    onCancelConfirmationAccount()
                    viewModel.onHandleCancelCreateAccount()
                }
            }

            BackHandler {
                activity?.finish()
            }

            if (uiState.isNewRegistrationUiEnabled == true) {
                AndroidTheme(isDark = themeMode.isDarkMode()) {
                    NewConfirmEmailGraph(
                        fullName = uiState.firstName.orEmpty(),
                        viewModel = viewModel,
                        uiState = uiState,
                        onShowPendingFragment = ::onShowPendingFragment,
                        onSetTemporalEmail = ::onSetTemporalEmail,
                        sendFeedbackEmail = ::sendFeedbackEmail
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
        (activity as? LoginActivity)?.showFragment(fragmentType)
    }

    private fun onSetTemporalEmail(email: String) {
        (activity as? LoginActivity)?.setTemporalEmail(email)
    }

    private fun onCancelConfirmationAccount() {
        (activity as? LoginActivity)?.cancelConfirmationAccount()
    }

    private fun sendFeedbackEmail(email: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val emailIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
                putExtra(Intent.EXTRA_SUBJECT, "Mega Feedback")
                putExtra(Intent.EXTRA_TEXT, viewModel.generateSupportEmailBody())
            }
            val intent = Intent.createChooser(emailIntent, " ")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
    }
}
