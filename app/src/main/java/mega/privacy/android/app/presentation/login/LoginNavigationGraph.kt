package mega.privacy.android.app.presentation.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalFocusManager
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSavedStateNavEntryDecorator
import androidx.navigation3.scene.rememberSceneSetupNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import de.palm.composestateevents.EventEffect
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import mega.privacy.android.app.appstate.content.navigation.NavigationHandlerImpl
import mega.privacy.android.app.globalmanagement.MegaChatRequestHandler
import mega.privacy.android.app.presentation.login.confirmemail.changeemail.ChangeEmailAddressViewModel
import mega.privacy.android.app.presentation.login.confirmemail.changeemail.changeEmailAddress
import mega.privacy.android.app.presentation.login.confirmemail.confirmationEmailScreen
import mega.privacy.android.app.presentation.login.confirmemail.openConfirmationEmailScreen
import mega.privacy.android.app.presentation.login.createaccount.createAccountScreen
import mega.privacy.android.app.presentation.login.createaccount.openCreateAccountScreen
import mega.privacy.android.app.presentation.login.model.LoginScreen
import mega.privacy.android.app.presentation.login.onboarding.openTourScreen
import mega.privacy.android.app.presentation.login.onboarding.tourScreen
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.feature.payment.presentation.billing.BillingViewModel

@Serializable
data class LoginGraph(
    @SerialName(Constants.VISIBLE_FRAGMENT)
    val startScreen: Int? = null,
) : NavKey

@Serializable
data object StartRoute : NavKey

/**
 * Login navigation graph that can be integrated into other navigation graphs
 * using the navigation<> approach instead of creating a separate NavHost
 */
fun NavGraphBuilder.loginNavigationGraph(
    navController: NavController,
    chatRequestHandler: MegaChatRequestHandler,
    onFinish: () -> Unit,
    activityViewModel: LoginViewModel? = null,
    stopShowingSplashScreen: () -> Unit,
) {
    navigation<LoginGraph>(
        startDestination = StartRoute,
    ) {
        composable<StartRoute> { backStackEntry ->
            val sharedViewModel = activityViewModel ?: run {
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry<LoginGraph>()
                }
                hiltViewModel<LoginViewModel>(parentEntry)
            }
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
                    LoginScreen.CreateAccount -> navController.openCreateAccountScreen()

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
            chatRequestHandler = chatRequestHandler,
            onFinish = onFinish,
            stopShowingSplashScreen = stopShowingSplashScreen,
            activityViewModel = activityViewModel
        )
        createAccountScreen(
            navController = navController,
            chatRequestHandler = chatRequestHandler,
            onFinish = onFinish,
            stopShowingSplashScreen = stopShowingSplashScreen,
            activityViewModel = activityViewModel
        )
        tourScreen(
            navController = navController,
            chatRequestHandler = chatRequestHandler,
            onFinish = onFinish,
            stopShowingSplashScreen = stopShowingSplashScreen,
            onBackPressed = onFinish,
            activityViewModel = activityViewModel,
        )
        confirmationEmailScreen(
            navController = navController,
            chatRequestHandler = chatRequestHandler,
            onFinish = onFinish,
            stopShowingSplashScreen = stopShowingSplashScreen,
            activityViewModel = activityViewModel,
        )
        changeEmailAddress(
            navController = navController,
            chatRequestHandler = chatRequestHandler,
            onFinish = onFinish,
            stopShowingSplashScreen = stopShowingSplashScreen,
            onChangeEmailSuccess = { newEmail ->
                navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.set(ChangeEmailAddressViewModel.EMAIL, newEmail)
                navController.popBackStack()
            },
            activityViewModel = activityViewModel
        )
    }
}

@Composable
fun LoginNavDisplay(
    chatRequestHandler: MegaChatRequestHandler,
    onFinish: () -> Unit,
    activityViewModel: LoginViewModel? = null,
    stopShowingSplashScreen: () -> Unit,
) {
    val backStack = rememberNavBackStack(StartRoute)
    // Create NavigationHandler to provide a clean abstraction over backStack operations
    // This allows screens to use high-level navigation methods instead of direct backStack manipulation
    val loginNavigationHandler = remember {
        NavigationHandlerImpl(backStack)
    }
    val sharedViewModel = activityViewModel ?: hiltViewModel<LoginViewModel>()
    val billingViewModel = hiltViewModel<BillingViewModel>()
    NavDisplay(
        backStack = backStack,
        entryDecorators = listOf(
            rememberSceneSetupNavEntryDecorator(),
            rememberSavedStateNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = entryProvider {

            loginStartScreen(
                sharedViewModel = sharedViewModel,
                navigationHandler = loginNavigationHandler,
                stopShowingSplashScreen = stopShowingSplashScreen
            )

            loginScreen(
                navigationHandler = loginNavigationHandler,
                chatRequestHandler = chatRequestHandler,
                onFinish = onFinish,
                stopShowingSplashScreen = stopShowingSplashScreen,
                sharedViewModel = sharedViewModel,
                billingViewModel = billingViewModel
            )

            createAccountScreen(
                navigationHandler = loginNavigationHandler,
                chatRequestHandler = chatRequestHandler,
                onFinish = onFinish,
                stopShowingSplashScreen = stopShowingSplashScreen,
                sharedViewModel = sharedViewModel
            )

            tourScreen(
                navigationHandler = loginNavigationHandler,
                chatRequestHandler = chatRequestHandler,
                onFinish = onFinish,
                stopShowingSplashScreen = stopShowingSplashScreen,
                sharedViewModel = sharedViewModel,
                onBackPressed = onFinish
            )

            confirmationEmailScreen(
                navigationHandler = loginNavigationHandler,
                chatRequestHandler = chatRequestHandler,
                onFinish = onFinish,
                stopShowingSplashScreen = stopShowingSplashScreen,
                sharedViewModel = sharedViewModel
            )

            changeEmailAddress(
                navigationHandler = loginNavigationHandler,
                chatRequestHandler = chatRequestHandler,
                onFinish = onFinish,
                stopShowingSplashScreen = stopShowingSplashScreen,
                sharedViewModel = sharedViewModel,
                onChangeEmailSuccess = { newEmail ->
                    loginNavigationHandler.returnResult(ChangeEmailAddressViewModel.EMAIL, newEmail)
                }
            )
        }
    )
}