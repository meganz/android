package mega.privacy.android.app.presentation.login.confirmemail.changeemail

import androidx.compose.runtime.remember
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
import mega.privacy.android.app.presentation.login.createaccount.CreateAccountRoute
import mega.privacy.android.app.presentation.login.onboarding.TourScreen
import mega.privacy.android.navigation.contract.NavigationHandler

/**
 * function to build the ChangeEmailAddress screen.
 */
fun NavGraphBuilder.changeEmailAddress(
    navController: NavController,
    activityViewModel: LoginViewModel?,
    chatRequestHandler: MegaChatRequestHandler,
    onFinish: () -> Unit,
    stopShowingSplashScreen: () -> Unit,
    onChangeEmailSuccess: (String) -> Unit,
) {
    composable<ChangeEmailAddressScreen> { backStackEntry ->
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
            ChangeEmailAddressRoute(
                onChangeEmailSuccess = onChangeEmailSuccess
            )
        }
    }
}

@Serializable
data class ChangeEmailAddressScreen(
    val email: String?,
    val fullName: String?,
) : NavKey


internal fun EntryProviderBuilder<NavKey>.changeEmailAddress(
    navigationHandler: NavigationHandler,
    chatRequestHandler: MegaChatRequestHandler,
    onFinish: () -> Unit,
    sharedViewModel: LoginViewModel,
    stopShowingSplashScreen: () -> Unit,
    onChangeEmailSuccess: (String) -> Unit,
) {
    entry<ChangeEmailAddressScreen> { key ->
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
            ChangeEmailAddressRoute(
                onChangeEmailSuccess = { newEmail ->
                    onChangeEmailSuccess(newEmail)
                    navigationHandler.back()
                }
            )
        }
    }
}

/**
 * Navigation for [ChangeEmailAddressRoute]
 */
fun NavController.navigateToChangeEmailAddress(
    email: String?,
    fullName: String?,
    navOptions: NavOptions? = null,
) {
    this.navigate(
        ChangeEmailAddressScreen(
            email = email,
            fullName = fullName,
        ),
        navOptions = navOptions
    )
}