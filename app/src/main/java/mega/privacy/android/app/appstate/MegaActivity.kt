package mega.privacy.android.app.appstate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.appstate.model.AuthState
import mega.privacy.android.app.appstate.view.LoggedInAppView
import mega.privacy.android.app.appstate.view.NotLoggedInAppView
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.passcode.model.PasscodeCryptObjectFactory
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import javax.inject.Inject

@AndroidEntryPoint
class MegaActivity : ComponentActivity() {

    @Inject
    lateinit var passcodeCryptObjectFactory: PasscodeCryptObjectFactory

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
            keepSplashScreen = state.value is AuthState.Loading

            OriginalTheme(isDark = state.value.themeMode.isDarkMode()) {
                when (val authState = state.value) {
                    is AuthState.Loading -> {}
                    is AuthState.LoggedIn -> {
                        LoggedInAppView(
                            navController = navController,
                            credentials = authState.credentials,
                        )
                    }

                    is AuthState.RequireLogin -> {
                        NotLoggedInAppView(
                            navController = navController,
                        )
                    }
                }
            }
        }
    }
}
