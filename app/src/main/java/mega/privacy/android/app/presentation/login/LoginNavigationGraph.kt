package mega.privacy.android.app.presentation.login

import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalFocusManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import de.palm.composestateevents.EventEffect
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import mega.privacy.android.app.presentation.login.confirmemail.confirmationEmailScreen
import mega.privacy.android.app.presentation.login.confirmemail.openConfirmationEmailScreen
import mega.privacy.android.app.presentation.login.confirmemail.updateEmail.UpdateEmailForAccountCreationViewModel
import mega.privacy.android.app.presentation.login.confirmemail.updateEmail.updateEmailForAccountCreation
import mega.privacy.android.app.presentation.login.createaccount.createAccountScreen
import mega.privacy.android.app.presentation.login.createaccount.openCreateAccountScreen
import mega.privacy.android.app.presentation.login.model.LoginScreen
import mega.privacy.android.app.presentation.login.onboarding.openTourScreen
import mega.privacy.android.app.presentation.login.onboarding.tourScreen
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.navkey.NoSessionNavKey

@Serializable
data class LoginGraph(
    @SerialName(Constants.VISIBLE_FRAGMENT)
    val startScreen: Int? = null,
) : NoSessionNavKey.Mandatory

@Serializable
data object StartRoute : NoSessionNavKey.Mandatory

/**
 * Login navigation graph that can be integrated into other navigation graphs
 * using the navigation<> approach instead of creating a separate NavHost
 */
fun NavGraphBuilder.loginNavigationGraph(
    navController: NavController,
    onFinish: () -> Unit,
    activityViewModel: LoginViewModel,
    stopShowingSplashScreen: () -> Unit,
) {
    navigation<LoginGraph>(
        startDestination = StartRoute,
    ) {
        composable<StartRoute> { backStackEntry ->
            val sharedViewModel = activityViewModel
            val uiState by sharedViewModel.state.collectAsStateWithLifecycle()
            val focusManager = LocalFocusManager.current
            // start composable to handle the initial state and navigation logic
            EventEffect(
                uiState.isPendingToShowFragment,
                sharedViewModel::isPendingToShowFragmentConsumed
            ) {

                if (it != LoginScreen.LoginScreen) {
                    stopShowingSplashScreen()
                }

                when (it) {
                    LoginScreen.LoginScreen -> navController.openLoginScreen()
                    LoginScreen.CreateAccount -> {
                        navController.openCreateAccountScreen(uiState.initialEmail)
                        sharedViewModel.onInitialEmailConsumed()
                    }

                    LoginScreen.Tour -> {
                        focusManager.clearFocus()
                        navController.openTourScreen(
                            NavOptions.Builder()
                                .setPopUpTo(route = StartRoute, inclusive = false)
                                .build()
                        )
                    }

                    LoginScreen.ConfirmEmail -> navController.openConfirmationEmailScreen()
                }
            }
        }
        loginScreen(
            navController = navController,
            onFinish = onFinish,
            stopShowingSplashScreen = stopShowingSplashScreen,
            activityViewModel = activityViewModel
        )
        createAccountScreen(
            navController = navController,
            onFinish = onFinish,
            stopShowingSplashScreen = stopShowingSplashScreen,
            activityViewModel = activityViewModel
        )
        tourScreen(
            navController = navController,
            onFinish = onFinish,
            stopShowingSplashScreen = stopShowingSplashScreen,
            onBackPressed = onFinish,
            activityViewModel = activityViewModel,
        )
        confirmationEmailScreen(
            navController = navController,
            onFinish = onFinish,
            stopShowingSplashScreen = stopShowingSplashScreen,
            activityViewModel = activityViewModel,
        )
        updateEmailForAccountCreation(
            navController = navController,
            onFinish = onFinish,
            stopShowingSplashScreen = stopShowingSplashScreen,
            onChangeEmailSuccess = { newEmail ->
                navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.set(UpdateEmailForAccountCreationViewModel.EMAIL, newEmail)
                navController.popBackStack()
            },
            activityViewModel = activityViewModel
        )
    }
}

internal fun EntryProviderScope<NavKey>.loginEntryProvider(
    navigationHandler: NavigationHandler,
    loginViewModel: LoginViewModel,
    onFinish: () -> Unit,
) {
    loginScreen(
        sharedViewModel = loginViewModel,
    )

    createAccountScreen(
        sharedViewModel = loginViewModel
    )

    tourScreen(
        sharedViewModel = loginViewModel,
        onBackPressed = onFinish,
    )

    confirmationEmailScreen(
        navigationHandler = navigationHandler,
        onFinish = onFinish,
        sharedViewModel = loginViewModel
    )

    updateEmailForAccountCreation(
        navigationHandler = navigationHandler,
        onChangeEmailSuccess = { newEmail ->
            navigationHandler.returnResult(
                key = UpdateEmailForAccountCreationViewModel.EMAIL,
                value = newEmail
            )
        },
    )
}