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
import mega.privacy.android.app.presentation.login.LoginGraph
import mega.privacy.android.app.presentation.login.LoginNavKey
import mega.privacy.android.app.presentation.login.LoginNavigationHandler
import mega.privacy.android.app.presentation.login.LoginViewModel
import mega.privacy.android.app.presentation.login.StartRoute
import mega.privacy.android.app.presentation.login.confirmemail.ConfirmationEmailNavKey
import mega.privacy.android.app.presentation.login.createaccount.CreateAccountNavKey
import mega.privacy.android.app.presentation.login.onboarding.view.NewTourRoute
import mega.privacy.android.navigation.contract.navkey.NoSessionNavKey

@Serializable
data object TourNavKey : NoSessionNavKey.Mandatory

internal fun NavGraphBuilder.tourScreen(
    navController: NavController,
    onFinish: () -> Unit,
    stopShowingSplashScreen: () -> Unit,
    activityViewModel: LoginViewModel? = null,
    onBackPressed: () -> Unit,
) {
    composable<TourNavKey> { backStackEntry ->
        val sharedViewModel = activityViewModel ?: run {
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry<LoginGraph>()
            }
            hiltViewModel<LoginViewModel>(parentEntry)
        }
        LoginNavigationHandler(
            navigateToLoginScreen = { navController.navigate(LoginNavKey()) },
            navigateToCreateAccountScreen = { navController.navigate(CreateAccountNavKey()) },
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
    entry<TourNavKey> { key ->
        NewTourRoute(
            activityViewModel = sharedViewModel,
            onBackPressed = onBackPressed,
        )
    }
}

internal fun NavController.openTourScreen(navOptions: NavOptions? = null) {
    navigate(TourNavKey, navOptions)
}