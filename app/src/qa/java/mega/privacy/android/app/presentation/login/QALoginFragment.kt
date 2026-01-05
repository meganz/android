package mega.privacy.android.app.presentation.login

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.sharedcomponents.extension.isDarkMode
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.navigation.destination.HomeScreensNavKey
import timber.log.Timber
import javax.inject.Inject

/**
 * QA Login Fragment for multi-account testing.
 * This fragment displays a simple login screen for testing purposes.
 */
@AndroidEntryPoint
class QALoginFragment : DialogFragment() {

    @Inject
    lateinit var monitorThemeModeUseCase: MonitorThemeModeUseCase

    @Inject
    lateinit var megaNavigator: MegaNavigator

    private val qaLoginViewModel by viewModels<QALoginViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Material_Light_NoActionBar)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val themeMode by monitorThemeModeUseCase().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
                AndroidTheme(isDark = themeMode.isDarkMode()) {
                    val uiState by qaLoginViewModel.state.collectAsStateWithLifecycle()

                    // Handle login events - navigate to main activity
                    LaunchedEffect(Unit) {
                        qaLoginViewModel.events.collect { event ->
                            when (event) {
                                is LoginEvent.NavigateToHome -> {
                                    Timber.d("QA Login successful, navigating to ManagerActivity")
                                    navigateToMainActivity()
                                }
                            }
                        }
                    }

                    QALoginView(
                        state = uiState,
                        onEmailChanged = qaLoginViewModel::onEmailChanged,
                        onPasswordChanged = qaLoginViewModel::onPasswordChanged,
                        onLoginClicked = qaLoginViewModel::onLoginClicked,
                        onBackPressed = {
                            dismiss()
                        },
                    )
                }
            }
        }
    }

    private fun navigateToMainActivity() {
        runCatching {
            context?.let {
                megaNavigator.openManagerActivity(
                    context = it,
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK,
                    singleActivityDestination = HomeScreensNavKey(),
                )

                activity?.finish()
                dismiss()
            }
        }.onFailure {
            Timber.e(it, "Failed to navigate to ManagerActivity")
        }
    }
}