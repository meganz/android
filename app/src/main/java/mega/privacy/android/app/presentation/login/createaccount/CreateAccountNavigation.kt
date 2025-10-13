package mega.privacy.android.app.presentation.login.createaccount

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.navOptions
import androidx.navigation3.runtime.EntryProviderBuilder
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import mega.privacy.android.app.globalmanagement.MegaChatRequestHandler
import mega.privacy.android.app.presentation.login.Login
import mega.privacy.android.app.presentation.login.LoginGraph
import mega.privacy.android.app.presentation.login.LoginGraphContent
import mega.privacy.android.app.presentation.login.LoginViewModel
import mega.privacy.android.app.presentation.login.StartRoute
import mega.privacy.android.app.presentation.login.confirmemail.ConfirmationEmailScreen
import mega.privacy.android.app.presentation.login.createaccount.view.NewCreateAccountRoute
import mega.privacy.android.app.presentation.login.onboarding.TourScreen
import mega.privacy.android.navigation.contract.NavigationHandler

@Serializable
data object CreateAccountRoute: NavKey

internal fun NavGraphBuilder.createAccountScreen(
    navController: NavController,
    chatRequestHandler: MegaChatRequestHandler,
    onFinish: () -> Unit,
    activityViewModel: LoginViewModel? = null,
    stopShowingSplashScreen: () -> Unit,
) {
    composable<CreateAccountRoute> { backStackEntry ->
        val sharedViewModel = activityViewModel ?: run {
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry<LoginGraph>()
            }
            hiltViewModel<LoginViewModel>(parentEntry)
        }
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
            NewCreateAccountRoute(
                activityViewModel = sharedViewModel,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

internal fun EntryProviderBuilder<NavKey>.createAccountScreen(
    navigationHandler: NavigationHandler,
    chatRequestHandler: MegaChatRequestHandler,
    onFinish: () -> Unit,
    sharedViewModel: LoginViewModel,
    stopShowingSplashScreen: () -> Unit,
) {
    entry<CreateAccountRoute> { key ->
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
            NewCreateAccountRoute(
                activityViewModel = sharedViewModel,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

internal fun NavController.openCreateAccountScreen(navOptions: NavOptions? = null) {
    navigate(CreateAccountRoute, navOptions)
}