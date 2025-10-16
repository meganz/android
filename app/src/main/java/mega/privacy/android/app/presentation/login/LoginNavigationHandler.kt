package mega.privacy.android.app.presentation.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.flow.collectLatest
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.privacy.android.app.R
import mega.privacy.android.app.globalmanagement.MegaChatRequestHandler
import mega.privacy.android.app.presentation.login.model.LoginScreen
import mega.privacy.android.domain.exception.LoginLoggedOutFromOtherLocation
import mega.privacy.android.shared.resources.R as sharedResR

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
    chatRequestHandler: MegaChatRequestHandler,
    onFinish: () -> Unit,
    stopShowingSplashScreen: () -> Unit,
    viewModel: LoginViewModel,
    content: @Composable () -> Unit = {},
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    var showLoggedOutDialog by rememberSaveable { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(Unit) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_CREATE) {
                chatRequestHandler.setIsLoggingRunning(true)
            } else if (event == Lifecycle.Event.ON_DESTROY) {
                chatRequestHandler.setIsLoggingRunning(false)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.monitorLoggedOutFromAnotherLocation.collectLatest { loggedOut ->
            if (loggedOut) {
                showLoggedOutDialog = true
                viewModel.setHandledLoggedOutFromAnotherLocation()
            }
        }
    }

    LaunchedEffect(uiState.isPendingToFinishActivity) {
        if (uiState.isPendingToFinishActivity) {
            onFinish()
        }
    }

    LaunchedEffect(uiState.loginException) {
        if (uiState.loginException is LoginLoggedOutFromOtherLocation) {
            showLoggedOutDialog = true
            viewModel.setLoginErrorConsumed()
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
    if (showLoggedOutDialog) {
        BasicDialog(
            modifier = Modifier.testTag(LOGGED_OUT_DIALOG),
            title = stringResource(id = R.string.title_alert_logged_out),
            description = stringResource(id = R.string.error_server_expired_session),
            positiveButtonText = stringResource(id = sharedResR.string.general_ok),
            onPositiveButtonClicked = {
                showLoggedOutDialog = false
            },
            onDismiss = {
                showLoggedOutDialog = false
            }
        )
    }
}

internal const val LOGGED_OUT_DIALOG = "logged_out_dialog" 