package mega.privacy.android.app.appstate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.Serializable
import mega.android.core.ui.theme.AndroidTheme
import mega.privacy.android.app.appstate.model.AuthState
import mega.privacy.android.app.appstate.view.LoggedInAppView
import mega.privacy.android.app.globalmanagement.MegaChatRequestHandler
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.login.LoginGraph
import mega.privacy.android.app.presentation.passcode.model.PasscodeCryptObjectFactory
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
    data object LoginScreens

    @Serializable
    data class LoggedInScreens(val session: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        var keepSplashScreen = true
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { keepSplashScreen }
        enableEdgeToEdge()
        setContent {
            val viewModel = viewModel<AuthStateViewModel>()
            val state = viewModel.state.collectAsStateWithLifecycle()
            val navController = rememberNavController()
            // This is used to recompose the LoginGraph when new login request is made
            keepSplashScreen = state.value is AuthState.Loading

            LaunchedEffect(state.value) {
                when (val currentState = state.value) {
                    is AuthState.Loading -> {}
                    is AuthState.LoggedIn -> {
                        navController.popBackStack()
                        navController.navigate(route = LoggedInScreens(session = currentState.session))
                    }

                    is AuthState.RequireLogin -> {
                        navController.popBackStack()
                        navController.navigate(route = LoginScreens)
                    }
                }
            }

            AndroidTheme(isDark = state.value.themeMode.isDarkMode()) {
                NavHost(
                    navController = navController,
                    startDestination = LoginLoading
                ) {

                    composable<LoginLoading> {
                        // Splash screen or empty? Technically this should never be seen.
                        Text("Loading...")
                    }

                    composable<LoginScreens> {
                        LoginGraph(
                            chatRequestHandler = chatRequestHandler,
                            onFinish = {
                                navController.popBackStack()
                            },
                            stopShowingSplashScreen = {
                                keepSplashScreen = false
                            }
                        )
                    }
                    composable<LoggedInScreens> {
                        val appStateViewModel = hiltViewModel<AppStateViewModel>()
                        LoggedInAppView(
                            viewModel = appStateViewModel,
                        )
                    }
                }
            }
        }
    }
}
