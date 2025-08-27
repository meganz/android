package mega.privacy.android.app.presentation.login

import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.navOptions
import kotlinx.serialization.Serializable
import mega.privacy.android.app.globalmanagement.MegaChatRequestHandler
import mega.privacy.android.feature.payment.presentation.billing.BillingViewModel
import mega.privacy.android.app.presentation.login.confirmemail.ConfirmationEmailScreen
import mega.privacy.android.app.presentation.login.createaccount.CreateAccountRoute
import mega.privacy.android.app.presentation.login.onboarding.TourScreen

@Serializable
data object LoginScreen

internal fun NavGraphBuilder.loginScreen(
    navController: NavController,
    chatRequestHandler: MegaChatRequestHandler,
    onFinish: () -> Unit,
    activityViewModel: LoginViewModel? = null,
    stopShowingSplashScreen: () -> Unit,
) {
    composable<LoginScreen> { backStackEntry ->
        val parentEntry = remember(backStackEntry) {
            navController.getBackStackEntry<LoginGraph>()
        }
        val sharedViewModel = activityViewModel ?: hiltViewModel<LoginViewModel>(parentEntry)
        val billingViewModel = hiltViewModel<BillingViewModel>(parentEntry)
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
            LoginScreen(
                viewModel = sharedViewModel,
                billingViewModel = billingViewModel
            )
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
    navigate(LoginScreen, options)
}