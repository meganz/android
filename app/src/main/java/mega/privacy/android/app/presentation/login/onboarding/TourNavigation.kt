package mega.privacy.android.app.presentation.login.onboarding

import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.navOptions
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import mega.privacy.android.app.globalmanagement.MegaChatRequestHandler
import mega.privacy.android.app.presentation.login.Login
import mega.privacy.android.app.presentation.login.LoginGraph
import mega.privacy.android.app.presentation.login.LoginNavigationHandler
import mega.privacy.android.app.presentation.login.LoginViewModel
import mega.privacy.android.app.presentation.login.StartRoute
import mega.privacy.android.app.presentation.login.confirmemail.ConfirmationEmailScreen
import mega.privacy.android.app.presentation.login.createaccount.CreateAccountRoute
import mega.privacy.android.app.presentation.login.onboarding.view.NewTourRoute

@Serializable
data object TourScreen : NavKey

internal fun NavGraphBuilder.tourScreen(
    navController: NavController,
    chatRequestHandler: MegaChatRequestHandler,
    onFinish: () -> Unit,
    stopShowingSplashScreen: () -> Unit,
    activityViewModel: LoginViewModel? = null,
    onBackPressed: () -> Unit,
) {
    composable<TourScreen> { backStackEntry ->
        val sharedViewModel = activityViewModel ?: run {
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry<LoginGraph>()
            }
            hiltViewModel<LoginViewModel>(parentEntry)
        }
        LoginNavigationHandler(
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
            NewTourRoute(
                activityViewModel = sharedViewModel,
                onBackPressed = onBackPressed,
            )
        }
    }
}

internal fun EntryProviderScope<NavKey>.tourScreen(
    sharedViewModel: LoginViewModel,
    onBackPressed: () -> Unit,
) {
    entry<TourScreen> { key ->
        NewTourRoute(
            activityViewModel = sharedViewModel,
            onBackPressed = onBackPressed,
        )
    }
}

internal fun NavController.openTourScreen(navOptions: NavOptions? = null) {
    navigate(TourScreen, navOptions)
}