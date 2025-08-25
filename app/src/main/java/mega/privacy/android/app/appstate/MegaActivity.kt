package mega.privacy.android.app.appstate

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.Serializable
import mega.android.core.ui.theme.AndroidTheme
import mega.privacy.android.app.appstate.content.AppContentStateViewModel
import mega.privacy.android.app.appstate.content.view.AppContentView
import mega.privacy.android.app.appstate.global.GlobalStateViewModel
import mega.privacy.android.app.appstate.global.model.GlobalState
import mega.privacy.android.app.globalmanagement.MegaChatRequestHandler
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.login.LoginGraph
import mega.privacy.android.app.presentation.login.LoginScreen
import mega.privacy.android.app.presentation.login.loginNavigationGraph
import mega.privacy.android.app.presentation.passcode.model.PasscodeCryptObjectFactory
import mega.privacy.android.app.utils.Constants
import javax.inject.Inject

@AndroidEntryPoint
class MegaActivity : ComponentActivity() {

    @Inject
    lateinit var passcodeCryptObjectFactory: PasscodeCryptObjectFactory

    @Inject
    lateinit var chatRequestHandler: MegaChatRequestHandler

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
            val state = viewModel.state.collectAsStateWithLifecycle()
            val navController = rememberNavController()
            // This is used to recompose the LoginGraph when new login request is made
            keepSplashScreen = state.value is GlobalState.Loading

            LaunchedEffect(state.value) {
                when (val currentState = state.value) {
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

            AndroidTheme(isDark = state.value.themeMode.isDarkMode()) {
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
                        val appContentStateViewModel = hiltViewModel<AppContentStateViewModel>()
                        AppContentView(
                            viewModel = appContentStateViewModel,
                        )
                    }
                }
            }
        }
    }
}
