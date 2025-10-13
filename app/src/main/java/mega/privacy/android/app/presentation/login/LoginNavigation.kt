package mega.privacy.android.app.presentation.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalFocusManager
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.navOptions
import androidx.navigation3.runtime.EntryProviderBuilder
import androidx.navigation3.runtime.NavKey
import de.palm.composestateevents.EventEffect
import kotlinx.serialization.Serializable
import mega.privacy.android.app.globalmanagement.MegaChatRequestHandler
import mega.privacy.android.app.presentation.login.confirmemail.ConfirmationEmailScreen
import mega.privacy.android.app.presentation.login.createaccount.CreateAccountRoute
import mega.privacy.android.app.presentation.login.model.LoginScreen
import mega.privacy.android.app.presentation.login.onboarding.TourScreen
import mega.privacy.android.feature.payment.presentation.billing.BillingViewModel
import mega.privacy.android.navigation.contract.NavigationHandler

@Serializable
data object Login : NavKey

internal fun NavGraphBuilder.loginScreen(
    navController: NavController,
    chatRequestHandler: MegaChatRequestHandler,
    onFinish: () -> Unit,
    activityViewModel: LoginViewModel? = null,
    stopShowingSplashScreen: () -> Unit,
) {
    composable<Login> { backStackEntry ->
        val parentEntry = remember(backStackEntry) {
            navController.getBackStackEntry<LoginGraph>()
        }
        val sharedViewModel = activityViewModel ?: hiltViewModel<LoginViewModel>(parentEntry)
        val billingViewModel = hiltViewModel<BillingViewModel>(parentEntry)
        LoginGraphContent(
            navigateToLoginScreen = { navController.navigate(Login) },
            navigateToCreateAccountScreen = { navController.navigate(CreateAccountRoute) },
            navigateToTourScreen = {
                navController.navigate(TourScreen, navOptions {
                    popUpTo<StartRoute> {
                        inclusive = false
                    }
                })
            },
            navigateToConfirmationEmailScreen = { navController.navigate(ConfirmationEmailScreen) },
            viewModel = sharedViewModel,
            chatRequestHandler = chatRequestHandler,
            onFinish = onFinish,
            stopShowingSplashScreen = stopShowingSplashScreen,
        ) {
            LoginScreen(
                viewModel = sharedViewModel,
                billingViewModel = billingViewModel
            )
        }
    }
}

internal fun EntryProviderBuilder<NavKey>.loginScreen(
    navigationHandler: NavigationHandler,
    chatRequestHandler: MegaChatRequestHandler,
    onFinish: () -> Unit,
    sharedViewModel: LoginViewModel,
    billingViewModel: BillingViewModel,
    stopShowingSplashScreen: () -> Unit,
) {
    entry<Login> { key ->
        LoginGraphContent(
            navigateToLoginScreen = { navigationHandler.navigate(Login) },
            navigateToCreateAccountScreen = { navigationHandler.navigate(CreateAccountRoute) },
            navigateToTourScreen = {
                navigationHandler.navigateAndClearBackStack(TourScreen)
            },
            navigateToConfirmationEmailScreen = { navigationHandler.navigate(ConfirmationEmailScreen) },
            viewModel = sharedViewModel,
            chatRequestHandler = chatRequestHandler,
            onFinish = onFinish,
            stopShowingSplashScreen = stopShowingSplashScreen,
        ) {
            LoginScreen(
                viewModel = sharedViewModel,
                billingViewModel = billingViewModel
            )
        }
    }
}

internal fun EntryProviderBuilder<NavKey>.loginStartScreen(
    sharedViewModel: LoginViewModel,
    navigationHandler: NavigationHandler,
    stopShowingSplashScreen: () -> Unit,
) {
    entry<StartRoute> { key ->
        val uiState by sharedViewModel.state.collectAsStateWithLifecycle()
        val focusManager = LocalFocusManager.current
        // start composable to handle the initial state and navigation logic

        EventEffect(
            uiState.isPendingToShowFragment,
            sharedViewModel::isPendingToShowFragmentConsumed
        ) {
            if (it != LoginScreen.LoginScreen) {
                stopShowingSplashScreen()
            }
            if (it == LoginScreen.Tour) focusManager.clearFocus()

            navigationHandler.navigate(it.navKey)
        }
    }
}

internal fun NavController.openLoginScreen(
    options: NavOptions? = navOptions {
        popUpTo(0) {
            inclusive = true
        }
    },
) {
    navigate(Login, options)
}