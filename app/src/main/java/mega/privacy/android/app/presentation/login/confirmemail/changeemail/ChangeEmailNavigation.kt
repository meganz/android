package mega.privacy.android.app.presentation.login.confirmemail.changeemail

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
import mega.privacy.android.app.presentation.login.LoginGraph
import mega.privacy.android.app.presentation.login.LoginNavKey
import mega.privacy.android.app.presentation.login.LoginNavigationHandler
import mega.privacy.android.app.presentation.login.LoginViewModel
import mega.privacy.android.app.presentation.login.StartRoute
import mega.privacy.android.app.presentation.login.confirmemail.ConfirmationEmailNavKey
import mega.privacy.android.app.presentation.login.createaccount.CreateAccountNavKey
import mega.privacy.android.app.presentation.login.onboarding.TourNavKey
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.navkey.NoSessionNavKey

/**
 * function to build the ChangeEmailAddress screen.
 */
fun NavGraphBuilder.changeEmailAddress(
    navController: NavController,
    activityViewModel: LoginViewModel?,
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
        LoginNavigationHandler(
            navigateToLoginScreen = { navController.navigate(LoginNavKey()) },
            navigateToCreateAccountScreen = { navController.navigate(CreateAccountNavKey) },
            navigateToTourScreen = {
                navController.navigate(TourNavKey, navOptions {
                    popUpTo<StartRoute> {
                        inclusive = false
                    }
                })
            },
            navigateToConfirmationEmailScreen = { navController.navigate(ConfirmationEmailNavKey) },
            viewModel = sharedViewModel,
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
) : NoSessionNavKey.Optional


internal fun EntryProviderScope<NavKey>.changeEmailAddress(
    navigationHandler: NavigationHandler,
    onChangeEmailSuccess: (String) -> Unit,
) {
    entry<ChangeEmailAddressScreen> { key ->
        ChangeEmailAddressRoute(
            onChangeEmailSuccess = { newEmail ->
                onChangeEmailSuccess(newEmail)
                navigationHandler.back()
            }
        )
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