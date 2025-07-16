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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.collectLatest
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.privacy.android.app.R
import mega.privacy.android.app.globalmanagement.MegaChatRequestHandler
import mega.privacy.android.app.presentation.billing.BillingViewModel
import mega.privacy.android.app.presentation.login.confirmemail.confirmationEmailScreen
import mega.privacy.android.app.presentation.login.confirmemail.openConfirmationEmailScreen
import mega.privacy.android.app.presentation.login.createaccount.createAccountScreen
import mega.privacy.android.app.presentation.login.createaccount.openCreateAccountScreen
import mega.privacy.android.app.presentation.login.model.LoginFragmentType
import mega.privacy.android.app.presentation.login.onboarding.openTourScreen
import mega.privacy.android.app.presentation.login.onboarding.tourScreen
import mega.privacy.android.domain.exception.LoginLoggedOutFromOtherLocation

/**
 * Login Graph Composable.
 */
@Composable
fun LoginGraph(
    chatRequestHandler: MegaChatRequestHandler,
    viewModel: LoginViewModel = hiltViewModel(),
    billingViewModel: BillingViewModel = hiltViewModel(),
    onFinish: () -> Unit,
    stopShowingSplashScreen: () -> Unit
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val navController = rememberNavController()
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

    if (showLoggedOutDialog) {
        BasicDialog(
            modifier = Modifier.testTag(LOGGED_OUT_DIALOG),
            title = stringResource(id = R.string.title_alert_logged_out),
            description = stringResource(id = R.string.error_server_expired_session),
            positiveButtonText = stringResource(id = R.string.general_ok),
            onPositiveButtonClicked = {
                showLoggedOutDialog = false
            },
            onDismiss = {
                showLoggedOutDialog = false
            }
        )
    }

    LaunchedEffect(uiState.isPendingToShowFragment) {
        val fragmentType = uiState.isPendingToShowFragment
        if (fragmentType != null) {
            if (fragmentType != LoginFragmentType.Login) {
                stopShowingSplashScreen()
            }

            when (fragmentType) {
                LoginFragmentType.Login -> navController.openLoginScreen()
                LoginFragmentType.CreateAccount -> navController.openCreateAccountScreen()

                LoginFragmentType.Tour -> {
                    focusManager.clearFocus()
                    navController.openTourScreen(
                        NavOptions.Builder()
                            .setPopUpTo(route = StartRoute, inclusive = false)
                            .build()
                    )
                }

                LoginFragmentType.ConfirmEmail -> navController.openConfirmationEmailScreen()
            }
            viewModel.isPendingToShowFragmentConsumed()
        }
    }

    NavHost(
        navController = navController,
        startDestination = StartRoute
    ) {
        composable(StartRoute) {
            // no-op, we checking start destination in the view model
        }
        loginScreen(
            sharedViewModel = viewModel,
            billingViewModel = billingViewModel,
        )
        createAccountScreen(
            sharedViewModel = viewModel,
        )
        tourScreen(
            sharedViewModel = viewModel,
            onBackPressed = onFinish
        )
        confirmationEmailScreen(
            sharedViewModel = viewModel,
            onBackPressed = onFinish
        )
    }
}

private const val StartRoute = "start"
internal const val LOGGED_OUT_DIALOG = "logged_out_dialog"