package mega.privacy.android.app.presentation.login

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.globalmanagement.MegaChatRequestHandler
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.billing.BillingViewModel
import mega.privacy.android.app.presentation.login.model.LoginFragmentType
import mega.privacy.android.app.presentation.security.PasscodeCheck
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.AccountBlockedEvent
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
    private val billingViewModel by viewModels<BillingViewModel>()

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
            LoginGraph(
                chatRequestHandler = chatRequestHandler,
                viewModel = viewModel,
                billingViewModel = billingViewModel,
                onFinish = ::finish,
                stopShowingSplashScreen = ::stopShowingSplashScreen
            )
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