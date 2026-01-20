package mega.privacy.android.app.presentation.login

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.android.core.ui.components.dialogs.BasicDialogButton
import mega.android.core.ui.theme.AndroidTheme
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.R
import mega.privacy.android.app.appstate.MegaActivity
import mega.privacy.android.app.globalmanagement.MegaChatRequestHandler
import mega.privacy.android.app.presentation.login.model.LoginScreen
import mega.privacy.android.app.presentation.security.PasscodeCheck
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.core.sharedcomponents.coroutine.collectFlow
import mega.privacy.android.core.sharedcomponents.extension.isDarkMode
import mega.privacy.android.domain.entity.AccountBlockedEvent
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.navigation.MegaNavigator
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

    @Inject
    lateinit var megaNavigator: MegaNavigator

    private val disabledPasscodeCheck = object : PasscodeCheck {
        override fun disablePasscode() {
//            no-op
        }

        override fun enablePassCode() {
//            no-op
        }

        override fun canLock() = false

    }

    private val viewModel: LoginViewModel by viewModels(
        extrasProducer = {
            defaultViewModelCreationExtras.withCreationCallback<LoginViewModel.Factory> { factory ->
                factory.create(isInSingleActivity = false)
            }
        }
    )

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
            megaNavigator.openManagerActivity(
                context = this,
                singleActivityDestination = null,
            )
            finish()
            return
        }

        val refreshForShare = intent.action == Constants.ACTION_FILE_EXPLORER_UPLOAD
        collectFlow(viewModel.state) { uiState ->
            if (uiState.accountSession?.session.isNullOrEmpty() && uiState.isSingleActivityEnabled && !refreshForShare) {
                startActivity(Intent(this@LoginActivity, MegaActivity::class.java))
                finish()
            }
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


                var showLoggedOutDialog by rememberSaveable { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    viewModel.monitorLoggedOutFromAnotherLocation.collectLatest { loggedOut ->
                        if (loggedOut) {
                            showLoggedOutDialog = true
                            viewModel.setHandledLoggedOutFromAnotherLocation()
                        }
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
                        activityViewModel = viewModel,
                    )
                }

                if (showLoggedOutDialog) {
                    BasicDialog(
                        modifier = Modifier.testTag(LOGGED_OUT_DIALOG),
                        title = stringResource(id = R.string.title_alert_logged_out),
                        description = stringResource(id = R.string.error_server_expired_session),
                        buttons = persistentListOf(
                            BasicDialogButton(
                                text = stringResource(id = R.string.general_ok),
                                onClick = {
                                    showLoggedOutDialog = false
                                }
                            ),
                        ),
                        onDismissRequest = {
                            showLoggedOutDialog = false
                        },
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
        const val ACTION_REFRESH_AND_OPEN_SESSION_LINK = "REFRESH_AND_OPEN_SESSION_LINK"

        fun getIntent(
            context: Context,
            action: String?,
            link: Uri?,
        ) = Intent(context, LoginActivity::class.java).apply {
            this.action = action
            data = link
        }
    }
}