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
import mega.privacy.android.app.appstate.model.AppState
import mega.privacy.android.app.appstate.view.MegaApp
import mega.privacy.android.app.presentation.passcode.model.PasscodeCryptObjectFactory
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
            val viewModel = viewModel<AppStateViewModel>()
            val state = viewModel.state.collectAsStateWithLifecycle()
            val navController = rememberNavController()
            when (val appState = state.value) {
                is AppState.Data -> {
                    keepSplashScreen = false
                    MegaApp(
                        navController = navController,
                        passcodeCryptObjectFactory = passcodeCryptObjectFactory,
                        appState = appState
                    )
                }

                AppState.Loading -> {}
            }
        }
    }
}
