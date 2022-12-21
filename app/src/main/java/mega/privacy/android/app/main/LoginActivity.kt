package mega.privacy.android.app.main

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.graphics.Rect
import android.os.Bundle
import android.util.TypedValue
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.widget.Toolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.constants.BroadcastConstants
import mega.privacy.android.app.databinding.ActivityLoginBinding
import mega.privacy.android.app.globalmanagement.MegaChatRequestHandler
import mega.privacy.android.app.interfaces.OnKeyboardVisibilityListener
import mega.privacy.android.app.presentation.extensions.getFormattedStringOrDefault
import mega.privacy.android.app.presentation.login.LoginViewModel
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.JobUtil
import mega.privacy.android.app.utils.Util
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import nz.mega.sdk.MegaTransfer
import timber.log.Timber
import javax.inject.Inject

/**
 * Login Activity.
 *
 * @property chatRequestHandler       [MegaChatRequestHandler]
 */
@AndroidEntryPoint
class LoginActivity : BaseActivity(), MegaRequestListenerInterface {

    @Inject
    lateinit var chatRequestHandler: MegaChatRequestHandler

    private val viewModel by viewModels<LoginViewModel>()

    private lateinit var binding: ActivityLoginBinding

    private var cancelledConfirmationProcess = false

    //Fragments
    private var tourFragment: TourFragment? = null
    private var loginFragment: LoginFragment? = null
    private var createAccountFragment: CreateAccountFragment? = null

    private var visibleFragment = 0
    private var waitingForConfirmAccount = false
    private var sessionTemp: String? = null
    private var emailTemp: String? = null
    private var passwdTemp: String? = null
    private var firstNameTemp: String? = null
    private var lastNameTemp: String? = null

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            Timber.d("onBackPressed")
            retryConnectionsAndSignalPresence()
            var valueReturn = -1

            when (visibleFragment) {
                Constants.LOGIN_FRAGMENT -> valueReturn = loginFragment?.onBackPressed() ?: 0
                Constants.CREATE_ACCOUNT_FRAGMENT -> showFragment(Constants.TOUR_FRAGMENT)
                Constants.TOUR_FRAGMENT, Constants.CONFIRM_EMAIL_FRAGMENT -> valueReturn = 0
            }

            if (valueReturn == 0) {
                finish()
            }
        }
    }

    private val updateMyAccountReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val actionType = intent?.getIntExtra(BroadcastConstants.ACTION_TYPE,
                BroadcastConstants.INVALID_ACTION)

            if (actionType == Constants.UPDATE_PAYMENT_METHODS) {
                Timber.d("BROADCAST TO UPDATE AFTER UPDATE_PAYMENT_METHODS")
            }
        }
    }

    private val onAccountUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BroadcastConstants.ACTION_ON_ACCOUNT_UPDATE && waitingForConfirmAccount) {
                waitingForConfirmAccount = false
                visibleFragment = Constants.LOGIN_FRAGMENT
                showFragment(visibleFragment)
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)

        visibleFragment = intent?.getIntExtra(Constants.VISIBLE_FRAGMENT, Constants.LOGIN_FRAGMENT)
            ?: Constants.LOGIN_FRAGMENT

        if (visibleFragment == Constants.LOGIN_FRAGMENT) {
            loginFragment = LoginFragment()
        }

        showFragment(visibleFragment)
    }

    override fun onDestroy() {
        Timber.d("onDestroy")
        chatRequestHandler.setIsLoggingRunning(false)
        unregisterReceiver(updateMyAccountReceiver)
        unregisterReceiver(onAccountUpdateReceiver)
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("onCreate")
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        chatRequestHandler.setIsLoggingRunning(true)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        hideAB()

        if (savedInstanceState != null) {
            Timber.d("Bundle is NOT NULL")
            visibleFragment =
                savedInstanceState.getInt(Constants.VISIBLE_FRAGMENT, Constants.LOGIN_FRAGMENT)
        } else {
            visibleFragment =
                intent?.getIntExtra(Constants.VISIBLE_FRAGMENT, Constants.LOGIN_FRAGMENT)
                    ?: Constants.LOGIN_FRAGMENT
            Timber.d("There is an intent! VisibleFragment: %s", visibleFragment)
        }

        dbH.ephemeral?.let {
            visibleFragment = Constants.CONFIRM_EMAIL_FRAGMENT
            emailTemp = it.email
            passwdTemp = it.password
            sessionTemp = it.session
            firstNameTemp = it.firstName
            lastNameTemp = it.lastName
            megaApi.resumeCreateAccount(sessionTemp, this)
        }

        setupObservers()
        isBackFromLoginPage = false
        showFragment(visibleFragment)
    }

    private fun setupObservers() {
        registerReceiver(updateMyAccountReceiver,
            IntentFilter(Constants.BROADCAST_ACTION_INTENT_UPDATE_ACCOUNT_DETAILS))

        registerReceiver(onAccountUpdateReceiver,
            IntentFilter(BroadcastConstants.BROADCAST_ACTION_INTENT_ON_ACCOUNT_UPDATE).apply {
                addAction(BroadcastConstants.ACTION_ON_ACCOUNT_UPDATE)
            })

        registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (viewModel.intentAction == ACTION_FORCE_RELOAD_ACCOUNT) {
                    MegaApplication.isLoggingIn = false
                    finish()
                }
            }
        }, IntentFilter(ACTION_FETCH_NODES_FINISHED))
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home && visibleFragment == Constants.LOGIN_FRAGMENT
            && loginFragment?.isAdded == true
        ) {
            loginFragment?.returnToLogin()
            onBackPressedDispatcher.onBackPressed()
        }

        return super.onOptionsItemSelected(item)
    }

    /**
     * Shows a snackbar.
     *
     * @param message Message to show.
     */
    fun showSnackbar(message: String) = showSnackbar(binding.relativeContainerLogin, message)

    /**
     * Shows a fragment.
     *
     * @param visibleFragment The fragment to show.
     */
    fun showFragment(visibleFragment: Int) {
        Timber.d("visibleFragment: %s", visibleFragment)
        this.visibleFragment = visibleFragment
        restrictOrientation()

        when (visibleFragment) {
            Constants.LOGIN_FRAGMENT -> {
                Timber.d("Show LOGIN_FRAGMENT")
                if (loginFragment == null) {
                    loginFragment = LoginFragment()
                }

                if (passwdTemp != null && emailTemp != null) {
                    loginFragment?.emailTemp = emailTemp
                    loginFragment?.passwdTemp = passwdTemp
                }

                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container_login, loginFragment ?: return)
                    .commitNowAllowingStateLoss()

                Util.setDrawUnderStatusBar(this, false)
            }
            Constants.CREATE_ACCOUNT_FRAGMENT -> {
                Timber.d("Show CREATE_ACCOUNT_FRAGMENT")
                if (createAccountFragment == null || cancelledConfirmationProcess) {
                    createAccountFragment = CreateAccountFragment()
                    cancelledConfirmationProcess = false
                }

                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container_login, createAccountFragment ?: return)
                    .commitNowAllowingStateLoss()

                Util.setDrawUnderStatusBar(this, false)
            }
            Constants.TOUR_FRAGMENT -> {
                Timber.d("Show TOUR_FRAGMENT")
                tourFragment =
                    when {
                        Constants.ACTION_RESET_PASS == intent?.action -> {
                            TourFragment.newInstance(intent?.dataString, null)
                        }
                        Constants.ACTION_PARK_ACCOUNT == intent?.action -> {
                            TourFragment.newInstance(null, intent?.dataString)
                        }
                        else -> {
                            TourFragment.newInstance(null, null)
                        }
                    }
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container_login, tourFragment ?: return)
                    .commitNowAllowingStateLoss()

                Util.setDrawUnderStatusBar(this, true)
            }
            Constants.CONFIRM_EMAIL_FRAGMENT -> {
                val confirmEmailFragment = ConfirmEmailFragment()

                if (passwdTemp != null && emailTemp != null) {
                    confirmEmailFragment.emailTemp = emailTemp
                    confirmEmailFragment.passwdTemp = passwdTemp
                    confirmEmailFragment.firstNameTemp = firstNameTemp
                }

                with(supportFragmentManager) {
                    beginTransaction()
                        .replace(R.id.fragment_container_login, confirmEmailFragment)
                        .commitNowAllowingStateLoss()

                    executePendingTransactions()
                }

                Util.setDrawUnderStatusBar(this, false)
            }
        }

        if ((application as MegaApplication).isEsid) {
            showAlertLoggedOut()
        }
    }

    /**
     * Restrict to portrait mode always for mobile devices and tablets (already restricted via Manifest).
     * Allow the landscape mode only for tablets and only for TOUR_FRAGMENT.
     */
    @SuppressLint("SourceLockedOrientationActivity")
    private fun restrictOrientation() {
        if (Util.isTablet(this)) {
            requestedOrientation =
                if (visibleFragment == Constants.TOUR_FRAGMENT) {
                    Timber.d("Tablet landscape mode allowed")
                    ActivityInfo.SCREEN_ORIENTATION_FULL_USER
                } else {
                    Timber.d("Tablet landscape mode restricted")
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
        }
    }

    override fun shouldSetStatusBarTextColor() = false

    /**
     * Shows a warning informing the Recovery Key is not correct.
     */
    fun showAlertIncorrectRK() {
        Timber.d("showAlertIncorrectRK")
        MaterialAlertDialogBuilder(this)
            .setTitle(getFormattedStringOrDefault(R.string.incorrect_MK_title))
            .setMessage(getFormattedStringOrDefault(R.string.incorrect_MK))
            .setCancelable(false)
            .setPositiveButton(getFormattedStringOrDefault(R.string.general_ok), null)
            .show()
    }

    /**
     * Shows a warning informing the account has been logged out.
     */
    fun showAlertLoggedOut() {
        Timber.d("showAlertLoggedOut")
        (application as MegaApplication).isEsid = false

        if (!isFinishing) {
            MaterialAlertDialogBuilder(this)
                .setTitle(getFormattedStringOrDefault(R.string.title_alert_logged_out))
                .setMessage(getFormattedStringOrDefault(R.string.error_server_expired_session))
                .setPositiveButton(getFormattedStringOrDefault(R.string.general_ok), null)
                .show()
        }
    }

    /**
     * Shows a confirmation dialog for cancelling transfers.
     */
    fun showConfirmationCancelAllTransfers() {
        Timber.d("showConfirmationCancelAllTransfers")
        intent = null

        MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Mega_MaterialAlertDialog)
            .setMessage(getFormattedStringOrDefault(R.string.cancel_all_transfer_confirmation))
            .setPositiveButton(R.string.cancel_all_action) { _, _ ->
                Timber.d("Pressed button positive to cancel transfer")
                megaApi.cancelTransfers(MegaTransfer.TYPE_DOWNLOAD)
                megaApiFolder.cancelTransfers(MegaTransfer.TYPE_DOWNLOAD)
            }.setNegativeButton(R.string.general_dismiss, null)
            .show()
    }

    public override fun onResume() {
        Timber.d("onResume")
        super.onResume()
        Util.setAppFontSize(this)

        if (intent?.action != null) {
            when (intent.action) {
                Constants.ACTION_CANCEL_CAM_SYNC -> showCancelCUWarning()
                Constants.ACTION_CANCEL_DOWNLOAD -> showConfirmationCancelAllTransfers()
                Constants.ACTION_OVERQUOTA_TRANSFER -> showGeneralTransferOverQuotaWarning()
            }

            intent.action = null
        }

        intent = null
    }

    private fun showCancelCUWarning() {
        Timber.d("ACTION_CANCEL_CAM_SYNC")
        val title = getFormattedStringOrDefault(R.string.cam_sync_syncing)
        val text = getFormattedStringOrDefault(R.string.cam_sync_cancel_sync)

        Util.getCustomAlertBuilder(this, title, text, null)
            .setPositiveButton(getFormattedStringOrDefault(R.string.general_yes)
            ) { _, _ ->
                JobUtil.fireStopCameraUploadJob(this@LoginActivity)
                dbH.setCamSyncEnabled(false)
            }.setNegativeButton(getFormattedStringOrDefault(R.string.general_no), null)
            .show()
    }

    public override fun showConfirmationEnableLogsKarere() {
        loginFragment?.numberOfClicksKarere = 0
        super.showConfirmationEnableLogsKarere()
    }

    public override fun showConfirmationEnableLogsSDK() {
        loginFragment?.numberOfClicksSDK = 0
        super.showConfirmationEnableLogsSDK()
    }

    /**
     * Sets the received string as temporal email.
     *
     * @param emailTemp The temporal email.
     */
    fun setTemporalEmail(emailTemp: String?) {
        this.emailTemp = emailTemp
        val ephemeral = dbH.ephemeral ?: return

        ephemeral.email = emailTemp
        dbH.clearEphemeral()
        dbH.saveEphemeral(ephemeral)
    }

    override fun onRequestStart(api: MegaApiJava, request: MegaRequest) {
        Timber.d("onRequestStart - ${request.requestString}")
    }

    override fun onRequestUpdate(api: MegaApiJava, request: MegaRequest) {
        Timber.d("onRequestUpdate - ${request.requestString}")
    }

    override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) {
        Timber.d("onRequestFinish - ${request.requestString}_${e.errorCode}")

        if (request.type == MegaRequest.TYPE_CREATE_ACCOUNT) {
            try {
                if (request.paramType == 1) {
                    if (e.errorCode == MegaError.API_OK) {
                        waitingForConfirmAccount = true
                        visibleFragment = Constants.CONFIRM_EMAIL_FRAGMENT
                        showFragment(visibleFragment)
                    } else {
                        cancelConfirmationAccount()
                    }
                } // In case getParamType == 3 (creating ephemeral account ++) we don't need to trigger a fetch nodes (sdk performs internal)
                if (request.paramType == 4) {
                    if (e.errorCode == MegaError.API_OK) {
                        //Resuming ephemeral account ++, we need to trigger a fetch nodes
                        megaApi.fetchNodes()
                    }
                }
            } catch (exc: Exception) {
                Timber.e(exc)
            }
        }
    }

    /**
     * Cancels the account confirmation.
     */
    fun cancelConfirmationAccount() {
        Timber.d("cancelConfirmationAccount")
        dbH.clearEphemeral()
        dbH.clearCredentials()
        cancelledConfirmationProcess = true
        waitingForConfirmAccount = false
        passwdTemp = null
        emailTemp = null
        visibleFragment = Constants.TOUR_FRAGMENT
        showFragment(visibleFragment)
    }

    override fun onRequestTemporaryError(api: MegaApiJava, request: MegaRequest, e: MegaError) {
        Timber.w("onRequestTemporaryError - ${request.requestString}")
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        Timber.d("onSaveInstanceState")
        super.onSaveInstanceState(outState)
        outState.putInt(Constants.VISIBLE_FRAGMENT, visibleFragment)
    }

    /**
     * Shows the action bar.
     */
    fun showAB(tB: Toolbar?) {
        setSupportActionBar(tB)
        supportActionBar?.let {
            with(it) {
                show()
                setHomeButtonEnabled(true)
                setDisplayHomeAsUpEnabled(true)
            }
        }

        if (visibleFragment == Constants.LOGIN_FRAGMENT) {
            Util.setDrawUnderStatusBar(this, false)
        }
    }

    /**
     * Sets [OnKeyboardVisibilityListener].
     *
     * @param onKeyboardVisibilityListener The listener.
     */
    fun setKeyboardVisibilityListener(onKeyboardVisibilityListener: OnKeyboardVisibilityListener) {
        val parentView = (findViewById<View>(android.R.id.content) as ViewGroup).getChildAt(0)
        if (parentView == null) {
            Timber.w("Cannot set the keyboard visibility listener. Parent view is NULL.")
            return
        }

        parentView.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            private var alreadyOpen = false
            private val defaultKeyboardHeightDP = 100
            private val EstimatedKeyboardDP = defaultKeyboardHeightDP + 48
            private val rect = Rect()

            override fun onGlobalLayout() {
                val estimatedKeyboardHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    EstimatedKeyboardDP.toFloat(),
                    parentView.resources.displayMetrics).toInt()
                parentView.getWindowVisibleDisplayFrame(rect)

                val heightDiff = parentView.rootView.height - (rect.bottom - rect.top)
                val isShown = heightDiff >= estimatedKeyboardHeight

                if (isShown == alreadyOpen) {
                    return
                }
                alreadyOpen = isShown
                onKeyboardVisibilityListener.onVisibilityChanged(isShown)
            }
        })
    }

    /**
     * Hides the action bar.
     */
    fun hideAB() = supportActionBar?.hide()

    /**
     * Sets temporal data for account creation.
     *
     * @param email    Email.
     * @param name     First name.
     * @param lastName Last name.
     * @param password Password.
     */
    fun setTemporalDataForAccountCreation(
        email: String,
        name: String,
        lastName: String,
        password: String,
    ) {
        setTemporalEmail(email)
        firstNameTemp = name
        lastNameTemp = lastName
        passwdTemp = password
        waitingForConfirmAccount = true
    }

    companion object {

        /**
         * Flag for knowing if it was already in the login page.
         */
        @JvmField
        var isBackFromLoginPage = false

        /**
         * Intent action for showing the login fetching nodes.
         */
        const val ACTION_FORCE_RELOAD_ACCOUNT = "FORCE_RELOAD"

        /**
         * Intent action for notifying fetchNodes finished.
         */
        const val ACTION_FETCH_NODES_FINISHED = "FETCH_NODES_FINISHED"
    }
}