package mega.privacy.android.app.appstate

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import de.palm.composestateevents.EventEffect
import kotlinx.serialization.Serializable
import mega.android.core.ui.theme.AndroidTheme
import mega.privacy.android.app.appstate.content.AppContentStateViewModel
import mega.privacy.android.app.appstate.content.navigation.NavigationHandlerImpl
import mega.privacy.android.app.appstate.content.view.AppContentView
import mega.privacy.android.app.appstate.global.GlobalStateViewModel
import mega.privacy.android.app.appstate.global.SnackbarEventsViewModel
import mega.privacy.android.app.appstate.global.event.AppDialogViewModel
import mega.privacy.android.app.appstate.global.model.GlobalState
import mega.privacy.android.app.appstate.global.util.show
import mega.privacy.android.app.globalmanagement.MegaChatRequestHandler
import mega.privacy.android.app.presentation.container.AppContainer
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.login.LoginGraph
import mega.privacy.android.app.presentation.login.LoginScreen
import mega.privacy.android.app.presentation.login.loginNavigationGraph
import mega.privacy.android.app.presentation.passcode.model.PasscodeCryptObjectFactory
import mega.privacy.android.app.presentation.security.check.PasscodeContainer
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.navigation.contract.AppDialogDestinations
import javax.inject.Inject

@AndroidEntryPoint
class MegaActivity : ComponentActivity() {

    @Inject
    lateinit var passcodeCryptObjectFactory: PasscodeCryptObjectFactory

    @Inject
    lateinit var chatRequestHandler: MegaChatRequestHandler

    @Inject
    lateinit var appDialogDestinations: Set<@JvmSuppressWildcards AppDialogDestinations>


    @Serializable
    data object LoginLoading

    @Serializable
    data class LoggedInScreens(val isFromLogin: Boolean = false, val session: String)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        this.intent = intent
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        var keepSplashScreen = true
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { keepSplashScreen }
        enableEdgeToEdge()
        setContent {
            val viewModel = viewModel<GlobalStateViewModel>()
            val appDialogViewModel = hiltViewModel<AppDialogViewModel>()

            val state by viewModel.state.collectAsStateWithLifecycle()
            val dialogEvents by appDialogViewModel.dialogEvents.collectAsStateWithLifecycle()

            val snackbarEventsViewModel = viewModel<SnackbarEventsViewModel>()
            val snackbarEventsState by snackbarEventsViewModel.snackbarEventState.collectAsStateWithLifecycle()
            val navController = rememberNavController()
            val navigationHandler = remember { NavigationHandlerImpl(navController) }
            val snackbarHostState = remember { SnackbarHostState() }
            // This is used to recompose the LoginGraph when new login request is made
            keepSplashScreen = state is GlobalState.Loading

            LaunchedEffect(state) {
                when (val currentState = state) {
                    is GlobalState.Loading -> {}
                    is GlobalState.LoggedIn -> {
                        val isFromLogin =
                            navController.currentBackStackEntry?.destination?.hasRoute<LoginScreen>() == true
                        navController.navigate(
                            route = LoggedInScreens(
                                isFromLogin = isFromLogin,
                                session = currentState.session
                            )
                        ) {
                            popUpTo(0) {
                                inclusive = true
                            }
                        }
                    }

                    is GlobalState.RequireLogin -> {
                        navController.navigate(
                            route = LoginGraph(
                                startScreen = intent.getIntExtra(Constants.VISIBLE_FRAGMENT, -1)
                                    .takeIf { it != -1 }
                            ),
                        ) {
                            popUpTo(0) {
                                inclusive = true
                            }
                        }
                    }
                }
            }

            EventEffect(event = dialogEvents, onConsumed = appDialogViewModel::dialogDisplayed) {
                navController.navigate(it.dialogDestination)
            }

            AndroidTheme(isDark = state.themeMode.isDarkMode()) {
                EventEffect(
                    event = snackbarEventsState,
                    onConsumed = snackbarEventsViewModel::consumeEvent,
                    action = { snackbarHostState.show(it) }
                )

                NavHost(
                    navController = navController,
                    startDestination = LoginLoading,
                ) {

                    composable<LoginLoading> {}

                    loginNavigationGraph(
                        navController = navController,
                        chatRequestHandler = chatRequestHandler,
                        onFinish = {
                            navController.popBackStack(LoginGraph, inclusive = true)
                        },
                        stopShowingSplashScreen = {
                            keepSplashScreen = false
                        },
                    )

                    composable<LoggedInScreens> {
                        AppContainer(
                            containers = containers,
                        ) {
                            val appContentStateViewModel = hiltViewModel<AppContentStateViewModel>()
                            AppContentView(
                                viewModel = appContentStateViewModel,
                                snackbarHostState = snackbarHostState,
                            )
                        }
                    }

                    appDialogDestinations.forEach {
                        it.navigationGraph(
                            this,
                            navigationHandler,
                            appDialogViewModel::eventHandled
                        )
                    }

                }
            }
        }
    }

    private val containers: List<@Composable (@Composable () -> Unit) -> Unit> = listOf(
        {
            PasscodeContainer(
                passcodeCryptObjectFactory = passcodeCryptObjectFactory,
                content = it
            )
        },
    )
}
