package mega.privacy.android.app.presentation.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalFocusManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.palm.composestateevents.EventEffect
import mega.privacy.android.app.presentation.login.model.LoginScreen

/**
 * Login Graph Content Composable that handles state management and navigation logic
 * without creating its own NavHost. This can be used within other navigation graphs.
 */
@Composable
fun LoginNavigationHandler(
    navigateToLoginScreen: () -> Unit,
    navigateToCreateAccountScreen: () -> Unit,
    navigateToTourScreen: () -> Unit,
    navigateToConfirmationEmailScreen: () -> Unit,
    onFinish: () -> Unit,
    stopShowingSplashScreen: () -> Unit,
    viewModel: LoginViewModel,
    content: @Composable () -> Unit = {},
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(uiState.isPendingToFinishActivity) {
        if (uiState.isPendingToFinishActivity) {
            onFinish()
        }
    }

    EventEffect(uiState.isPendingToShowFragment, viewModel::isPendingToShowFragmentConsumed) {
        if (it != LoginScreen.LoginScreen) {
            stopShowingSplashScreen()
        }

        when (it) {
            LoginScreen.LoginScreen -> navigateToLoginScreen()
            LoginScreen.CreateAccount -> navigateToCreateAccountScreen()

            LoginScreen.Tour -> {
                focusManager.clearFocus()
                navigateToTourScreen()
            }

            LoginScreen.ConfirmEmail -> navigateToConfirmationEmailScreen()
        }
    }

    content()

}

internal const val LOGGED_OUT_DIALOG = "logged_out_dialog" 