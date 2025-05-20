package mega.privacy.android.app.presentation.login

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
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
import mega.privacy.android.app.presentation.login.createaccount.CreateAccountScreen
import mega.privacy.android.app.presentation.login.createaccount.createAccountScreen
import mega.privacy.android.app.presentation.login.createaccount.openCreateAccountScreen
import mega.privacy.android.app.presentation.login.model.LoginFragmentType
import mega.privacy.android.app.presentation.login.onboarding.TourScreen
import mega.privacy.android.app.presentation.login.onboarding.openTourScreen
import mega.privacy.android.app.presentation.login.onboarding.tourScreen
import mega.privacy.android.app.presentation.security.PasscodeCheck
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Util
import mega.privacy.android.domain.entity.account.AccountBlockedDetail
import mega.privacy.android.domain.exception.LoginLoggedOutFromOtherLocation
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
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            val visibleFragment = when {
                currentDestination?.hierarchy?.any {
                    it.route == LoginScreen::class.qualifiedName
                } == true -> Constants.LOGIN_FRAGMENT

                currentDestination?.hierarchy?.any {
                    it.route == CreateAccountScreen::class.qualifiedName
                } == true -> Constants.CREATE_ACCOUNT_FRAGMENT

                currentDestination?.hierarchy?.any {
                    it.route == TourScreen::class.qualifiedName
                } == true -> Constants.TOUR_FRAGMENT

                else -> Constants.CONFIRM_EMAIL_FRAGMENT
            }

            LaunchedEffect(uiState.isLoginNewDesignEnabled, visibleFragment) {
                restrictOrientation(uiState.isLoginNewDesignEnabled == true, visibleFragment)
            }

            LaunchedEffect(uiState.isPendingToShowFragment) {
                val fragmentType = uiState.isPendingToShowFragment
                if (fragmentType != null) {
                    if (fragmentType != LoginFragmentType.Login) {
                        stopShowingSplashScreen()
                    }

                    when (fragmentType) {
                        LoginFragmentType.Login -> navController.openLoginScreen()
                        LoginFragmentType.CreateAccount -> navController.openCreateAccountScreen()
                        LoginFragmentType.Tour -> navController.openTourScreen()
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
                loginScreen()
                createAccountScreen()
                tourScreen()
                confirmationEmailScreen()
            }
        }
        setupSplashExitAnimation(splashScreen)
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
     * Disables the splash screen exit animation to prevent a visual "jump" of the app icon.
     *
     * Skipped on Android 13 (Tiramisu) for certain Chinese OEMs (e.g., OPPO, Realme, OnePlus),
     * or specific models (e.g., Galaxy A03 Core), as it may cause crashes.
     * See: https://issuetracker.google.com/issues/242118185
     */
    private fun setupSplashExitAnimation(splashScreen: SplashScreen) {
        val isAndroid13 = Build.VERSION.SDK_INT == Build.VERSION_CODES.TIRAMISU
        val isAffectedBrand = Build.BRAND.lowercase() in setOf("oppo", "realme", "oneplus")
        val isAffectedModel = Build.MODEL.lowercase().contains("a03 core")

        if (isAndroid13 && (isAffectedBrand || isAffectedModel)) return

        splashScreen.setOnExitAnimationListener {
            it.remove()
            WindowCompat.setDecorFitsSystemWindows(window, false)
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

    /**
     * Restrict to portrait mode always for mobile devices and tablets (already restricted via Manifest).
     * Allow the landscape mode only for tablets and only for TOUR_FRAGMENT.
     */
    @SuppressLint("SourceLockedOrientationActivity")
    private fun restrictOrientation(isLoginNewDesignEnabled: Boolean, visibleFragment: Int) {
        requestedOrientation =
            if (isLoginNewDesignEnabled || visibleFragment == Constants.TOUR_FRAGMENT || visibleFragment == Constants.CREATE_ACCOUNT_FRAGMENT) {
                Timber.d("Tour/create account screen landscape mode allowed")
                ActivityInfo.SCREEN_ORIENTATION_FULL_USER
            } else {
                Timber.d("Other screens landscape mode restricted")
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
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

    /**
     * Sets the received string as temporal email.
     *
     * @param emailTemp The temporal email.
     */
    fun setTemporalEmail(emailTemp: String) {
        viewModel.setTemporalEmail(emailTemp)
    }

    /**
     * Cancels the account confirmation.
     */
    fun cancelConfirmationAccount() {
        Timber.d("cancelConfirmationAccount")
        viewModel.cancelCreateAccount()
    }

    /**
     * Sets temporal data for account creation.
     *
     * @param email    Email.
     */
    fun setTemporalDataForAccountCreation(
        email: String,
    ) {
        setTemporalEmail(email)
        viewModel.setIsWaitingForConfirmAccount()
    }

    fun showAccountBlockedDialog(accountBlockedDetail: AccountBlockedDetail) {
        viewModel.triggerAccountBlockedEvent(accountBlockedDetail)
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
