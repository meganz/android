package mega.privacy.android.app.appstate

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import dagger.hilt.android.AndroidEntryPoint
import de.palm.composestateevents.EventEffect
import kotlinx.serialization.Serializable
import mega.android.core.ui.theme.AndroidTheme
import mega.privacy.android.app.appstate.content.AppContentStateViewModel
import mega.privacy.android.app.appstate.content.view.AppContentView
import mega.privacy.android.app.appstate.global.GlobalStateViewModel
import mega.privacy.android.app.appstate.global.SnackbarEventsViewModel
import mega.privacy.android.app.appstate.global.model.GlobalState
import mega.privacy.android.app.appstate.global.util.show
import mega.privacy.android.app.globalmanagement.MegaChatRequestHandler
import mega.privacy.android.app.presentation.container.AppContainer
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.login.LoginNavDisplay
import mega.privacy.android.app.presentation.passcode.model.PasscodeCryptObjectFactory
import mega.privacy.android.app.presentation.security.check.PasscodeContainer
import javax.inject.Inject

@AndroidEntryPoint
class MegaActivity : ComponentActivity() {

    @Inject
    lateinit var passcodeCryptObjectFactory: PasscodeCryptObjectFactory

    @Inject
    lateinit var chatRequestHandler: MegaChatRequestHandler

    @Serializable
    data class LoggedInScreens(val isFromLogin: Boolean = false, val session: String) : NavKey

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

            val state by viewModel.state.collectAsStateWithLifecycle()
            val snackbarEventsViewModel = viewModel<SnackbarEventsViewModel>()
            val snackbarEventsState by snackbarEventsViewModel.snackbarEventState.collectAsStateWithLifecycle()
            val snackbarHostState = remember { SnackbarHostState() }
            // This is used to recompose the LoginGraph when new login request is made
            var fromLogin by remember { mutableStateOf(false) }
            keepSplashScreen = state is GlobalState.Loading

            AndroidTheme(isDark = state.themeMode.isDarkMode()) {
                EventEffect(
                    event = snackbarEventsState,
                    onConsumed = snackbarEventsViewModel::consumeEvent,
                    action = { snackbarHostState.show(it) }
                )

                when (val currentState = state) {
                    is GlobalState.Loading -> {}
                    is GlobalState.LoggedIn -> {
                        AppContainer(
                            containers = containers,
                        ) {
                            val appContentStateViewModel = hiltViewModel<AppContentStateViewModel>()
                            AppContentView(
                                viewModel = appContentStateViewModel,
                                snackbarHostState = snackbarHostState,
                                navKey = LoggedInScreens(
                                    isFromLogin = fromLogin,
                                    session = currentState.session
                                ),
                            )
                        }
                    }

                    is GlobalState.RequireLogin -> {
                        fromLogin = true
                        LoginNavDisplay(
                            chatRequestHandler = chatRequestHandler,
                            onFinish = ::finish,
                            stopShowingSplashScreen = {
                                keepSplashScreen = false
                            },
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
