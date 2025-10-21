package mega.privacy.android.app.presentation.login

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mega.android.core.ui.theme.AndroidTheme
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.globalmanagement.MegaChatRequestHandler
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.login.model.LoginScreen
import mega.privacy.android.app.presentation.security.PasscodeCheck
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.AccountBlockedEvent
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.shared.original.core.ui.utils.setupSplashExitAnimation
import timber.log.Timber
import javax.inject.Inject

/**
 * Login Activity.
 *
 * @property chatRequestHandler       [MegaChatRequestHandler]
 */
@AndroidEntryPoint
class LoginActivity : BaseActivity() {

    @Inject
    lateinit var chatRequestHandler: MegaChatRequestHandler

    @Inject
    lateinit var monitorThemeModeUseCase: MonitorThemeModeUseCase

    private val disabledPasscodeCheck = object : PasscodeCheck {
        override fun disablePasscode() {
//            no-op
        }

        override fun enablePassCode() {
//            no-op
        }

        override fun canLock() = false

    }

    private val viewModel by viewModels<LoginViewModel>()

    /**
     * Flag to delay showing the splash screen.
     */
    private var keepShowingSplashScreen = true

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        val visibleFragment =
            intent.getIntExtra(Constants.VISIBLE_FRAGMENT, Constants.LOGIN_FRAGMENT)

        LoginScreen.entries.find { it.value == visibleFragment }?.let {
            viewModel.setPendingFragmentToShow(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("onCreate")
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition {
            keepShowingSplashScreen
        }
        super.onCreate(savedInstanceState)
        appContainerWrapper.setPasscodeCheck(disabledPasscodeCheck)
        if (intent.action == Intent.ACTION_MAIN
            && intent.hasCategory(Intent.CATEGORY_LAUNCHER)
            && !viewModel.isConnected
        ) {
            // in case offline mode, go to ManagerActivity
            stopShowingSplashScreen()
            startActivity(Intent(this, ManagerActivity::class.java))
            finish()
            return
        }

        enableEdgeToEdge()
        setContent {
            val themeMode by monitorThemeModeUseCase().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            val navController = rememberNavController()
            AndroidTheme(isDark = themeMode.isDarkMode()) {

                val lifecycleOwner = LocalLifecycleOwner.current

                DisposableEffect(Unit) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_CREATE) {
                            chatRequestHandler.setIsLoginRunning(true)
                        } else if (event == Lifecycle.Event.ON_DESTROY) {
                            chatRequestHandler.setIsLoginRunning(false)
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = LoginGraph(
                        startScreen = intent.getIntExtra(Constants.VISIBLE_FRAGMENT, -1)
                            .takeIf { it != -1 }
                    ),
                ) {
                    loginNavigationGraph(
                        navController = navController,
                        onFinish = ::finish,
                        stopShowingSplashScreen = ::stopShowingSplashScreen,
                        activityViewModel = viewModel
                    )
                }
            }
        }
        splashScreen.setupSplashExitAnimation(window)
        lifecycleScope.launch {
            // A fail-safe to avoid the splash screen to be shown forever
            // in case not called by expected fragments
            delay(1500)
            if (keepShowingSplashScreen) {
                stopShowingSplashScreen()
                Timber.w("Splash screen is being shown for too long")
            }
        }
    }

    /**
     * Stops showing the splash screen.
     */
    fun stopShowingSplashScreen() {
        keepShowingSplashScreen = false
    }

    override fun shouldSetStatusBarTextColor() = false

    fun showAccountBlockedDialog(accountBlockedEvent: AccountBlockedEvent) {
        viewModel.triggerAccountBlockedEvent(accountBlockedEvent)
    }

    companion object {

        /**
         * Flag for knowing if it was already in the login page.
         */
        @JvmField
        var isBackFromLoginPage = false

        /**
         * Intent extra for knowing if the user is logged in.
         */
        const val EXTRA_IS_LOGGED_IN = "isLoggedIn"
    }
}