package mega.privacy.android.app

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
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
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_LONG
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.SnackbarLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.activities.settingsActivities.FileManagementPreferencesActivity
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.databinding.TransferOverquotaLayoutBinding
import mega.privacy.android.app.globalmanagement.MyAccountInfo
import mega.privacy.android.app.interfaces.ActivityLauncher
import mega.privacy.android.app.interfaces.PermissionRequester
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.listeners.ChatLogoutListener
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.meeting.activity.MeetingActivity
import mega.privacy.android.app.myAccount.MyAccountActivity
import mega.privacy.android.app.presentation.base.BaseViewModel
import mega.privacy.android.app.presentation.billing.BillingViewModel
import mega.privacy.android.app.presentation.container.AppContainerWrapper
import mega.privacy.android.app.presentation.locale.SupportedLanguageContextWrapper
import mega.privacy.android.app.presentation.login.LoginActivity
import mega.privacy.android.app.presentation.transfers.TransfersManagementViewModel
import mega.privacy.android.app.presentation.verification.SMSVerificationActivity
import mega.privacy.android.app.presentation.weakaccountprotection.WeakAccountProtectionAlertActivity
import mega.privacy.android.app.service.iar.RatingHandlerImpl
import mega.privacy.android.app.snackbarListeners.SnackbarNavigateOption
import mega.privacy.android.app.utils.AlertDialogUtil.dismissAlertDialogIfExists
import mega.privacy.android.app.utils.AlertDialogUtil.isAlertDialogShown
import mega.privacy.android.app.utils.AlertsAndWarnings.showForeignStorageOverQuotaWarningDialog
import mega.privacy.android.app.utils.ColorUtils.setStatusBarTextColor
import mega.privacy.android.app.utils.Constants.ACCOUNT_BLOCKED_STRING
import mega.privacy.android.app.utils.Constants.ACCOUNT_BLOCKED_TYPE
import mega.privacy.android.app.utils.Constants.ACTION_OVERQUOTA_STORAGE
import mega.privacy.android.app.utils.Constants.ACTION_PRE_OVERQUOTA_STORAGE
import mega.privacy.android.app.utils.Constants.ACTION_SHOW_UPGRADE_ACCOUNT
import mega.privacy.android.app.utils.Constants.ACTION_SHOW_WARNING_ACCOUNT_BLOCKED
import mega.privacy.android.app.utils.Constants.BUSINESS
import mega.privacy.android.app.utils.Constants.DISMISS_ACTION_SNACKBAR
import mega.privacy.android.app.utils.Constants.INVITE_CONTACT_TYPE
import mega.privacy.android.app.utils.Constants.LAUNCH_INTENT
import mega.privacy.android.app.utils.Constants.LOGIN_FRAGMENT
import mega.privacy.android.app.utils.Constants.MESSAGE_SNACKBAR_TYPE
import mega.privacy.android.app.utils.Constants.MUTE_NOTIFICATIONS_SNACKBAR_TYPE
import mega.privacy.android.app.utils.Constants.NOT_CALL_PERMISSIONS_SNACKBAR_TYPE
import mega.privacy.android.app.utils.Constants.NOT_SPACE_SNACKBAR_TYPE
import mega.privacy.android.app.utils.Constants.OPEN_FILE_SNACKBAR_TYPE
import mega.privacy.android.app.utils.Constants.PERMISSIONS_TYPE
import mega.privacy.android.app.utils.Constants.SENT_REQUESTS_TYPE
import mega.privacy.android.app.utils.Constants.SNACKBAR_TYPE
import mega.privacy.android.app.utils.Constants.VISIBLE_FRAGMENT
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.billing.PaymentUtils.getSubscriptionRenewalType
import mega.privacy.android.app.utils.billing.PaymentUtils.getSubscriptionType
import mega.privacy.android.app.utils.permission.PermissionUtils.requestPermission
import mega.privacy.android.app.utils.permission.PermissionUtils.toAppInfo
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.data.qualifier.MegaApiFolder
import mega.privacy.android.domain.entity.PurchaseType
import mega.privacy.android.domain.entity.account.AccountBlockedDetail
import mega.privacy.android.domain.entity.account.AccountBlockedType
import mega.privacy.android.domain.entity.account.Skus
import mega.privacy.android.domain.entity.billing.BillingEvent
import mega.privacy.android.domain.entity.billing.MegaPurchase
import mega.privacy.android.domain.exception.NotEnoughQuotaMegaException
import mega.privacy.android.domain.exception.QuotaExceededMegaException
import mega.privacy.android.domain.exception.node.ForeignNodeException
import mega.privacy.android.domain.usecase.GetAccountDetailsUseCase
import mega.privacy.android.domain.usecase.MonitorChatSignalPresenceUseCase
import mega.privacy.android.domain.usecase.login.GetAccountCredentialsUseCase
import mega.privacy.android.domain.usecase.login.SaveAccountCredentialsUseCase
import mega.privacy.android.domain.usecase.network.MonitorSslVerificationFailedUseCase
import mega.privacy.android.domain.usecase.setting.MonitorCookieSettingsSavedUseCase
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.TransferOverQuotaDialogEvent
import mega.privacy.mobile.analytics.event.TransferOverQuotaUpgradeAccountButtonEvent
import nz.mega.sdk.MegaAccountDetails
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaChatApi
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/**
 * Base activity which includes common behaviors for several activities.
 *
 * @property megaApi                        [MegaApiAndroid]
 * @property megaApiFolder                  [MegaApiAndroid]
 * @property megaChatApi                    [MegaChatApiAndroid]
 * @property dbH                            [DatabaseHandler]
 * @property myAccountInfo                  [MyAccountInfo]
 * @property app                            [MegaApplication]
 * @property outMetrics                     [DisplayMetrics]
 * @property getAccountDetailsUseCase
 * @property billingViewModel
 */
@AndroidEntryPoint
abstract class BaseActivity : AppCompatActivity(), ActivityLauncher, PermissionRequester,
    SnackbarShower {

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
    lateinit var getAccountDetailsUseCase: GetAccountDetailsUseCase

    @Inject
    lateinit var monitorCookieSettingsSavedUseCase: MonitorCookieSettingsSavedUseCase

    @Inject
    lateinit var monitorSslVerificationFailedUseCase: MonitorSslVerificationFailedUseCase

    @Inject
    lateinit var getAccountCredentialsUseCase: GetAccountCredentialsUseCase

    @Inject
    lateinit var saveAccountCredentialsUseCase: SaveAccountCredentialsUseCase

    /**
     * Monitor Chat Signal Presence Use Case
     * Check if chat has signal presence
     */
    @Inject
    lateinit var monitorChatSignalPresenceUseCase: MonitorChatSignalPresenceUseCase

    /**
     * Psa handler
     */
    @Inject
    lateinit var appContainerWrapper: AppContainerWrapper

    private val billingViewModel by viewModels<BillingViewModel>()
    private val viewModel by viewModels<BaseViewModel>()
    protected val transfersManagementViewModel: TransfersManagementViewModel by viewModels()

    @JvmField
    protected var app: MegaApplication = MegaApplication.getInstance()
    private var sslErrorDialog: AlertDialog? = null
    private var delaySignalPresence = false
    private var isGeneralTransferOverQuotaWarningShown = false
    private var transferGeneralOverQuotaWarning: AlertDialog? = null
    private var setDownloadLocationDialog: AlertDialog? = null
    private var confirmationChecked = false
    private var downloadLocation: String? = null
    private var upgradeAlert: AlertDialog? = null
    private var purchaseType: PurchaseType? = null
    private var activeSubscriptionSku: String? = null
    private var expiredBusinessAlert: AlertDialog? = null
    private var isExpiredBusinessAlertShown = false

    protected val outMetrics: DisplayMetrics by lazy { resources.displayMetrics }

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

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            handleGoBack()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        collectFlow(billingViewModel.billingUpdateEvent) {
            if (it is BillingEvent.OnPurchaseUpdate) {
                onPurchasesUpdated(it.purchases, it.activeSubscription)
                billingViewModel.markHandleBillingEvent()
            }
        }

        collectFlow(viewModel.state) { uiState ->
            with(uiState) {
                accountBlockedDetail?.apply {
                    checkWhyAmIBlocked(this)
                    viewModel.onAccountBlockedConsumed()
                }
                if (showExpiredBusinessAlert) {
                    showExpiredBusinessAlert()
                    viewModel.onShowExpiredBusinessAlertConsumed()
                }
            }
        }

        collectFlow(monitorChatSignalPresenceUseCase(), Lifecycle.State.CREATED) {
            Timber.d("BROADCAST TO SEND SIGNAL PRESENCE")
            if (delaySignalPresence && megaChatApi.presenceConfig != null && !megaChatApi.presenceConfig.isPending) {
                delaySignalPresence = false
                retryConnectionsAndSignalPresence()
            }
        }

        collectFlow(monitorCookieSettingsSavedUseCase()) {
            val view = window.decorView.findViewById<View>(android.R.id.content)
            if (view != null) {
                showSnackbar(
                    view,
                    getString(R.string.dialog_cookie_snackbar_saved)
                )
            }
        }

        collectFlow(monitorSslVerificationFailedUseCase()) {
            Timber.d("BROADCAST TO MANAGE A SSL VERIFICATION ERROR")
            if (sslErrorDialog?.isShowing != true) {
                showSSLErrorDialog()
            }
        }

        if (allowToShowOverQuotaWarning) {
            collectFlow(transfersManagementViewModel.state.map { it.transferOverQuotaWarning }
                .distinctUntilChanged()) { isTransferOverQuotaWarning ->
                if (isTransferOverQuotaWarning) {
                    showGeneralTransferOverQuotaWarning()
                    transfersManagementViewModel.onTransferOverQuotaWarningConsumed()
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
                TRANSFER_OVER_QUOTA_WARNING_SHOWN, false
            )

            if (isGeneralTransferOverQuotaWarningShown) {
                showGeneralTransferOverQuotaWarning()
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

                showQueryPurchasesResult(MegaPurchase(sku = getString(ACTIVE_SUBSCRIPTION_SKU)))
            }
        }

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


    override fun onSaveInstanceState(outState: Bundle) {
        outState.apply {
            putBoolean(EXPIRED_BUSINESS_ALERT_SHOWN, isExpiredBusinessAlertShown)
            putBoolean(TRANSFER_OVER_QUOTA_WARNING_SHOWN, isGeneralTransferOverQuotaWarningShown)
            putBoolean(SET_DOWNLOAD_LOCATION_SHOWN, isAlertDialogShown(setDownloadLocationDialog))
            putBoolean(IS_CONFIRMATION_CHECKED, confirmationChecked)
            putString(DOWNLOAD_LOCATION, downloadLocation)
            putBoolean(UPGRADE_ALERT_SHOWN, isAlertDialogShown(upgradeAlert))
            putSerializable(PURCHASE_TYPE, purchaseType)
            putString(ACTIVE_SUBSCRIPTION_SKU, activeSubscriptionSku)
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

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(SupportedLanguageContextWrapper.wrap(newBase))
    }


    override fun onDestroy() {
        dismissAlertDialogIfExists(transferGeneralOverQuotaWarning)
        dismissAlertDialogIfExists(transferGeneralOverQuotaWarning)
        dismissAlertDialogIfExists(setDownloadLocationDialog)
        dismissAlertDialogIfExists(upgradeAlert)
        super.onDestroy()
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
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
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
            megaChatApi.retryPendingConnections(false)

            if (megaChatApi.presenceConfig != null && !megaChatApi.presenceConfig.isPending) {
                delaySignalPresence = false

                if (this !is MeetingActivity) {
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
        forceDarkMode: Boolean = false,
    ) {
        showSnackbar(
            type = type,
            view = view,
            anchor = anchor,
            s = s,
            idChat = idChat,
            forceDarkMode = forceDarkMode
        )
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
        showSnackbar(type, view, anchor, s, idChat, userEmail, false)
    }

    /**
     * Method to display a simple or action Snackbar.
     *
     * @param type          There are three possible values to this param:
     *                      - SNACKBAR_TYPE: creates a simple snackbar
     *                      - MESSAGE_SNACKBAR_TYPE: creates an action snackbar which function is to go to Chat section
     *                      - NOT_SPACE_SNACKBAR_TYPE: creates an action snackbar which function is to go to Storage-Settings section
     *                      - MUTE_NOTIFICATIONS_SNACKBAR_TYPE: creates an action snackbar which function is unmute chats notifications
     *                      - INVITE_CONTACT_TYPE: creates an action snackbar which function is to send a contact invitation
     * @param view          Layout where the snackbar is going to show.
     * @param anchor        Sets the view the Snackbar should be anchored above, null as default
     * @param s             Text to shown in the snackbar
     * @param idChat        Chat ID. If this param has a valid value the function of MESSAGE_SNACKBAR_TYPE ends in the specified chat.
     *                      If the value is -1 (MEGACHAT_INVALID_HANDLE) the function ends in chats list view.
     * @param userEmail     Email of the user to be invited.
     * @param forceDarkMode True if want to force to display the snackbar like in dark mode or False otherwise
     * @param action        To perform when the snackbar action is clicked.
     */
    fun showSnackbar(
        type: Int,
        view: View,
        anchor: View? = null,
        s: String?,
        idChat: Long = MEGACHAT_INVALID_HANDLE,
        userEmail: String? = null,
        forceDarkMode: Boolean = false,
        action: () -> Unit = {},
    ) {
        Timber.d("Show snackbar: %s", s)
        snackbar = try {
            when (type) {
                MESSAGE_SNACKBAR_TYPE -> Snackbar.make(
                    view,
                    (if (s?.isNotEmpty() == true) s else getString(R.string.sent_as_message))
                        ?: return,
                    Snackbar.LENGTH_LONG
                )

                NOT_SPACE_SNACKBAR_TYPE -> Snackbar.make(
                    view,
                    R.string.error_not_enough_free_space,
                    Snackbar.LENGTH_LONG
                )

                MUTE_NOTIFICATIONS_SNACKBAR_TYPE -> Snackbar.make(
                    view,
                    R.string.notifications_are_already_muted,
                    Snackbar.LENGTH_LONG
                )

                DISMISS_ACTION_SNACKBAR -> Snackbar.make(
                    view,
                    s ?: return,
                    Snackbar.LENGTH_INDEFINITE
                )

                NOT_CALL_PERMISSIONS_SNACKBAR_TYPE -> Snackbar.make(
                    view,
                    s ?: return,
                    Snackbar.LENGTH_LONG
                )

                else -> Snackbar.make(view, s ?: return, Snackbar.LENGTH_LONG)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error showing snackbar")
            return
        }

        snackbar?.apply {
            // Match snackbar time with compose MegaSnackbar
            if (duration == LENGTH_LONG) {
                duration = 4.seconds.inWholeMilliseconds.toInt()
            }
            val snackbarLayout = this.view as SnackbarLayout
            if (forceDarkMode) {
                setTextColor(resources.getColor(R.color.grey_alpha_087, theme))
                snackbarLayout.setBackgroundColor(resources.getColor(R.color.white, theme))
            } else {
                snackbarLayout.setBackgroundResource(R.drawable.background_snackbar)
            }

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
                    setAction(
                        R.string.action_see,
                        SnackbarNavigateOption(context = view.context, idChat = idChat)
                    )
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
                    setAction(
                        R.string.contact_invite,
                        SnackbarNavigateOption(view.context, type, userEmail)
                    )
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
                    setAction(getString(R.string.general_confirmation_open)) { action() }
                    show()
                }

                SENT_REQUESTS_TYPE -> {
                    setAction(
                        R.string.tab_sent_requests,
                        SnackbarNavigateOption(view.context, type, userEmail)
                    )
                    show()
                }

                NOT_CALL_PERMISSIONS_SNACKBAR_TYPE -> {
                    setAction(
                        R.string.general_allow, toAppInfo(applicationContext)
                    )
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
            runCatching {
                getAccountDetailsUseCase(false)
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    /**
     * This method is shown in a business account when the account is expired.
     * It informs that all the actions are only read.
     * The message is different depending if the account belongs to an admin or an user.
     */
    @Deprecated(
        message = "This Dialog is deprecated. Please use the Compose version BusinessAccountSuspendedDialog instead",
        replaceWith = ReplaceWith("BusinessAccountSuspendedDialog"),
        level = DeprecationLevel.WARNING,
    )
    protected fun showExpiredBusinessAlert() {
        if (isActivityInBackground || expiredBusinessAlert != null && expiredBusinessAlert?.isShowing == true) {
            return
        }

        val isProFlexiAccount =
            myAccountInfo.accountType == MegaAccountDetails.ACCOUNT_TYPE_PRO_FLEXI

        val builder =
            MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Mega_MaterialAlertDialog)
                .apply {
                    setTitle(
                        if (isProFlexiAccount) {
                            sharedR.string.account_pro_flexi_account_deactivated_dialog_title
                        } else {
                            R.string.account_business_account_deactivated_dialog_title
                        }
                    )
                    setMessage(
                        if (megaApi.isMasterBusinessAccount) {
                            R.string.account_business_account_deactivated_dialog_admin_body
                        } else if (isProFlexiAccount) {
                            sharedR.string.account_pro_flexi_account_deactivated_dialog_body
                        } else {
                            R.string.account_business_account_deactivated_dialog_sub_user_body
                        }
                    )
                    setNegativeButton(R.string.account_business_account_deactivated_dialog_button) { dialog, _ ->
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
     * @param accountBlockedDetail [AccountBlockedDetail]
     */
    private fun checkWhyAmIBlocked(accountBlockedDetail: AccountBlockedDetail) {
        when (accountBlockedDetail.type) {
            AccountBlockedType.NOT_BLOCKED -> {}
            AccountBlockedType.TOS_COPYRIGHT -> megaChatApi.logout(
                ChatLogoutListener {
                    showAccountBlockedDialog(
                        AccountBlockedType.TOS_COPYRIGHT,
                        getString(sharedR.string.dialog_account_suspended_ToS_copyright_message)
                    )
                }
            )

            AccountBlockedType.TOS_NON_COPYRIGHT -> megaChatApi.logout(
                ChatLogoutListener {
                    showAccountBlockedDialog(
                        AccountBlockedType.TOS_NON_COPYRIGHT,
                        getString(sharedR.string.dialog_account_suspended_ToS_non_copyright_message)
                    )
                }
            )

            AccountBlockedType.SUBUSER_DISABLED -> megaChatApi.logout(
                ChatLogoutListener {
                    showAccountBlockedDialog(
                        AccountBlockedType.SUBUSER_DISABLED,
                        getString(sharedR.string.error_business_disabled)
                    )
                }
            )

            AccountBlockedType.SUBUSER_REMOVED -> megaChatApi.logout(
                ChatLogoutListener {
                    showAccountBlockedDialog(
                        AccountBlockedType.SUBUSER_REMOVED,
                        getString(sharedR.string.error_business_removed)
                    )
                }
            )

            AccountBlockedType.VERIFICATION_SMS -> {
                if (megaApi.smsAllowedState() == 0 || MegaApplication.isVerifySMSShowed) return
                MegaApplication.smsVerifyShowed(true)
                lifecycleScope.launch {
                    runCatching {
                        saveAccountCredentialsUseCase()
                    }.onFailure {
                        Timber.e(it)
                    }
                    Timber.d("Show SMS verification activity.")
                    val intent = Intent(applicationContext, SMSVerificationActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    intent.putExtra(NAME_USER_LOCKED, true)
                    startActivity(intent)
                }
            }

            AccountBlockedType.VERIFICATION_EMAIL -> {
                showAccountBlockedDialog(
                    AccountBlockedType.VERIFICATION_EMAIL,
                    getString(sharedR.string.login_account_suspension_email_verification_message)
                )
            }

            else -> Util.showErrorAlertDialog(accountBlockedDetail.text, false, this)
        }
    }

    /**
     * Sets if should finish the activity because of some error.
     */
    protected fun setFinishActivityAtError(finishActivityAtError: Boolean) {
        this.finishActivityAtError = finishActivityAtError
    }

    private fun showAccountBlockedDialog(
        accountBlockedType: AccountBlockedType,
        accountBlockedString: String,
    ) {
        if (!TextUtil.isTextEmpty(accountBlockedString)) {
            if (this is LoginActivity) {
                this.showAccountBlockedDialog(
                    AccountBlockedDetail(
                        accountBlockedType,
                        accountBlockedString
                    )
                )
            } else {
                if (this is WeakAccountProtectionAlertActivity && accountBlockedType == AccountBlockedType.VERIFICATION_EMAIL) {
                    return
                } else {
                    val loginIntent =
                        Intent(this, LoginActivity::class.java).apply {
                            action = ACTION_SHOW_WARNING_ACCOUNT_BLOCKED
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT)
                            putExtra(
                                ACCOUNT_BLOCKED_STRING, accountBlockedString
                            )
                            putExtra(
                                ACCOUNT_BLOCKED_TYPE, accountBlockedType
                            )
                        }
                    startActivity(loginIntent)
                }
            }
        }
    }

    /**
     * Shows a warning indicating transfer over quota occurred.
     */
    fun showGeneralTransferOverQuotaWarning() = lifecycleScope.launch {
        if (isActivityInBackground || transfersManagementViewModel.isInTransfersSection() || transferGeneralOverQuotaWarning != null) return@launch
        val builder =
            MaterialAlertDialogBuilder(
                this@BaseActivity,
                R.style.ThemeOverlay_Mega_MaterialAlertDialog
            )
        val binding = TransferOverquotaLayoutBinding.inflate(layoutInflater)
        builder.setView(binding.root)
            .setOnDismissListener {
                isGeneralTransferOverQuotaWarningShown = false
                transferGeneralOverQuotaWarning = null
                transfersManagementViewModel.resetTransferOverQuotaTimestamp()
            }
            .setCancelable(false)
        transferGeneralOverQuotaWarning = builder.create()
        transferGeneralOverQuotaWarning?.setCanceledOnTouchOutside(false)
        val stringResource =
            if (transfersManagementViewModel.isTransferOverQuota()) R.string.current_text_depleted_transfer_overquota else R.string.text_depleted_transfer_overquota
        binding.textTransferOverquota.text = getString(
            stringResource, TimeUtils.getHumanizedTime(megaApi.bandwidthOverquotaDelay)
        )
        binding.transferOverquotaButtonPayment.apply {
            val credentials = runCatching { getAccountCredentialsUseCase() }.getOrNull()
            val isLoggedIn = megaApi.isLoggedIn != 0 && credentials != null
            text = if (isLoggedIn) {
                val isFreeAccount =
                    myAccountInfo.accountType == MegaAccountDetails.ACCOUNT_TYPE_FREE
                getString(if (isFreeAccount) sharedR.string.general_upgrade_button else R.string.plans_depleted_transfer_overquota)
            } else {
                getString(sharedR.string.login_text)
            }
            setOnClickListener {
                transferGeneralOverQuotaWarning?.dismiss()
                if (isLoggedIn) {
                    Analytics.tracker.trackEvent(TransferOverQuotaUpgradeAccountButtonEvent)
                    navigateToUpgradeAccount()
                } else {
                    navigateToLogin()
                }
            }
        }
        binding.transferOverquotaButtonDissmiss.setOnClickListener {
            transferGeneralOverQuotaWarning?.dismiss()
        }
        TimeUtils.createAndShowCountDownTimer(
            stringResource,
            transferGeneralOverQuotaWarning,
            binding.textTransferOverquota
        )
        Analytics.tracker.trackEvent(TransferOverQuotaDialogEvent)
        transferGeneralOverQuotaWarning?.show()
        isGeneralTransferOverQuotaWarningShown = true
    }

    /**
     * Launches an intent to navigate to Login screen.
     */
    @JvmOverloads
    protected fun navigateToLogin(
        isNewTask: Boolean = false,
        keepCurrentActivity: Boolean = false,
    ) {
        val intent = Intent(this, LoginActivity::class.java)
        intent.putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT)
        if (isNewTask) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        if (keepCurrentActivity) {
            intent.putExtra(LAUNCH_INTENT, this.intent)
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

    /**
     * Register Broadcast Receiver
     */
    @SuppressLint("WrongConstant")
    override fun registerReceiver(receiver: BroadcastReceiver?, filter: IntentFilter): Intent? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.registerReceiver(
                    this, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED
                )
            } else {
                super.registerReceiver(receiver, filter)
            }
        } catch (e: IllegalStateException) {
            Timber.e(e, "IllegalStateException registering receiver")
            null
        }
    }

    /**
     * Unregister Broadcast Receiver
     */
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
    @JvmOverloads
    protected fun shouldRefreshSessionDueToSDK(keepCurrentActivity: Boolean = false): Boolean {
        if (megaApi.rootNode == null) {
            Timber.w("Refresh session - sdk")
            refreshSession(keepCurrentActivity)
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
            val credentials =
                runBlocking { runCatching { getAccountCredentialsUseCase() }.getOrNull() }
            state = megaChatApi.init(credentials?.session)
            Timber.d("result of init ---> %s", state)

            if (state == MegaChatApi.INIT_ERROR) {
                // The megaChatApi cannot be recovered, then logout
                megaChatApi.logout(ChatLogoutListener())
                return true
            }
        }

        return false
    }

    /**
     * Refresh session.
     */
    protected fun refreshSession(keepCurrentActivity: Boolean = false) {
        navigateToLogin(keepCurrentActivity = keepCurrentActivity)
        finish()
    }

    override fun launchActivity(intent: Intent) {
        startActivity(intent)
    }


    override fun launchActivityForResult(intent: Intent, requestCode: Int) {
        @Suppress("DEPRECATION")
        startActivityForResult(intent, requestCode)
    }

    override fun askPermissions(permissions: Array<String>, requestCode: Int) {
        requestPermission(this, requestCode, *permissions)
    }

    private fun onPurchasesUpdated(
        purchases: List<MegaPurchase>,
        activeSubscription: MegaPurchase?,
    ) {
        val type: PurchaseType = if (purchases.isNotEmpty()) {
            val purchase = purchases.first()
            //payment may take time to process, we will not give privilege until it has been fully processed
            val sku = purchase.sku
            if (billingViewModel.isPurchased(purchase)) {
                //payment has been processed
                Timber.d(
                    "Purchase " + sku + " successfully, subscription type is: "
                            + getSubscriptionType(sku, this) + ", subscription renewal type is: "
                            + getSubscriptionRenewalType(sku, this)
                )
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
        when {
            this is MyAccountActivity && myAccountInfo.isUpgradeFromAccount()
                    || this is ManagerActivity && myAccountInfo.isUpgradeFromManager()
                    || this is FileManagementPreferencesActivity && myAccountInfo.isUpgradeFromSettings() -> {
                purchaseType = type
                activeSubscriptionSku = activeSubscription?.sku
                if (!handlePurchased(type)) {
                    showQueryPurchasesResult(activeSubscription)
                }
            }
        }
    }

    override fun showSnackbar(type: Int, content: String?, chatId: Long) {
        val rootView = Util.getRootViewFromContext(this)
        showSnackbar(type = type, view = rootView, s = content, idChat = chatId)
    }

    override fun showSnackbar(type: Int, content: String, action: () -> Unit) {
        val rootView = Util.getRootViewFromContext(this)
        showSnackbar(type = type, view = rootView, s = content, action = action)
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
    private fun showQueryPurchasesResult(activeSubscription: MegaPurchase?) {
        if (purchaseType == null || isAlertDialogShown(upgradeAlert)) {
            return
        }
        val builder = MaterialAlertDialogBuilder(this)
            .setPositiveButton(getString(R.string.general_ok), null)
        when (purchaseType) {
            PurchaseType.PENDING -> upgradeAlert =
                builder.setTitle(getString(R.string.title_user_purchased_subscription))
                    .setMessage(getString(R.string.message_user_payment_pending))
                    .create()

            PurchaseType.DOWNGRADE -> upgradeAlert =
                builder.setTitle(getString(sharedR.string.general_upgrade_button))
                    .setMessage(getString(R.string.message_user_purchased_subscription_down_grade))
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
                        val activeSubscriptionSku = activeSubscription?.sku.orEmpty()

                        when (activeSubscription?.sku) {
                            Skus.SKU_PRO_I_MONTH, Skus.SKU_PRO_I_YEAR -> {
                                account = R.string.pro1_account
                                image = R.drawable.ic_pro_i_big_crest
                                purchaseMessage.text = getString(
                                    if (Skus.SKU_PRO_I_YEAR == activeSubscriptionSku) R.string.upgrade_account_successful_pro_1_yearly
                                    else R.string.upgrade_account_successful_pro_1_monthly
                                )
                            }

                            Skus.SKU_PRO_II_MONTH, Skus.SKU_PRO_II_YEAR -> {
                                account = R.string.pro2_account
                                image = R.drawable.ic_pro_ii_big_crest
                                purchaseMessage.text = getString(
                                    if (Skus.SKU_PRO_II_YEAR == activeSubscriptionSku) R.string.upgrade_account_successful_pro_2_yearly
                                    else R.string.upgrade_account_successful_pro_2_monthly
                                )
                            }

                            Skus.SKU_PRO_III_MONTH, Skus.SKU_PRO_III_YEAR -> {
                                account = R.string.pro3_account
                                image = R.drawable.ic_pro_iii_big_crest
                                purchaseMessage.text = getString(
                                    if (Skus.SKU_PRO_III_YEAR == activeSubscriptionSku) R.string.upgrade_account_successful_pro_3_yearly
                                    else R.string.upgrade_account_successful_pro_3_monthly
                                )
                            }

                            Skus.SKU_PRO_LITE_MONTH, Skus.SKU_PRO_LITE_YEAR -> {
                                account = R.string.prolite_account
                                color = R.color.orange_400_orange_300
                                image = R.drawable.ic_lite_big_crest
                                purchaseMessage.text = getString(
                                    if (Skus.SKU_PRO_LITE_YEAR == activeSubscriptionSku) R.string.upgrade_account_successful_pro_lite_yearly
                                    else R.string.upgrade_account_successful_pro_lite_monthly
                                )
                            }

                            else -> {
                                Timber.w("Unexpected account subscription level")
                                return@setOnShowListener
                            }
                        }
                        purchaseType.text = getString(account)
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
    protected open fun manageCopyMoveException(throwable: Throwable?): Boolean = when (throwable) {
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
        startActivity(
            Intent(this, ManagerActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .setAction(ACTION_OVERQUOTA_STORAGE)
        )
        finish()
    }

    /**
     * Launches ManagerActivity intent to show pre over quota warning.
     */
    protected fun launchPreOverQuota() {
        startActivity(
            Intent(this, ManagerActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .setAction(ACTION_PRE_OVERQUOTA_STORAGE)
        )
        finish()
    }

    /**
     * Shows foreign storage over quota warning.
     */
    protected fun launchForeignNodeError() {
        showForeignStorageOverQuotaWarningDialog(this)
    }

    /**
     * Handle purchased
     *
     * @return true if handled and skip default behavior otherwise false
     */
    protected open fun handlePurchased(purchaseType: PurchaseType): Boolean = false

    /**
     * Allow to show the transfer over quota warning.
     */
    open val allowToShowOverQuotaWarning: Boolean = true

    companion object {
        private const val EXPIRED_BUSINESS_ALERT_SHOWN = "EXPIRED_BUSINESS_ALERT_SHOWN"
        private const val TRANSFER_OVER_QUOTA_WARNING_SHOWN = "TRANSFER_OVER_QUOTA_WARNING_SHOWN"
        private const val SET_DOWNLOAD_LOCATION_SHOWN = "SET_DOWNLOAD_LOCATION_SHOWN"
        private const val IS_CONFIRMATION_CHECKED = "IS_CONFIRMATION_CHECKED"
        private const val DOWNLOAD_LOCATION = "DOWNLOAD_LOCATION"
        private const val UPGRADE_ALERT_SHOWN = "UPGRADE_ALERT_SHOWN"
        private const val PURCHASE_TYPE = "PURCHASE_TYPE"
        private const val ACTIVE_SUBSCRIPTION_SKU = "ACTIVE_SUBSCRIPTION_SKU"

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
            params.setMargins(
                Util.dp2px(8f, outMetrics),
                0,
                Util.dp2px(8f, outMetrics),
                Util.dp2px(8f, outMetrics)
            )
            snackLayout.layoutParams = params
            val snackTextView =
                snackbar.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
            snackTextView.maxLines = 5
            snackbar.show()
        }
    }
}
