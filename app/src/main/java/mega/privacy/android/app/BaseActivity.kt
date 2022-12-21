package mega.privacy.android.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.SnackbarLayout
import com.jeremyliao.liveeventbus.LiveEventBus
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.disposables.CompositeDisposable
import kotlinx.coroutines.launch
import mega.privacy.android.app.activities.contract.NameCollisionActivityContract
import mega.privacy.android.app.activities.settingsActivities.FileManagementPreferencesActivity
import mega.privacy.android.app.components.saver.AutoPlayInfo
import mega.privacy.android.app.constants.BroadcastConstants
import mega.privacy.android.app.constants.EventConstants.EVENT_TRANSFER_OVER_QUOTA
import mega.privacy.android.app.globalmanagement.MyAccountInfo
import mega.privacy.android.app.globalmanagement.TransfersManagement
import mega.privacy.android.app.interfaces.ActivityLauncher
import mega.privacy.android.app.interfaces.PermissionRequester
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.listeners.ChatLogoutListener
import mega.privacy.android.app.logging.LegacyLoggingSettings
import mega.privacy.android.app.main.LoginActivity
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.meeting.activity.MeetingActivity
import mega.privacy.android.app.middlelayer.iab.BillingManager
import mega.privacy.android.app.middlelayer.iab.BillingManager.RequestCode
import mega.privacy.android.app.middlelayer.iab.BillingUpdatesListener
import mega.privacy.android.app.middlelayer.iab.MegaPurchase
import mega.privacy.android.app.middlelayer.iab.MegaSku
import mega.privacy.android.app.myAccount.MyAccountActivity
import mega.privacy.android.app.namecollision.data.NameCollision
import mega.privacy.android.app.psa.Psa
import mega.privacy.android.app.psa.PsaWebBrowser
import mega.privacy.android.app.service.iab.BillingManagerImpl
import mega.privacy.android.app.service.iar.RatingHandlerImpl
import mega.privacy.android.app.smsVerification.SMSVerificationActivity
import mega.privacy.android.app.snackbarListeners.SnackbarNavigateOption
import mega.privacy.android.app.upgradeAccount.PaymentActivity
import mega.privacy.android.app.upgradeAccount.UpgradeAccountActivity
import mega.privacy.android.app.usecase.exception.ForeignNodeException
import mega.privacy.android.app.usecase.exception.NotEnoughQuotaMegaException
import mega.privacy.android.app.usecase.exception.QuotaExceededMegaException
import mega.privacy.android.app.utils.AlertDialogUtil.dismissAlertDialogIfExists
import mega.privacy.android.app.utils.AlertDialogUtil.isAlertDialogShown
import mega.privacy.android.app.utils.AlertsAndWarnings.showForeignStorageOverQuotaWarningDialog
import mega.privacy.android.app.utils.AlertsAndWarnings.showResumeTransfersWarning
import mega.privacy.android.app.utils.ColorUtils.getColorHexString
import mega.privacy.android.app.utils.ColorUtils.setStatusBarTextColor
import mega.privacy.android.app.utils.Constants.ACCOUNT_NOT_BLOCKED
import mega.privacy.android.app.utils.Constants.ACTION_OVERQUOTA_STORAGE
import mega.privacy.android.app.utils.Constants.ACTION_PRE_OVERQUOTA_STORAGE
import mega.privacy.android.app.utils.Constants.ACTION_SHOW_UPGRADE_ACCOUNT
import mega.privacy.android.app.utils.Constants.BROADCAST_ACTION_INTENT_BUSINESS_EXPIRED
import mega.privacy.android.app.utils.Constants.BROADCAST_ACTION_INTENT_SIGNAL_PRESENCE
import mega.privacy.android.app.utils.Constants.BROADCAST_ACTION_INTENT_SSL_VERIFICATION_FAILED
import mega.privacy.android.app.utils.Constants.BUSINESS
import mega.privacy.android.app.utils.Constants.CHAT_ID
import mega.privacy.android.app.utils.Constants.DISABLED_BUSINESS_ACCOUNT_BLOCK
import mega.privacy.android.app.utils.Constants.DISMISS_ACTION_SNACKBAR
import mega.privacy.android.app.utils.Constants.EVENT_PSA
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import mega.privacy.android.app.utils.Constants.INVITE_CONTACT_TYPE
import mega.privacy.android.app.utils.Constants.LOGIN_FRAGMENT
import mega.privacy.android.app.utils.Constants.MESSAGE_SNACKBAR_TYPE
import mega.privacy.android.app.utils.Constants.MUTE_NOTIFICATIONS_SNACKBAR_TYPE
import mega.privacy.android.app.utils.Constants.NOT_SPACE_SNACKBAR_TYPE
import mega.privacy.android.app.utils.Constants.OPEN_FILE_SNACKBAR_TYPE
import mega.privacy.android.app.utils.Constants.PERMISSIONS_TYPE
import mega.privacy.android.app.utils.Constants.PRO_I
import mega.privacy.android.app.utils.Constants.PRO_II
import mega.privacy.android.app.utils.Constants.PRO_III
import mega.privacy.android.app.utils.Constants.PRO_LITE
import mega.privacy.android.app.utils.Constants.REMOVED_BUSINESS_ACCOUNT_BLOCK
import mega.privacy.android.app.utils.Constants.RESUME_TRANSFERS_TYPE
import mega.privacy.android.app.utils.Constants.SENT_REQUESTS_TYPE
import mega.privacy.android.app.utils.Constants.SMS_VERIFICATION_ACCOUNT_BLOCK
import mega.privacy.android.app.utils.Constants.SNACKBAR_TYPE
import mega.privacy.android.app.utils.Constants.TOS_COPYRIGHT_ACCOUNT_BLOCK
import mega.privacy.android.app.utils.Constants.TOS_NON_COPYRIGHT_ACCOUNT_BLOCK
import mega.privacy.android.app.utils.Constants.VISIBLE_FRAGMENT
import mega.privacy.android.app.utils.Constants.WEAK_PROTECTION_ACCOUNT_BLOCK
import mega.privacy.android.app.utils.MegaNodeUtil.autoPlayNode
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.billing.PaymentUtils.getSkuDetails
import mega.privacy.android.app.utils.billing.PaymentUtils.getSubscriptionRenewalType
import mega.privacy.android.app.utils.billing.PaymentUtils.getSubscriptionType
import mega.privacy.android.app.utils.billing.PaymentUtils.updateAccountInfo
import mega.privacy.android.app.utils.billing.PaymentUtils.updatePricing
import mega.privacy.android.app.utils.billing.PaymentUtils.updateSubscriptionLevel
import mega.privacy.android.app.utils.permission.PermissionUtils.requestPermission
import mega.privacy.android.app.utils.permission.PermissionUtils.toAppInfo
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.data.model.UserCredentials
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.data.qualifier.MegaApiFolder
import mega.privacy.android.domain.entity.LogsType
import mega.privacy.android.domain.entity.PurchaseType
import mega.privacy.android.domain.usecase.IsDatabaseEntryStale
import nz.mega.sdk.MegaAccountDetails
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaChatApi
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import timber.log.Timber
import javax.inject.Inject

/**
 * Base activity which includes common behaviors for several activities.
 *
 * @property megaApi                        [MegaApiAndroid]
 * @property megaApiFolder                  [MegaApiAndroid]
 * @property megaChatApi                    [MegaChatApiAndroid]
 * @property dbH                            [DatabaseHandler]
 * @property myAccountInfo                  [MyAccountInfo]
 * @property transfersManagement            [TransfersManagement]
 * @property loggingSettings                [LegacyLoggingSettings]
 * @property composite                      [CompositeDisposable]
 * @property nameCollisionActivityContract  [NameCollisionActivityContract]
 * @property app                            [MegaApplication]
 * @property outMetrics                     [DisplayMetrics]
 * @property isResumeTransfersWarningShown  True if the warning should be shown, false otherwise.
 * @property resumeTransfersWarning         [AlertDialog] for paused transfers.
 */
@AndroidEntryPoint
open class BaseActivity : AppCompatActivity(), ActivityLauncher, PermissionRequester,
    SnackbarShower, BillingUpdatesListener {

    @Inject
    @MegaApi
    lateinit var megaApi: MegaApiAndroid

    @Inject
    @MegaApiFolder
    lateinit var megaApiFolder: MegaApiAndroid

    @Inject
    lateinit var megaChatApi: MegaChatApiAndroid

    @Inject
    lateinit var dbH: LegacyDatabaseHandler

    @Inject
    lateinit var myAccountInfo: MyAccountInfo

    @Inject
    lateinit var transfersManagement: TransfersManagement

    @Inject
    lateinit var loggingSettings: LegacyLoggingSettings

    @Inject
    lateinit var isDatabaseEntryStale: IsDatabaseEntryStale

    @JvmField
    var composite = CompositeDisposable()

    @JvmField
    var nameCollisionActivityContract: ActivityResultLauncher<ArrayList<NameCollision>>? = null
    private var billingManager: BillingManager? = null
    private var skuDetailsList: List<MegaSku>? = null

    @JvmField
    protected var app: MegaApplication? = MegaApplication.getInstance()
    private var sslErrorDialog: AlertDialog? = null
    private var delaySignalPresence = false
    private var isGeneralTransferOverQuotaWarningShown = false
    private var transferGeneralOverQuotaWarning: AlertDialog? = null
    private var setDownloadLocationDialog: AlertDialog? = null
    private var confirmationChecked = false
    private var downloadLocation: String? = null
    private var upgradeAlert: AlertDialog? = null
    private var purchaseType: PurchaseType? = null
    private var expiredBusinessAlert: AlertDialog? = null
    private var isExpiredBusinessAlertShown = false
    private var psaWebBrowserContainer: FrameLayout? = null
    private val uiHandler = Handler(Looper.getMainLooper())

    protected val outMetrics: DisplayMetrics by lazy { resources.displayMetrics }
    var isResumeTransfersWarningShown = false
    var resumeTransfersWarning: AlertDialog? = null

    //Indicates when the activity should finish due to some error
    private var finishActivityAtError = false

    /**
     * True if is a business account and is expired.
     */
    protected val isBusinessExpired: Boolean
        get() = megaApi.isBusinessAccount &&
                myAccountInfo.accountType == BUSINESS &&
                megaApi.businessStatus == MegaApiJava.BUSINESS_STATUS_EXPIRED

    /**
     * Get snackbar instance
     *
     * @return snackbar
     */
    var snackbar: Snackbar? = null
        private set

    /**
     * Contains the info of a node that to be opened in-app.
     */
    private var autoPlayInfo: AutoPlayInfo? = null

    /**
     * Load the psa in the web browser fragment if the psa is a web one and this activity
     * is on the top of the task stack
     */
    private val psaObserver = Observer { psa: Psa ->
        if (psa.url != null && Util.isTopActivity(javaClass.name, this)) {
            loadPsaInWebBrowser(psa)
        }
    }

    /**
     * Checks if the current activity is in background.
     *
     * @return True if the current activity is in background, false otherwise.
     */
    protected var isActivityInBackground = false
        private set

    /**
     * Checks if the current activity is in foreground.
     *
     * @return True if the current activity is in foreground, false otherwise.
     */
    protected val isActivityInForeground: Boolean
        get() = !isActivityInBackground

    /**
     * Every sub-class activity has an embedded PsaWebBrowser fragment, either visible or invisible.
     * In order to show the newly retrieved PSA at any occasion
     */
    @JvmField
    protected var psaWebBrowser: PsaWebBrowser? = null

    /**
     * Broadcast receiver to manage the errors shown and actions when an account is blocked.
     */
    private val accountBlockedReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != BroadcastConstants.BROADCAST_ACTION_INTENT_EVENT_ACCOUNT_BLOCKED)
                return

            checkWhyAmIBlocked(
                intent.getLongExtra(BroadcastConstants.EVENT_NUMBER, INVALID_VALUE.toLong()),
                intent.getStringExtra(BroadcastConstants.EVENT_TEXT))
        }
    }

    /**
     * Broadcast receiver to manage a possible SSL verification error.
     */
    private val sslErrorReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Timber.d("BROADCAST TO MANAGE A SSL VERIFICATION ERROR")
            if (!(sslErrorDialog ?: return).isShowing) {
                showSSLErrorDialog()
            }
        }
    }

    /**
     * Broadcast to send presence after first launch of app
     */
    private val signalPresenceReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Timber.d("BROADCAST TO SEND SIGNAL PRESENCE")
            if (delaySignalPresence && megaChatApi.presenceConfig != null && !megaChatApi.presenceConfig.isPending) {
                delaySignalPresence = false
                retryConnectionsAndSignalPresence()
            }
        }
    }

    /**
     * Broadcast to show an alert informing about the current business account has expired.
     */
    private val businessExpiredReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            showExpiredBusinessAlert()
        }
    }

    /**
     * Broadcast to show taken down files info
     */
    private val takenDownFilesReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            Timber.d("BROADCAST INFORM THERE ARE TAKEN DOWN FILES IMPLIED IN ACTION")
            val numberFiles = intent.getIntExtra(BroadcastConstants.NUMBER_FILES, 1)
            Util.showSnackbar(this@BaseActivity,
                resources.getQuantityString(R.plurals.alert_taken_down_files,
                    numberFiles,
                    numberFiles))
        }
    }

    /**
     * Broadcast to show a snackbar when all the transfers finish
     */
    private val transferFinishedReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null || isActivityInBackground) {
                return
            }
            if (intent.getBooleanExtra(BroadcastConstants.FILE_EXPLORER_CHAT_UPLOAD, false)) {
                Util.showSnackbar(
                    this@BaseActivity,
                    MESSAGE_SNACKBAR_TYPE,
                    null,
                    intent.getLongExtra(CHAT_ID, MEGACHAT_INVALID_HANDLE)
                )
                return
            }
            val numTransfers = intent.getIntExtra(BroadcastConstants.NUMBER_FILES, 1)
            var message: String
            message = if (intent.getBooleanExtra(BroadcastConstants.OFFLINE_AVAILABLE, false)) {
                StringResourcesUtils.getString(R.string.file_available_offline)
            } else {
                resources.getQuantityString(
                    R.plurals.download_finish, numTransfers, numTransfers)
            }
            when (intent.getStringExtra(BroadcastConstants.TRANSFER_TYPE)) {
                BroadcastConstants.DOWNLOAD_TRANSFER -> Util.showSnackbar(this@BaseActivity,
                    message)
                BroadcastConstants.UPLOAD_TRANSFER -> {
                    message = resources.getQuantityString(R.plurals.upload_finish,
                        numTransfers,
                        numTransfers)
                    Util.showSnackbar(this@BaseActivity, message)
                }
                BroadcastConstants.DOWNLOAD_TRANSFER_OPEN -> {
                    autoPlayInfo = AutoPlayInfo(
                        intent.getStringExtra(BroadcastConstants.NODE_NAME) ?: return,
                        intent.getLongExtra(BroadcastConstants.NODE_HANDLE, INVALID_VALUE.toLong()),
                        intent.getStringExtra(BroadcastConstants.NODE_LOCAL_PATH) ?: return,
                        true
                    )

                    showSnackbar(OPEN_FILE_SNACKBAR_TYPE, message, MEGACHAT_INVALID_HANDLE)
                }
                BroadcastConstants.DOWNLOAD_MS_FILE_AND_OPEN -> {
                    //If the file is Microsoft file, open the file directly after downloaded
                    autoPlayInfo = AutoPlayInfo(
                        intent.getStringExtra(BroadcastConstants.NODE_NAME) ?: return,
                        intent.getLongExtra(BroadcastConstants.NODE_HANDLE,
                            INVALID_VALUE.toLong()),
                        intent.getStringExtra(BroadcastConstants.NODE_LOCAL_PATH) ?: return,
                        true
                    )
                    openDownloadedFile()
                }
            }
        }
    }

    /**
     * Broadcast to show a Snackbar with the received text.
     */
    private val showSnackbarReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (isActivityInBackground || intent == null || intent.action == null || intent.action != BroadcastConstants.BROADCAST_ACTION_SHOW_SNACKBAR
            ) return
            val message = intent.getStringExtra(BroadcastConstants.SNACKBAR_TEXT)
            if (!TextUtil.isTextEmpty(message)) {
                Util.showSnackbar(this@BaseActivity, message)
            }
        }
    }

    /**
     * Broadcast to show a warning when it tries to upload files to a chat conversation
     * and the transfers are paused.
     */
    private val resumeTransfersReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != BroadcastConstants.BROADCAST_ACTION_RESUME_TRANSFERS
                || isResumeTransfersWarningShown || isActivityInBackground
            ) return

            transfersManagement.hasResumeTransfersWarningAlreadyBeenShown = true
            showResumeTransfersWarning(this@BaseActivity)
        }
    }

    /**
     * Broadcast to show a snackbar when the Cookie settings has been saved
     */
    protected var cookieSettingsReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (isActivityInBackground || intent == null) {
                return
            }
            val view = window.decorView.findViewById<View>(android.R.id.content)
            if (view != null) {
                showSnackbar(view,
                    StringResourcesUtils.getString(R.string.dialog_cookie_snackbar_saved))
            }
        }
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            handleGoBack()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        nameCollisionActivityContract =
            registerForActivityResult(NameCollisionActivityContract()) { result: String? ->
                if (result != null) {
                    showSnackbar(SNACKBAR_TYPE, result, MEGACHAT_INVALID_HANDLE)
                }
            }

        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        registerReceiver(sslErrorReceiver,
            IntentFilter(BROADCAST_ACTION_INTENT_SSL_VERIFICATION_FAILED))

        registerReceiver(signalPresenceReceiver,
            IntentFilter(BROADCAST_ACTION_INTENT_SIGNAL_PRESENCE))

        registerReceiver(accountBlockedReceiver,
            IntentFilter(BroadcastConstants.BROADCAST_ACTION_INTENT_EVENT_ACCOUNT_BLOCKED))

        registerReceiver(businessExpiredReceiver,
            IntentFilter(BROADCAST_ACTION_INTENT_BUSINESS_EXPIRED))

        registerReceiver(takenDownFilesReceiver,
            IntentFilter(BroadcastConstants.BROADCAST_ACTION_INTENT_TAKEN_DOWN_FILES))

        registerReceiver(transferFinishedReceiver,
            IntentFilter(BroadcastConstants.BROADCAST_ACTION_INTENT_SHOWSNACKBAR_TRANSFERS_FINISHED))

        registerReceiver(showSnackbarReceiver,
            IntentFilter(BroadcastConstants.BROADCAST_ACTION_SHOW_SNACKBAR))

        registerReceiver(resumeTransfersReceiver,
            IntentFilter(BroadcastConstants.BROADCAST_ACTION_RESUME_TRANSFERS))

        registerReceiver(cookieSettingsReceiver,
            IntentFilter(BroadcastConstants.BROADCAST_ACTION_COOKIE_SETTINGS_SAVED))

        LiveEventBus.get(EVENT_TRANSFER_OVER_QUOTA, Boolean::class.java)
            .observe(this) { showGeneralTransferOverQuotaWarning() }

        LiveEventBus.get(EVENT_PURCHASES_UPDATED).observe(this) { type: Any? ->
            when {
                this is PaymentActivity || this is UpgradeAccountActivity -> finish()
                this is MyAccountActivity && myAccountInfo.isUpgradeFromAccount()
                        || this is ManagerActivity && myAccountInfo.isUpgradeFromManager()
                        || this is FileManagementPreferencesActivity && myAccountInfo.isUpgradeFromSettings() -> {
                    purchaseType = type as PurchaseType?
                    showQueryPurchasesResult()
                }
            }
        }

        savedInstanceState?.apply {
            isExpiredBusinessAlertShown =
                getBoolean(EXPIRED_BUSINESS_ALERT_SHOWN, false)

            if (isExpiredBusinessAlertShown) {
                showExpiredBusinessAlert()
            }

            isGeneralTransferOverQuotaWarningShown = getBoolean(
                TRANSFER_OVER_QUOTA_WARNING_SHOWN, false)

            if (isGeneralTransferOverQuotaWarningShown) {
                showGeneralTransferOverQuotaWarning()
            }

            isResumeTransfersWarningShown = getBoolean(
                RESUME_TRANSFERS_WARNING_SHOWN, false)

            if (isResumeTransfersWarningShown) {
                showResumeTransfersWarning(this@BaseActivity)
            }

            if (getBoolean(SET_DOWNLOAD_LOCATION_SHOWN, false)) {
                confirmationChecked = getBoolean(IS_CONFIRMATION_CHECKED, false)
                showConfirmationSaveInSameLocation(getString(DOWNLOAD_LOCATION))
            }

            if (getBoolean(UPGRADE_ALERT_SHOWN, false)) {
                purchaseType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    getSerializable(PURCHASE_TYPE, PurchaseType::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    getSerializable(PURCHASE_TYPE)
                } as PurchaseType?

                showQueryPurchasesResult()
            }
        }

        // Add an invisible full screen Psa web browser fragment to the activity.
        // Then show or hide it for browsing the PSA.
        addPsaWebBrowser()

        LiveEventBus.get(EVENT_PSA, Psa::class.java).observeStickyForever(psaObserver)

        if (shouldSetStatusBarTextColor()) {
            setStatusBarTextColor(this)
        }
    }

    /**
     * Checks if should set status bar text color.
     *
     * @return True if should set the color, false otherwise.
     */
    protected open fun shouldSetStatusBarTextColor(): Boolean = true

    /**
     * Create a fragment container and the web browser fragment, add them to the activity
     */
    private fun addPsaWebBrowser() {
        // Execute after the sub-class activity finish its setContentView()
        uiHandler.post {
            psaWebBrowserContainer = FrameLayout(this@BaseActivity)
            (psaWebBrowserContainer ?: return@post).id = R.id.psa_web_browser_container

            findViewById<ViewGroup>(android.R.id.content)
                .addView(psaWebBrowserContainer,
                    ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT))

            psaWebBrowser = PsaWebBrowser()

            // Don't put the fragment to the back stack. Since pressing back key just hide it,
            // never pop it up. onBackPressed() will let PSA browser to consume the back
            // key event anyway
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.psa_web_browser_container, psaWebBrowser ?: return@post)
                .commitNowAllowingStateLoss()
        }
    }

    /**
     * Display the new url PSA in the web browser
     *
     * @param psa the psa to display
     */
    private fun loadPsaInWebBrowser(psa: Psa) {
        (psaWebBrowser ?: return).loadPsa(psa.url ?: return, psa.id)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.apply {
            putBoolean(EXPIRED_BUSINESS_ALERT_SHOWN, isExpiredBusinessAlertShown)
            putBoolean(TRANSFER_OVER_QUOTA_WARNING_SHOWN, isGeneralTransferOverQuotaWarningShown)
            putBoolean(RESUME_TRANSFERS_WARNING_SHOWN, isResumeTransfersWarningShown)
            putBoolean(SET_DOWNLOAD_LOCATION_SHOWN, isAlertDialogShown(setDownloadLocationDialog))
            putBoolean(IS_CONFIRMATION_CHECKED, confirmationChecked)
            putString(DOWNLOAD_LOCATION, downloadLocation)
            putBoolean(UPGRADE_ALERT_SHOWN, isAlertDialogShown(upgradeAlert))
            putSerializable(PURCHASE_TYPE, purchaseType)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onPause() {
        isActivityInBackground = true
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        Util.setAppFontSize(this)
        isActivityInBackground = false
        retryConnectionsAndSignalPresence()
    }

    override fun onDestroy() {
        composite.clear()
        unregisterReceiver(sslErrorReceiver)
        unregisterReceiver(signalPresenceReceiver)
        unregisterReceiver(accountBlockedReceiver)
        unregisterReceiver(businessExpiredReceiver)
        unregisterReceiver(takenDownFilesReceiver)
        unregisterReceiver(transferFinishedReceiver)
        unregisterReceiver(showSnackbarReceiver)
        unregisterReceiver(resumeTransfersReceiver)
        unregisterReceiver(cookieSettingsReceiver)
        dismissAlertDialogIfExists(transferGeneralOverQuotaWarning)
        dismissAlertDialogIfExists(transferGeneralOverQuotaWarning)
        dismissAlertDialogIfExists(resumeTransfersWarning)
        dismissAlertDialogIfExists(setDownloadLocationDialog)
        dismissAlertDialogIfExists(upgradeAlert)
        LiveEventBus.get(EVENT_PSA, Psa::class.java).removeObserver(psaObserver)
        super.onDestroy()
    }

    /**
     * Open the downloaded file.
     */
    private fun openDownloadedFile() {
        autoPlayNode(
            this@BaseActivity,
            autoPlayInfo ?: return,
            this@BaseActivity,
            this@BaseActivity
        )

        autoPlayInfo = null
    }

    /**
     * Method to display an alert dialog indicating that the MEGA SSL key
     * can't be verified (API_ESSL Error) and giving the user several options.
     */
    private fun showSSLErrorDialog() {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val v = inflater.inflate(R.layout.dialog_three_vertical_buttons, null)
        builder.setView(v)
        val title = v.findViewById<TextView>(R.id.dialog_title)
        val text = v.findViewById<TextView>(R.id.dialog_text)
        val retryButton = v.findViewById<Button>(R.id.dialog_first_button)
        val openBrowserButton = v.findViewById<Button>(R.id.dialog_second_button)
        val dismissButton = v.findViewById<Button>(R.id.dialog_third_button)
        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT)
        params.gravity = Gravity.RIGHT
        title.setText(R.string.ssl_error_dialog_title)
        text.setText(R.string.ssl_error_dialog_text)
        retryButton.setText(R.string.general_retry)
        openBrowserButton.setText(R.string.general_open_browser)
        dismissButton.setText(R.string.general_dismiss)

        sslErrorDialog = builder.create().apply {
            setCancelable(false)
            setCanceledOnTouchOutside(false)

            retryButton.setOnClickListener {
                dismiss()
                megaApi.reconnect()
                megaApiFolder.reconnect()
            }

            openBrowserButton.setOnClickListener {
                dismiss()
                val uriUrl = Uri.parse("https://mega.nz/")
                val launchBrowser = Intent(Intent.ACTION_VIEW, uriUrl)
                startActivity(launchBrowser)
            }

            dismissButton.setOnClickListener {
                dismiss()
                megaApi.setPublicKeyPinning(false)
                megaApi.reconnect()
                megaApiFolder.setPublicKeyPinning(false)
                megaApiFolder.reconnect()
            }

            show()
        }
    }

    /**
     * Retry pending connections and signal presence.
     */
    protected fun retryConnectionsAndSignalPresence() {
        Timber.d("retryConnectionsAndSignalPresence")
        try {
            megaApi.retryPendingConnections()
            megaChatApi.retryPendingConnections(false, null)

            if (megaChatApi.presenceConfig != null && !megaChatApi.presenceConfig.isPending) {
                delaySignalPresence = false

                if (this !is MeetingActivity && megaChatApi.isSignalActivityRequired) {
                    Timber.d("Send signal presence")
                    megaChatApi.signalPresenceActivity()
                }
            } else {
                delaySignalPresence = true
            }
        } catch (e: Exception) {
            Timber.w(e, "Exception")
        }
    }

    private fun handleGoBack() {
        retryConnectionsAndSignalPresence()
        // we should disable and enable again in case BaseActivity has many fragments it should listen onBackPressed again
        onBackPressedCallback.isEnabled = false
        super.onBackPressed()
        onBackPressedCallback.isEnabled = true
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            retryConnectionsAndSignalPresence()
        }
        return super.dispatchTouchEvent(event)
    }

    /**
     * Method to display a simple Snackbar.
     *
     * @param view Layout where the snackbar is going to show.
     * @param s    Text to shown in the snackbar
     */
    open fun showSnackbar(view: View, s: String) {
        showSnackbar(type = SNACKBAR_TYPE, view = view, s = s)
    }

    /**
     * Method to display a simple or action Snackbar.
     *
     * @param type   There are three possible values to this param:
     *                - SNACKBAR_TYPE: creates a simple snackbar
     *                - MESSAGE_SNACKBAR_TYPE: creates an action snackbar which function is to go to Chat section
     *                - NOT_SPACE_SNACKBAR_TYPE: creates an action snackbar which function is to go to Storage-Settings section
     * @param view   Layout where the snackbar is going to show.
     * @param s      Text to shown in the snackbar
     * @param idChat Chat ID. If this param has a valid value the function of MESSAGE_SNACKBAR_TYPE ends in the specified chat.
     *               If the value is -1 (MEGACHAT_INVALID_HANDLE) the function ends in chats list view.
     */
    @JvmOverloads
    fun showSnackbar(
        type: Int,
        view: View,
        s: String?,
        idChat: Long = MEGACHAT_INVALID_HANDLE,
    ) {
        showSnackbar(type = type, view = view, s = s, idChat = idChat, userEmail = null)
    }

    /**
     * Method to display a simple or action Snackbar.
     *
     * @param type   There are three possible values to this param:
     *                  - SNACKBAR_TYPE: creates a simple snackbar
     *                  - MESSAGE_SNACKBAR_TYPE: creates an action snackbar which function is to go to Chat section
     *                  - NOT_SPACE_SNACKBAR_TYPE: creates an action snackbar which function is to go to Storage-Settings section
     *                  - MUTE_NOTIFICATIONS_SNACKBAR_TYPE: creates an action snackbar which function is unmute chats notifications
     *                  - INVITE_CONTACT_TYPE: creates an action snackbar which function is to send a contact invitation
     * @param view   Layout where the snackbar is going to show.
     * @param anchor Sets the view the Snackbar should be anchored above, null as default
     * @param s      Text to shown in the snackbar
     * @param idChat Chat ID. If this param has a valid value the function of MESSAGE_SNACKBAR_TYPE ends in the specified chat.
     *               If the value is -1 (MEGACHAT_INVALID_HANDLE) the function ends in chats list view.
     */
    fun showSnackbarWithAnchorView(
        type: Int,
        view: View,
        anchor: View?,
        s: String?,
        idChat: Long = MEGACHAT_INVALID_HANDLE,
    ) {
        showSnackbar(type = type, view = view, anchor = anchor, s = s, idChat = idChat)
    }

    /**
     * Method to display a simple or action Snackbar.
     *
     * @param type      There are three possible values to this param:
     *                  - SNACKBAR_TYPE: creates a simple snackbar
     *                  - MESSAGE_SNACKBAR_TYPE: creates an action snackbar which function is to go to Chat section
     *                  - NOT_SPACE_SNACKBAR_TYPE: creates an action snackbar which function is to go to Storage-Settings section
     *                  - MUTE_NOTIFICATIONS_SNACKBAR_TYPE: creates an action snackbar which function is unmute chats notifications
     *                  - INVITE_CONTACT_TYPE: creates an action snackbar which function is to send a contact invitation
     * @param view      Layout where the snackbar is going to show.
     * @param anchor    Sets the view the Snackbar should be anchored above, null as default
     * @param s         Text to shown in the snackbar
     * @param idChat    Chat ID. If this param has a valid value the function of MESSAGE_SNACKBAR_TYPE ends in the specified chat.
     *                  If the value is -1 (MEGACHAT_INVALID_HANDLE) the function ends in chats list view.
     * @param userEmail Email of the user to be invited.
     */
    fun showSnackbar(
        type: Int,
        view: View,
        anchor: View? = null,
        s: String?,
        idChat: Long = MEGACHAT_INVALID_HANDLE,
        userEmail: String? = null,
    ) {
        Timber.d("Show snackbar: %s", s)
        snackbar = try {
            when (type) {
                MESSAGE_SNACKBAR_TYPE -> Snackbar.make(view,
                    (if (s?.isNotEmpty() == true) s else StringResourcesUtils.getString(R.string.sent_as_message))
                        ?: return,
                    Snackbar.LENGTH_LONG)
                NOT_SPACE_SNACKBAR_TYPE -> Snackbar.make(view,
                    R.string.error_not_enough_free_space,
                    Snackbar.LENGTH_LONG)
                MUTE_NOTIFICATIONS_SNACKBAR_TYPE -> Snackbar.make(view,
                    R.string.notifications_are_already_muted,
                    Snackbar.LENGTH_LONG)
                DISMISS_ACTION_SNACKBAR -> Snackbar.make(view,
                    s ?: return,
                    Snackbar.LENGTH_INDEFINITE)
                else -> Snackbar.make(view, s ?: return, Snackbar.LENGTH_LONG)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error showing snackbar")
            return
        }

        snackbar?.apply {
            val snackbarLayout = this.view as SnackbarLayout
            snackbarLayout.setBackgroundResource(R.drawable.background_snackbar)

            if (anchor != null) {
                anchorView = anchor
            }

            when (type) {
                SNACKBAR_TYPE -> {
                    val snackbarTextView =
                        snackbarLayout.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
                    snackbarTextView.maxLines = 5
                    show()
                }
                MESSAGE_SNACKBAR_TYPE -> {
                    setAction(R.string.action_see, SnackbarNavigateOption(view.context, idChat))
                    show()
                }
                NOT_SPACE_SNACKBAR_TYPE -> {
                    setAction(R.string.action_settings, SnackbarNavigateOption(view.context))
                    show()
                }
                MUTE_NOTIFICATIONS_SNACKBAR_TYPE -> {
                    setAction(R.string.general_unmute, SnackbarNavigateOption(view.context, type))
                    show()
                }
                PERMISSIONS_TYPE -> {
                    val snackbarTextView =
                        snackbarLayout.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
                    snackbarTextView.maxLines = 3
                    setAction(R.string.action_settings, toAppInfo(applicationContext))
                    show()
                }
                INVITE_CONTACT_TYPE -> {
                    setAction(R.string.contact_invite, SnackbarNavigateOption(view.context, type, userEmail))
                    show()
                }
                DISMISS_ACTION_SNACKBAR -> {
                    val snackbarTextView =
                        snackbarLayout.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
                    snackbarTextView.maxLines = 5
                    setAction(R.string.general_ok, SnackbarNavigateOption(view.context, type))
                    show()
                }
                OPEN_FILE_SNACKBAR_TYPE -> {
                    setAction(StringResourcesUtils.getString(R.string.general_confirmation_open)) { openDownloadedFile() }
                    show()
                }
                SENT_REQUESTS_TYPE -> {
                    setAction(R.string.tab_sent_requests,
                        SnackbarNavigateOption(view.context, type, userEmail))
                    show()
                }
                RESUME_TRANSFERS_TYPE -> {
                    setAction(R.string.button_resume_individual_transfer,
                        SnackbarNavigateOption(view.context, type))
                    show()
                }
            }
        }
    }

    /**
     * Method to refresh the account details info if necessary.
     */
    protected fun refreshAccountInfo() {
        Timber.d("refreshAccountInfo")

        //Check if the call is recently
        Timber.d("Check the last call to getAccountDetails")
        lifecycleScope.launch {
            if (isDatabaseEntryStale()) {
                Timber.d("megaApi.getAccountDetails SEND")
                app?.askForAccountDetails()
            }
        }
    }

    /**
     * This method is shown in a business account when the account is expired.
     * It informs that all the actions are only read.
     * The message is different depending if the account belongs to an admin or an user.
     */
    protected fun showExpiredBusinessAlert() {
        if (isActivityInBackground || expiredBusinessAlert != null && expiredBusinessAlert?.isShowing == true) {
            return
        }

        val builder =
            MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Mega_MaterialAlertDialog)
                .apply {
                    setTitle(R.string.expired_business_title)

                    if (megaApi.isMasterBusinessAccount) {
                        setMessage(R.string.expired_admin_business_text)
                    } else {
                        var expiredString =
                            StringResourcesUtils.getString(R.string.expired_user_business_text)
                        try {
                            expiredString = expiredString.replace("[B]", "<b><font color=\'"
                                    + getColorHexString(this@BaseActivity, R.color.black_white)
                                    + "\'>")
                            expiredString = expiredString.replace("[/B]", "</font></b>")
                        } catch (e: Exception) {
                            Timber.w(e, "Exception formatting string")
                        }
                        setMessage(TextUtils.concat(HtmlCompat.fromHtml(expiredString,
                            HtmlCompat.FROM_HTML_MODE_LEGACY),
                            "\n\n${StringResourcesUtils.getString(R.string.expired_user_business_text_2)}"))
                    }

                    setNegativeButton(R.string.general_dismiss) { dialog, _ ->
                        isExpiredBusinessAlertShown = false

                        if (finishActivityAtError) {
                            finishActivityAtError = false
                            finish()
                        }

                        dialog.dismiss()
                    }

                    setCancelable(false)
                }

        expiredBusinessAlert = builder.create()
        expiredBusinessAlert?.show()
        isExpiredBusinessAlertShown = true
    }

    /**
     * Method to show an alert or error when the account has been suspended
     * for any reason
     *
     * @param eventNumber long that determines the event for which the account has been suspended
     * @param stringError string shown as an alert in case there is not any specific action for the event
     */
    fun checkWhyAmIBlocked(eventNumber: Long, stringError: String?) {

        val intent: Intent
        when (eventNumber.toString()) {
            ACCOUNT_NOT_BLOCKED -> {}
            TOS_COPYRIGHT_ACCOUNT_BLOCK -> megaChatApi.logout(ChatLogoutListener(this,
                StringResourcesUtils.getString(R.string.dialog_account_suspended_ToS_copyright_message),
                loggingSettings))
            TOS_NON_COPYRIGHT_ACCOUNT_BLOCK -> megaChatApi.logout(ChatLogoutListener(
                this,
                StringResourcesUtils.getString(R.string.dialog_account_suspended_ToS_non_copyright_message),
                loggingSettings))
            DISABLED_BUSINESS_ACCOUNT_BLOCK -> megaChatApi.logout(ChatLogoutListener(
                this,
                StringResourcesUtils.getString(R.string.error_business_disabled),
                loggingSettings))
            REMOVED_BUSINESS_ACCOUNT_BLOCK -> megaChatApi.logout(ChatLogoutListener(this,
                StringResourcesUtils.getString(R.string.error_business_removed),
                loggingSettings))
            SMS_VERIFICATION_ACCOUNT_BLOCK -> {
                if (megaApi.smsAllowedState() == 0 || MegaApplication.isVerifySMSShowed) return
                MegaApplication.smsVerifyShowed(true)
                val gSession = megaApi.dumpSession()
                //For first login, keep the valid session,
                //after added phone number, the account can use this session to fastLogin
                if (gSession != null) {
                    val myUser = megaApi.myUser
                    var myUserHandle: String? = null
                    var lastEmail: String? = null
                    if (myUser != null) {
                        lastEmail = myUser.email
                        myUserHandle = myUser.handle.toString() + ""
                    }
                    val credentials = UserCredentials(lastEmail, gSession, "", "", myUserHandle)
                    dbH.saveCredentials(credentials)
                }
                Timber.d("Show SMS verification activity.")
                intent = Intent(applicationContext, SMSVerificationActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                intent.putExtra(NAME_USER_LOCKED, true)
                startActivity(intent)
            }
            WEAK_PROTECTION_ACCOUNT_BLOCK -> {
                if (!MegaApplication.isBlockedDueToWeakAccount && !MegaApplication.isWebOpenDueToEmailVerification) {
                    startActivity(Intent(this, WeakAccountProtectionAlertActivity::class.java))
                }
            }
            else -> Util.showErrorAlertDialog(stringError, false, this)
        }
    }

    /**
     * Sets if should finish the activity because of some error.
     */
    protected fun setFinishActivityAtError(finishActivityAtError: Boolean) {
        this.finishActivityAtError = finishActivityAtError
    }

    /**
     * Shows a dialog to confirm enable the SDK logs.
     */
    protected open fun showConfirmationEnableLogsSDK() {
        showConfirmationEnableLogs(LogsType.SDK_LOGS)
    }

    /**
     * Shows a dialog to confirm enable the Karere logs.
     */
    protected open fun showConfirmationEnableLogsKarere() {
        showConfirmationEnableLogs(LogsType.MEGA_CHAT_LOGS)
    }

    /**
     * Shows a dialog to confirm enable the SDK or Karere logs.
     *
     * @param logsType SDK_LOGS to confirm enable the SDK logs,
     * KARERE_LOGS to confirm enable the Karere logs.
     */
    protected fun showConfirmationEnableLogs(logsType: LogsType?) {
        MaterialAlertDialogBuilder(this)
            .setMessage(R.string.enable_log_text_dialog)
            .setPositiveButton(R.string.general_enable) { _: DialogInterface?, _: Int ->
                when (logsType) {
                    LogsType.SDK_LOGS -> loggingSettings.setStatusLoggerSDK(this, true)
                    LogsType.MEGA_CHAT_LOGS -> loggingSettings.setStatusLoggerKarere(this, true)
                    else -> {}
                }
            }
            .setNegativeButton(R.string.general_cancel, null)
            .show()
            .setCanceledOnTouchOutside(false)
    }

    /**
     * Shows a warning indicating transfer over quota occurred.
     */
    fun showGeneralTransferOverQuotaWarning() {
        if (isActivityInBackground || transfersManagement.isOnTransfersSection || transferGeneralOverQuotaWarning != null) return
        val builder =
            MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Mega_MaterialAlertDialog)
        val dialogView = this.layoutInflater.inflate(R.layout.transfer_overquota_layout, null)
        builder.setView(dialogView)
            .setOnDismissListener {
                isGeneralTransferOverQuotaWarningShown = false
                transferGeneralOverQuotaWarning = null
                transfersManagement.resetTransferOverQuotaTimestamp()
            }
            .setCancelable(false)
        transferGeneralOverQuotaWarning = builder.create()
        transferGeneralOverQuotaWarning?.setCanceledOnTouchOutside(false)
        val text = dialogView.findViewById<TextView>(R.id.text_transfer_overquota)
        val stringResource =
            if (transfersManagement.isCurrentTransferOverQuota) R.string.current_text_depleted_transfer_overquota else R.string.text_depleted_transfer_overquota
        text.text = StringResourcesUtils.getString(stringResource, TimeUtils.getHumanizedTime(
            megaApi.bandwidthOverquotaDelay))
        val paymentButton = dialogView.findViewById<Button>(R.id.transfer_overquota_button_payment)
        val isLoggedIn = megaApi.isLoggedIn != 0 && dbH.credentials != null
        if (isLoggedIn) {
            val isFreeAccount = myAccountInfo.accountType == MegaAccountDetails.ACCOUNT_TYPE_FREE
            paymentButton.text =
                StringResourcesUtils.getString(if (isFreeAccount) R.string.my_account_upgrade_pro else R.string.plans_depleted_transfer_overquota)
        } else {
            paymentButton.text = StringResourcesUtils.getString(R.string.login_text)
        }
        paymentButton.setOnClickListener {
            transferGeneralOverQuotaWarning?.dismiss()
            if (isLoggedIn) {
                navigateToUpgradeAccount()
            } else {
                navigateToLogin()
            }
        }
        TimeUtils.createAndShowCountDownTimer(stringResource, transferGeneralOverQuotaWarning, text)
        transferGeneralOverQuotaWarning?.show()
        isGeneralTransferOverQuotaWarningShown = true
    }

    /**
     * Launches an intent to navigate to Login screen.
     */
    protected fun navigateToLogin(isNewTask: Boolean = false) {
        val intent = Intent(this, LoginActivity::class.java)
        intent.putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT)
        if (isNewTask) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(intent)
    }

    /**
     * Launches an intent to navigate to Upgrade Account screen.
     */
    open fun navigateToUpgradeAccount() {
        val intent = Intent(this, ManagerActivity::class.java)
        intent.action = ACTION_SHOW_UPGRADE_ACCOUNT
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }

    override fun registerReceiver(receiver: BroadcastReceiver?, filter: IntentFilter): Intent? {
        return try {
            super.registerReceiver(receiver, filter)
        } catch (e: IllegalStateException) {
            Timber.e(e, "IllegalStateException registering receiver")
            null
        }
    }

    override fun unregisterReceiver(receiver: BroadcastReceiver) {
        try {
            //If the receiver is not registered, it throws an IllegalArgumentException
            super.unregisterReceiver(receiver)
        } catch (e: IllegalArgumentException) {
            Timber.w(e, "IllegalArgumentException unregistering transfersUpdateReceiver")
        }
    }

    /**
     * Checks if should refresh session due to megaApi.
     *
     * @return True if should refresh session, false otherwise.
     */
    protected fun shouldRefreshSessionDueToSDK(): Boolean {
        if (megaApi.rootNode == null) {
            Timber.w("Refresh session - sdk")
            refreshSession()
            return true
        }
        return false
    }

    /**
     * Checks if should refresh session due to karere or init megaChatAp if the init state is not
     * the right one.
     *
     * @return True if should refresh session or megaChatApi cannot be recovered, false otherwise.
     */
    protected fun shouldRefreshSessionDueToKarere(): Boolean {
        var state = megaChatApi.initState

        if (state == MegaChatApi.INIT_ERROR || state == MegaChatApi.INIT_NOT_DONE) {
            Timber.w("MegaChatApi state: %s", state)

            state = megaChatApi.init(dbH.credentials?.session)
            Timber.d("result of init ---> %s", state)

            if (state == MegaChatApi.INIT_ERROR) {
                // The megaChatApi cannot be recovered, then logout
                megaChatApi.logout(ChatLogoutListener(this, loggingSettings))
                return true
            }
        }

        return false
    }

    /**
     * Refresh session.
     */
    protected fun refreshSession() {
        navigateToLogin()
        finish()
    }

    override fun launchActivity(intent: Intent) {
        startActivity(intent)
    }

    // TODO Migrate to registerForActivityResult()
    override fun launchActivityForResult(intent: Intent, requestCode: Int) {
        @Suppress("DEPRECATION")
        startActivityForResult(intent, requestCode)
    }

    override fun askPermissions(permissions: Array<String>, requestCode: Int) {
        requestPermission(this, requestCode, *permissions)
    }

    @SuppressWarnings("deprecation")
    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        Timber.d("Request code: %d, Result code:%d", requestCode, resultCode)
        if (requestCode == RequestCode.REQ_CODE_BUY) {
            if (resultCode == RESULT_OK) {
                val purchaseResult = billingManager?.getPurchaseResult(intent)
                if (BillingManager.ORDER_STATE_SUCCESS == purchaseResult) {
                    billingManager?.updatePurchase()
                } else {
                    Timber.w("Purchase failed, error code: %s", purchaseResult)
                }
            } else {
                Timber.w("cancel subscribe")
            }
        } else {
            Timber.w("No request code processed")
            @Suppress("DEPRECATION")
            super.onActivityResult(requestCode, resultCode, intent)
        }
    }

    /**
     * Initializes billing manager.
     */
    protected fun initPayments() {
        billingManager = BillingManagerImpl(this, this)
    }

    /**
     * Destroys billing manager.
     */
    protected fun destroyPayments() {
        billingManager?.destroy()
    }

    /**
     * Launches payment flow.
     */
    protected fun launchPayment(productId: String?) {
        val skuDetails = getSkuDetails(skuDetailsList, productId ?: return)
        if (skuDetails == null) {
            Timber.e("Cannot launch payment, MegaSku is null.")
            return
        }

        val purchase = myAccountInfo.activeSubscription
        val oldSku = purchase?.sku
        val token = purchase?.token
        billingManager?.initiatePurchaseFlow(oldSku, token, skuDetails)
    }

    override fun onBillingClientSetupFinished() {
        Timber.i("Billing client setup finished")
        billingManager?.getInventory { skuList: List<MegaSku>? ->
            skuDetailsList = skuList
            myAccountInfo.availableSkus = (skuList ?: return@getInventory)
            updatePricing(this)
        }
    }

    override fun onBillingClientSetupFailed() {
        Timber.w("Billing not available: Show pricing")
        updatePricing(this)
    }

    override fun onPurchasesUpdated(
        isFailed: Boolean,
        resultCode: Int,
        purchases: List<MegaPurchase>,
    ) {
        if (isFailed) {
            Timber.w("Update purchase failed, with result code: %s", resultCode)
            return
        }
        val purchaseResult: PurchaseType = if (purchases.isNotEmpty()) {
            val purchase = purchases[0]
            //payment may take time to process, we will not give privilege until it has been fully processed
            val sku = purchase.sku
            if (billingManager?.isPurchased(purchase) == true) {
                //payment has been processed
                Timber.d("Purchase " + sku + " successfully, subscription type is: "
                        + getSubscriptionType(sku) + ", subscription renewal type is: "
                        + getSubscriptionRenewalType(sku))
                updateAccountInfo(this, purchases, myAccountInfo)
                updateSubscriptionLevel(myAccountInfo, dbH, megaApi)
                RatingHandlerImpl(this).updateTransactionFlag(true)
                PurchaseType.SUCCESS
            } else {
                //payment is being processed or in unknown state
                Timber.d("Purchase %s is being processed or in unknown state.", sku)
                PurchaseType.PENDING
            }
        } else {
            //down grade case
            Timber.d("Downgrade, the new subscription takes effect when the old one expires.")
            PurchaseType.DOWNGRADE
        }
        LiveEventBus.get(EVENT_PURCHASES_UPDATED, PurchaseType::class.java).post(purchaseResult)
    }

    override fun onQueryPurchasesFinished(
        isFailed: Boolean,
        resultCode: Int,
        purchases: MutableList<MegaPurchase>?,
    ) {
        if (isFailed || purchases == null) {
            Timber.w("Query of purchases failed, result code is %d, is purchase null: %s",
                resultCode,
                purchases == null)
            return
        }
        updateAccountInfo(this, purchases, myAccountInfo)
        updateSubscriptionLevel(myAccountInfo, dbH, megaApi)
    }

    override fun showSnackbar(type: Int, content: String?, chatId: Long) {
        val rootView = Util.getRootViewFromContext(this)
        showSnackbar(type = type, view = rootView, s = content, idChat = chatId)
    }

    /**
     * Shows a dialog when the user is selecting the download location.
     * It asks if they want to set the current chosen location as default.
     * It the user enables the checkbox, the dialog should not appear again.
     *
     * @param path Download path to set as default location.
     */
    fun showConfirmationSaveInSameLocation(path: String?) {
        if (Util.isAndroid11OrUpper() || isAlertDialogShown(setDownloadLocationDialog)) {
            return
        }
        downloadLocation = path
        val builder = MaterialAlertDialogBuilder(this)
        val v = layoutInflater.inflate(R.layout.dialog_general_confirmation, null)
        builder.setView(v)
        val text = v.findViewById<TextView>(R.id.confirmation_text)
        text.setText(R.string.confirmation_download_location)
        val confirmationButton = v.findViewById<Button>(R.id.positive_button)
        confirmationButton.setText(R.string.general_yes)
        confirmationButton.setOnClickListener {
            setDownloadLocationDialog?.dismiss()
            dbH.setStorageAskAlways(false)
            dbH.setStorageDownloadLocation(path)
        }
        val cancelButton = v.findViewById<Button>(R.id.negative_button)
        cancelButton.setText(R.string.general_negative_button)
        cancelButton.setOnClickListener {
            setDownloadLocationDialog?.dismiss()
        }
        val checkBox = v.findViewById<CheckBox>(R.id.confirmation_checkbox)
        checkBox.isChecked = confirmationChecked
        val checkBoxLayout = v.findViewById<LinearLayout>(R.id.confirmation_checkbox_layout)
        checkBoxLayout.setOnClickListener { checkBox.isChecked = !checkBox.isChecked }
        setDownloadLocationDialog = builder.setCancelable(false)
            .setOnDismissListener { dbH.askSetDownloadLocation = !checkBox.isChecked }
            .create()
        setDownloadLocationDialog?.show()
    }

    /**
     * Shows the result of a purchase as an alert.
     */
    private fun showQueryPurchasesResult() {
        if (purchaseType == null || isAlertDialogShown(upgradeAlert)) {
            return
        }
        val builder = MaterialAlertDialogBuilder(this)
            .setPositiveButton(StringResourcesUtils.getString(R.string.general_ok), null)
        when (purchaseType) {
            PurchaseType.PENDING -> upgradeAlert =
                builder.setTitle(StringResourcesUtils.getString(R.string.title_user_purchased_subscription))
                    .setMessage(StringResourcesUtils.getString(R.string.message_user_payment_pending))
                    .create()
            PurchaseType.DOWNGRADE -> upgradeAlert =
                builder.setTitle(StringResourcesUtils.getString(R.string.my_account_upgrade_pro))
                    .setMessage(StringResourcesUtils.getString(R.string.message_user_purchased_subscription_down_grade))
                    .create()
            PurchaseType.SUCCESS -> {
                upgradeAlert = builder.setView(R.layout.dialog_purchase_success).create().apply {
                    setOnShowListener {
                        val purchaseType = findViewById<TextView>(R.id.purchase_type)
                        val purchaseImage = findViewById<ImageView>(R.id.purchase_image)
                        val purchaseMessage =
                            findViewById<TextView>(R.id.purchase_message)
                        if (purchaseType == null || purchaseImage == null || purchaseMessage == null) {
                            return@setOnShowListener
                        }
                        val account: Int
                        var color = R.color.red_600_red_300
                        val image: Int
                        val activeSubscriptionSku =
                            if (myAccountInfo.activeSubscription != null) myAccountInfo.activeSubscription?.sku
                            else ""

                        when (myAccountInfo.levelInventory) {
                            PRO_I -> {
                                account = R.string.pro1_account
                                image = R.drawable.ic_pro_i_big_crest
                                purchaseMessage.text = StringResourcesUtils.getString(
                                    if (BillingManagerImpl.SKU_PRO_I_YEAR == activeSubscriptionSku) R.string.upgrade_account_successful_pro_1_yearly
                                    else R.string.upgrade_account_successful_pro_1_monthly)
                            }
                            PRO_II -> {
                                account = R.string.pro2_account
                                image = R.drawable.ic_pro_ii_big_crest
                                purchaseMessage.text = StringResourcesUtils.getString(
                                    if (BillingManagerImpl.SKU_PRO_II_YEAR == activeSubscriptionSku) R.string.upgrade_account_successful_pro_2_yearly
                                    else R.string.upgrade_account_successful_pro_2_monthly)
                            }
                            PRO_III -> {
                                account = R.string.pro3_account
                                image = R.drawable.ic_pro_iii_big_crest
                                purchaseMessage.text = StringResourcesUtils.getString(
                                    if (BillingManagerImpl.SKU_PRO_III_YEAR == activeSubscriptionSku) R.string.upgrade_account_successful_pro_3_yearly
                                    else R.string.upgrade_account_successful_pro_3_monthly)
                            }
                            PRO_LITE -> {
                                account = R.string.prolite_account
                                color = R.color.orange_400_orange_300
                                image = R.drawable.ic_lite_big_crest
                                purchaseMessage.text = StringResourcesUtils.getString(
                                    if (BillingManagerImpl.SKU_PRO_LITE_YEAR == activeSubscriptionSku) R.string.upgrade_account_successful_pro_lite_yearly
                                    else R.string.upgrade_account_successful_pro_lite_monthly)
                            }
                            else -> {
                                Timber.w("Unexpected account subscription level")
                                return@setOnShowListener
                            }
                        }
                        purchaseType.text = StringResourcesUtils.getString(account)
                        purchaseType.setTextColor(ContextCompat.getColor(this@BaseActivity, color))
                        purchaseImage.setImageResource(image)
                    }
                }
            }
            else -> {
                Timber.w("Unexpected PurchaseType")
                return
            }
        }

        upgradeAlert?.show()
    }

    /**
     * Checks if can process the throwable and if so, launches the corresponding action.
     *
     * @param throwable Throwable to check.
     * @return True if the Throwable has been managed, false otherwise.
     */
    protected open fun manageCopyMoveException(throwable: Throwable?): Boolean =
        when (throwable) {
            is ForeignNodeException -> {
                launchForeignNodeError()
                true
            }
            is QuotaExceededMegaException -> {
                launchOverQuota()
                true
            }
            is NotEnoughQuotaMegaException -> {
                launchPreOverQuota()
                true
            }
            else -> false
        }

    /**
     * Launches ManagerActivity intent to show over quota warning.
     */
    protected fun launchOverQuota() {
        startActivity(Intent(this, ManagerActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .setAction(ACTION_OVERQUOTA_STORAGE))
        finish()
    }

    /**
     * Launches ManagerActivity intent to show pre over quota warning.
     */
    protected fun launchPreOverQuota() {
        startActivity(Intent(this, ManagerActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .setAction(ACTION_PRE_OVERQUOTA_STORAGE))
        finish()
    }

    /**
     * Shows foreign storage over quota warning.
     */
    protected fun launchForeignNodeError() {
        showForeignStorageOverQuotaWarningDialog(this)
    }

    companion object {
        private const val EXPIRED_BUSINESS_ALERT_SHOWN = "EXPIRED_BUSINESS_ALERT_SHOWN"
        private const val TRANSFER_OVER_QUOTA_WARNING_SHOWN = "TRANSFER_OVER_QUOTA_WARNING_SHOWN"
        private const val RESUME_TRANSFERS_WARNING_SHOWN = "RESUME_TRANSFERS_WARNING_SHOWN"
        private const val SET_DOWNLOAD_LOCATION_SHOWN = "SET_DOWNLOAD_LOCATION_SHOWN"
        private const val IS_CONFIRMATION_CHECKED = "IS_CONFIRMATION_CHECKED"
        private const val DOWNLOAD_LOCATION = "DOWNLOAD_LOCATION"
        private const val UPGRADE_ALERT_SHOWN = "UPGRADE_ALERT_SHOWN"
        private const val EVENT_PURCHASES_UPDATED = "EVENT_PURCHASES_UPDATED"
        private const val PURCHASE_TYPE = "PURCHASE_TYPE"

        /**
         * User account locked.
         */
        const val NAME_USER_LOCKED = "NAME_USER_LOCKED"

        /**
         * Method to display a simple Snackbar.
         *
         * @param outMetrics DisplayMetrics of the current device
         * @param view       Layout where the snackbar is going to show.
         * @param s          Text to shown in the snackbar
         */
        @JvmStatic
        fun showSimpleSnackbar(outMetrics: DisplayMetrics?, view: View?, s: String?) {
            val snackbar = Snackbar.make(view ?: return, s ?: return, Snackbar.LENGTH_LONG)
            val snackLayout = snackbar.view as SnackbarLayout
            snackLayout.setBackgroundResource(R.drawable.background_snackbar)
            val params = snackLayout.layoutParams as FrameLayout.LayoutParams
            params.setMargins(Util.dp2px(8f, outMetrics),
                0,
                Util.dp2px(8f, outMetrics),
                Util.dp2px(8f, outMetrics))
            snackLayout.layoutParams = params
            val snackTextView =
                snackbar.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
            snackTextView.maxLines = 5
            snackbar.show()
        }
    }
}
