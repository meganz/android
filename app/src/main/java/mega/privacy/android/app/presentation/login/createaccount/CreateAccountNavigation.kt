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
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import mega.privacy.android.app.presentation.login.LoginGraph
import mega.privacy.android.app.presentation.login.LoginNavKey
import mega.privacy.android.app.presentation.login.LoginNavigationHandler
import mega.privacy.android.app.presentation.login.LoginViewModel
import mega.privacy.android.app.presentation.login.StartRoute
import mega.privacy.android.app.presentation.login.confirmemail.ConfirmationEmailNavKey
import mega.privacy.android.app.presentation.login.createaccount.view.NewCreateAccountRoute
import mega.privacy.android.app.presentation.login.onboarding.TourNavKey
import mega.privacy.android.navigation.contract.navkey.NoSessionNavKey

/**
 * Create Account Screen
 * @param initialEmail if set, the email field will be pre-filled with this value
 */
@Serializable
data class CreateAccountNavKey(
    val initialEmail: String? = null,
) : NoSessionNavKey.Mandatory

internal fun NavGraphBuilder.createAccountScreen(
    navController: NavController,
    onFinish: () -> Unit,
    activityViewModel: LoginViewModel? = null,
    stopShowingSplashScreen: () -> Unit,
) {
    composable<CreateAccountNavKey> { backStackEntry ->
        val sharedViewModel = activityViewModel ?: run {
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry<LoginGraph>()
            }
            hiltViewModel<LoginViewModel>(parentEntry)
        }
        LoginNavigationHandler(
            navigateToLoginScreen = { navController.navigate(LoginNavKey) },
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
            NewCreateAccountRoute(
                activityViewModel = sharedViewModel,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

internal fun EntryProviderScope<NavKey>.createAccountScreen(
    sharedViewModel: LoginViewModel,
) {
    entry<CreateAccountNavKey> { key ->
        NewCreateAccountRoute(
            activityViewModel = sharedViewModel,
            initialEmail = key.initialEmail,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

internal fun NavController.openCreateAccountScreen(
    initialEmail: String? = null,
    navOptions: NavOptions? = null,
) {
    navigate(CreateAccountNavKey(initialEmail = initialEmail), navOptions)
}