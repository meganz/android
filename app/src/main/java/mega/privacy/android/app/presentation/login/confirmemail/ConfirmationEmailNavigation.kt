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
import mega.privacy.android.app.globalmanagement.MegaChatRequestHandler
import mega.privacy.android.app.presentation.login.Login
import mega.privacy.android.app.presentation.login.LoginGraph
import mega.privacy.android.app.presentation.login.LoginGraphContent
import mega.privacy.android.app.presentation.login.LoginViewModel
import mega.privacy.android.app.presentation.login.StartRoute
import mega.privacy.android.app.presentation.login.confirmemail.changeemail.ChangeEmailAddressScreen
import mega.privacy.android.app.presentation.login.confirmemail.changeemail.ChangeEmailAddressViewModel
import mega.privacy.android.app.presentation.login.confirmemail.changeemail.navigateToChangeEmailAddress
import mega.privacy.android.app.presentation.login.confirmemail.view.NewConfirmEmailRoute
import mega.privacy.android.app.presentation.login.createaccount.CreateAccountRoute
import mega.privacy.android.app.presentation.login.onboarding.TourScreen
import mega.privacy.android.app.utils.Constants.HELP_CENTRE_HOME_URL
import mega.privacy.android.navigation.contract.NavigationHandler

@Serializable
data object ConfirmationEmailScreen : NavKey

internal fun NavGraphBuilder.confirmationEmailScreen(
    navController: NavController,
    chatRequestHandler: MegaChatRequestHandler,
    onFinish: () -> Unit,
    stopShowingSplashScreen: () -> Unit,
    activityViewModel: LoginViewModel? = null,
) {
    composable<ConfirmationEmailScreen> { backStackEntry ->
        val newEmail =
            backStackEntry.savedStateHandle.get<String>(ChangeEmailAddressViewModel.EMAIL)
        val context = LocalContext.current
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
            NewConfirmEmailRoute(
                newEmail = newEmail,
                onShowPendingFragment = sharedViewModel::setPendingFragmentToShow,
                onNavigateToChangeEmailAddress = { email, fullName ->
                    navController.navigateToChangeEmailAddress(
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
    chatRequestHandler: MegaChatRequestHandler,
    onFinish: () -> Unit,
    sharedViewModel: LoginViewModel,
    stopShowingSplashScreen: () -> Unit,
) {
    entry<ConfirmationEmailScreen> { key ->
        val context = LocalContext.current
        val result by navigationHandler.monitorResult<String>(ChangeEmailAddressViewModel.EMAIL)
            .collectAsStateWithLifecycle("")
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
            NewConfirmEmailRoute(
                newEmail = result,
                onShowPendingFragment = sharedViewModel::setPendingFragmentToShow,
                onNavigateToChangeEmailAddress = { email, fullName ->
                    navigationHandler.navigate(
                        ChangeEmailAddressScreen(
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
}

internal fun NavController.openConfirmationEmailScreen(navOptions: NavOptions? = null) {
    navigate(ConfirmationEmailScreen, navOptions)
}