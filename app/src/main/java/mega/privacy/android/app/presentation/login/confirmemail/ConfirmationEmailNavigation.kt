package mega.privacy.android.app.presentation.login.confirmemail

import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.navOptions
import kotlinx.serialization.Serializable
import mega.privacy.android.app.globalmanagement.MegaChatRequestHandler
import mega.privacy.android.app.presentation.login.LoginGraph
import mega.privacy.android.app.presentation.login.LoginGraphContent
import mega.privacy.android.app.presentation.login.LoginScreen
import mega.privacy.android.app.presentation.login.LoginViewModel
import mega.privacy.android.app.presentation.login.StartRoute
import mega.privacy.android.app.presentation.login.createaccount.CreateAccountRoute
import mega.privacy.android.app.presentation.login.onboarding.TourScreen

@Serializable
data object ConfirmationEmailScreen

internal fun NavGraphBuilder.confirmationEmailScreen(
    navController: NavController,
    chatRequestHandler: MegaChatRequestHandler,
    onFinish: () -> Unit,
    stopShowingSplashScreen: () -> Unit,
    activityViewModel: LoginViewModel? = null,
    onBackPressed: () -> Unit,
) {
    composable<ConfirmationEmailScreen> { backStackEntry ->
        val sharedViewModel = activityViewModel ?: run {
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry<LoginGraph>()
            }
            hiltViewModel<LoginViewModel>(parentEntry)
        }
        LoginGraphContent(
            navigateToLoginScreen = { navController.navigate(LoginScreen) },
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
            NewConfirmEmailGraph(
                activityViewModel = sharedViewModel,
                onBackPressed = onBackPressed,
            )
        }
    }
}

internal fun NavController.openConfirmationEmailScreen(navOptions: NavOptions? = null) {
    navigate(ConfirmationEmailScreen, navOptions)
}