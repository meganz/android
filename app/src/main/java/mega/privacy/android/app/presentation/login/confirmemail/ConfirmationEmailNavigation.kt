package mega.privacy.android.app.presentation.login.confirmemail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.navOptions
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import mega.privacy.android.app.extensions.launchUrl
import mega.privacy.android.app.presentation.login.LoginGraph
import mega.privacy.android.app.presentation.login.LoginNavKey
import mega.privacy.android.app.presentation.login.LoginNavigationHandler
import mega.privacy.android.app.presentation.login.LoginViewModel
import mega.privacy.android.app.presentation.login.StartRoute
import mega.privacy.android.app.presentation.login.confirmemail.updateEmail.UpdateEmailForAccountCreationScreen
import mega.privacy.android.app.presentation.login.confirmemail.updateEmail.UpdateEmailForAccountCreationViewModel
import mega.privacy.android.app.presentation.login.confirmemail.updateEmail.navigateToUpdateEmailForAccountCreation
import mega.privacy.android.app.presentation.login.confirmemail.view.NewConfirmEmailRoute
import mega.privacy.android.app.presentation.login.createaccount.CreateAccountNavKey
import mega.privacy.android.app.presentation.login.onboarding.TourNavKey
import mega.privacy.android.app.utils.Constants.HELP_CENTRE_HOME_URL
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.navkey.NoSessionNavKey

@Serializable
data object ConfirmationEmailNavKey : NoSessionNavKey.Mandatory

internal fun NavGraphBuilder.confirmationEmailScreen(
    navController: NavController,
    onFinish: () -> Unit,
    stopShowingSplashScreen: () -> Unit,
    activityViewModel: LoginViewModel? = null,
) {
    composable<ConfirmationEmailNavKey> { backStackEntry ->
        val newEmail =
            backStackEntry.savedStateHandle.get<String>(UpdateEmailForAccountCreationViewModel.EMAIL)
        val context = LocalContext.current
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
            NewConfirmEmailRoute(
                newEmail = newEmail,
                onShowPendingFragment = sharedViewModel::setPendingFragmentToShow,
                onNavigateToChangeEmailAddress = { email, fullName ->
                    navController.navigateToUpdateEmailForAccountCreation(
                        email = email,
                        fullName = fullName,
                    )
                },
                onNavigateToHelpCentre = {
                    context.launchUrl(HELP_CENTRE_HOME_URL)
                },
                onBackPressed = onFinish,
                checkTemporalCredentials = sharedViewModel::checkTemporalCredentials,
                cancelCreateAccount = sharedViewModel::cancelCreateAccount,
                onSetTemporalEmail = sharedViewModel::setTemporalEmail
            )
        }
    }
}

internal fun EntryProviderScope<NavKey>.confirmationEmailScreen(
    navigationHandler: NavigationHandler,
    onFinish: () -> Unit,
    sharedViewModel: LoginViewModel,
) {
    entry<ConfirmationEmailNavKey> { key ->
        val context = LocalContext.current
        val result by navigationHandler.monitorResult<String>(UpdateEmailForAccountCreationViewModel.EMAIL)
            .collectAsStateWithLifecycle("")
        NewConfirmEmailRoute(
            newEmail = result,
            onShowPendingFragment = sharedViewModel::setPendingFragmentToShow,
            onNavigateToChangeEmailAddress = { email, fullName ->
                navigationHandler.navigate(
                    UpdateEmailForAccountCreationScreen(
                        email = email,
                        fullName = fullName
                    )
                )
            },
            onNavigateToHelpCentre = {
                context.launchUrl(HELP_CENTRE_HOME_URL)
            },
            onBackPressed = onFinish,
            checkTemporalCredentials = sharedViewModel::checkTemporalCredentials,
            cancelCreateAccount = sharedViewModel::cancelCreateAccount,
            onSetTemporalEmail = sharedViewModel::setTemporalEmail
        )
    }
}

internal fun NavController.openConfirmationEmailScreen(navOptions: NavOptions? = null) {
    navigate(ConfirmationEmailNavKey, navOptions)
}