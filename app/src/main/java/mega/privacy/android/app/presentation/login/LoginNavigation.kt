package mega.privacy.android.app.presentation.login

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
import mega.privacy.android.app.presentation.login.confirmemail.ConfirmationEmailNavKey
import mega.privacy.android.app.presentation.login.createaccount.CreateAccountNavKey
import mega.privacy.android.app.presentation.login.onboarding.TourNavKey
import mega.privacy.android.feature.payment.presentation.billing.BillingViewModel
import mega.privacy.android.navigation.contract.navkey.NoSessionNavKey

@Serializable
data object LoginNavKey : NoSessionNavKey.Mandatory

internal fun NavGraphBuilder.loginScreen(
    navController: NavController,
    onFinish: () -> Unit,
    activityViewModel: LoginViewModel? = null,
    stopShowingSplashScreen: () -> Unit,
) {
    composable<LoginNavKey> { backStackEntry ->
        val parentEntry = remember(backStackEntry) {
            navController.getBackStackEntry<LoginGraph>()
        }
        val sharedViewModel = activityViewModel ?: hiltViewModel<LoginViewModel>(parentEntry)
        val billingViewModel = hiltViewModel<BillingViewModel>(parentEntry)
        LoginNavigationHandler(
            navigateToLoginScreen = { navController.navigate(LoginNavKey) },
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
            LoginScreen(
                viewModel = sharedViewModel,
                billingViewModel = billingViewModel
            )
        }
    }
}

internal fun EntryProviderScope<NavKey>.loginScreen(
    sharedViewModel: LoginViewModel,
) {
    entry<LoginNavKey> { key ->
        val billingViewModel = hiltViewModel<BillingViewModel>()
        LoginScreen(
            viewModel = sharedViewModel,
            billingViewModel = billingViewModel
        )
    }
}

internal fun EntryProviderScope<NavKey>.loginStartScreen(
) {
    entry<StartRoute> { key ->
    }
}

internal fun NavController.openLoginScreen(
    options: NavOptions? = navOptions {
        popUpTo(0) {
            inclusive = true
        }
    },
) {
    navigate(LoginNavKey, options)
}