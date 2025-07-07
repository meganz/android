package mega.privacy.android.app.presentation.login

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalFocusManager
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.globalmanagement.MegaChatRequestHandler
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.login.confirmemail.confirmationEmailScreen
import mega.privacy.android.app.presentation.login.confirmemail.openConfirmationEmailScreen
import mega.privacy.android.app.presentation.login.createaccount.createAccountScreen
import mega.privacy.android.app.presentation.login.createaccount.openCreateAccountScreen
import mega.privacy.android.app.presentation.login.model.LoginFragmentType
import mega.privacy.android.app.presentation.login.onboarding.openTourScreen
import mega.privacy.android.app.presentation.login.onboarding.tourScreen
import mega.privacy.android.app.presentation.security.PasscodeCheck
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Util
import mega.privacy.android.domain.entity.account.AccountBlockedDetail
import mega.privacy.android.domain.exception.LoginLoggedOutFromOtherLocation
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

        LoginFragmentType.entries.find { it.value == visibleFragment }?.let {
            viewModel.setPendingFragmentToShow(it)
        }
    }

    override fun onDestroy() {
        Timber.d("onDestroy")
        chatRequestHandler.setIsLoggingRunning(false)
        super.onDestroy()
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
        chatRequestHandler.setIsLoggingRunning(true)

        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.state.collectAsStateWithLifecycle()
            val navController = rememberNavController()
            val focusManager = LocalFocusManager.current

            LaunchedEffect(uiState.isPendingToShowFragment) {
                val fragmentType = uiState.isPendingToShowFragment
                if (fragmentType != null) {
                    if (fragmentType != LoginFragmentType.Login) {
                        stopShowingSplashScreen()
                    }

                    when (fragmentType) {
                        LoginFragmentType.Login -> navController.openLoginScreen()
                        LoginFragmentType.CreateAccount -> navController.openCreateAccountScreen()

                        LoginFragmentType.Tour -> {
                            focusManager.clearFocus()
                            navController.openTourScreen(
                                NavOptions.Builder()
                                    .setPopUpTo(route = "start", inclusive = false)
                                    .build()
                            )
                        }

                        LoginFragmentType.ConfirmEmail -> navController.openConfirmationEmailScreen()
                    }
                    viewModel.isPendingToShowFragmentConsumed()
                }
            }

            NavHost(
                navController = navController,
                startDestination = "start"
            ) {
                composable("start") {
                    // no-op, we checking start destination in the view model
                }
                loginScreen(
                    onBackPressed = {
                        viewModel.triggerOnBackPressedEvent()
                    }
                )
                createAccountScreen(
                    onBackPressed = {
                        viewModel.setPendingFragmentToShow(LoginFragmentType.Tour)
                    }
                )
                tourScreen(
                    activityViewModel = viewModel,
                    onBackPressed = {
                        finish()
                    }
                )
                confirmationEmailScreen(
                    activityViewModel = viewModel,
                    onBackPressed = {
                        finish()
                    }
                )
            }
        }
        splashScreen.setupSplashExitAnimation(window)
        setupObservers()
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

    private fun setupObservers() {
        collectFlow(viewModel.state, Lifecycle.State.RESUMED) { uiState ->
            with(uiState) {
                when {
                    isPendingToFinishActivity -> finish()

                    loginException != null -> {
                        if (loginException is LoginLoggedOutFromOtherLocation) {
                            showAlertLoggedOut()
                            viewModel.setLoginErrorConsumed()
                        }
                    }
                }
            }
        }

        collectFlow(viewModel.monitorLoggedOutFromAnotherLocation) { loggedOut ->
            if (loggedOut) {
                Timber.d("logged out from another location")
                showAlertLoggedOut()
                viewModel.setHandledLoggedOutFromAnotherLocation()
            }
        }
    }

    /**
     * Show fragment
     *
     * @param fragmentType
     */
    fun showFragment(fragmentType: LoginFragmentType) {
        viewModel.setPendingFragmentToShow(fragmentType)
    }

    override fun shouldSetStatusBarTextColor() = false

    /**
     * Shows a warning informing the account has been logged out.
     */
    private fun showAlertLoggedOut() {
        Timber.d("showAlertLoggedOut")
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.title_alert_logged_out))
            .setMessage(getString(R.string.error_server_expired_session))
            .setPositiveButton(getString(R.string.general_ok), null)
            .show()
    }

    public override fun onResume() {
        Timber.d("onResume")
        super.onResume()

        if (intent?.action != null) {
            when (intent.action) {
                Constants.ACTION_CANCEL_CAM_SYNC -> showCancelCUWarning()
            }
        }
    }

    private fun showCancelCUWarning() {
        Timber.d("ACTION_CANCEL_CAM_SYNC")
        val title = getString(R.string.cam_sync_syncing)
        val text = getString(R.string.cam_sync_cancel_sync)

        Util.getCustomAlertBuilder(this, title, text, null)
            .setPositiveButton(getString(R.string.general_yes)) { _, _ ->
                viewModel.stopCameraUploads()
            }.setNegativeButton(getString(R.string.general_no), null)
            .show()
    }

    fun showAccountBlockedDialog(accountBlockedDetail: AccountBlockedDetail) {
        viewModel.triggerAccountBlockedEvent(accountBlockedDetail)
    }

    override val allowToShowOverQuotaWarning: Boolean = false

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
