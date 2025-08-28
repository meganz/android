package mega.privacy.android.app.myAccount

import android.app.NotificationManager
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.constants.IntentConstants.Companion.ACTION_OPEN_ACHIEVEMENTS
import mega.privacy.android.app.constants.IntentConstants.Companion.EXTRA_MASTER_KEY
import mega.privacy.android.app.databinding.ActivityMyAccountBinding
import mega.privacy.android.app.databinding.DialogErrorPasswordInputEditTextBinding
import mega.privacy.android.app.extensions.consumeInsetsWithToolbar
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.main.dialog.storagestatus.TYPE_ANDROID_PLATFORM
import mega.privacy.android.app.main.dialog.storagestatus.TYPE_ANDROID_PLATFORM_NO_NAVIGATION
import mega.privacy.android.app.main.dialog.storagestatus.TYPE_ITUNES
import mega.privacy.android.app.middlelayer.iab.BillingConstant
import mega.privacy.android.app.presentation.cancelaccountplan.CancelAccountPlanActivity
import mega.privacy.android.app.presentation.changepassword.ChangePasswordActivity
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.logout.LogoutConfirmationDialog
import mega.privacy.android.app.presentation.logout.LogoutViewModel
import mega.privacy.android.app.presentation.testpassword.TestPasswordActivity
import mega.privacy.android.app.utils.AlertDialogUtil.isAlertDialogShown
import mega.privacy.android.app.utils.AlertDialogUtil.quitEditTextError
import mega.privacy.android.app.utils.AlertDialogUtil.setEditTextError
import mega.privacy.android.app.utils.ColorUtils
import mega.privacy.android.app.utils.Constants.ACTION_CANCEL_ACCOUNT
import mega.privacy.android.app.utils.Constants.ACTION_CHANGE_MAIL
import mega.privacy.android.app.utils.Constants.ACTION_PASS_CHANGED
import mega.privacy.android.app.utils.Constants.ACTION_RESET_PASS
import mega.privacy.android.app.utils.Constants.ACTION_RESET_PASS_FROM_LINK
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import mega.privacy.android.app.utils.Constants.NOTIFICATION_STORAGE_OVERQUOTA
import mega.privacy.android.app.utils.Constants.RESULT
import mega.privacy.android.app.utils.MenuUtils.toggleAllMenuItemsVisibility
import mega.privacy.android.app.utils.Util.isDarkMode
import mega.privacy.android.app.utils.Util.isOnline
import mega.privacy.android.app.utils.Util.showAlert
import mega.privacy.android.app.utils.Util.showKeyboardDelayed
import mega.privacy.android.app.utils.ViewUtils.hideKeyboard
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.navigation.ExtraConstant
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.navigation.payment.UpgradeAccountSource
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.CancelSubscriptionMenuToolbarEvent
import mega.privacy.mobile.analytics.event.ToolbarOverflowMenuItemEvent
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError.API_OK
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
internal class MyAccountActivity : PasscodeActivity(),
    SnackbarShower {

    companion object {
        private const val KILL_SESSIONS_SHOWN = "KILL_SESSIONS_SHOWN"
        private const val CANCEL_SUBSCRIPTIONS_SHOWN = "CANCEL_SUBSCRIPTIONS_SHOWN"
        private const val TYPED_FEEDBACK = "TYPED_FEEDBACK"
        private const val CONFIRM_CANCEL_SUBSCRIPTIONS_SHOWN = "CONFIRM_CANCEL_SUBSCRIPTIONS_SHOWN"
        private const val CONFIRM_CHANGE_EMAIL_SHOWN = "CONFIRM_CHANGE_EMAIL_SHOWN"
        private const val CONFIRM_RESET_PASSWORD_SHOWN = "CONFIRM_RESET_PASSWORD_SHOWN"
        private const val TYPE_CHANGE_EMAIL = 1
        private const val TYPE_CANCEL_ACCOUNT = 2
    }

    private val viewModel: MyAccountViewModel by viewModels()
    private val logoutViewModel: LogoutViewModel by viewModels()

    /**
     * Application Theme Mode
     */
    @Inject
    lateinit var monitorThemeModeUseCase: MonitorThemeModeUseCase

    @Inject
    lateinit var megaNavigator: MegaNavigator

    private val isBusinessAccount
        get() = viewModel.state.value.isBusinessAccount

    private val isProFlexiAccount
        get() = viewModel.state.value.isProFlexiAccount

    private lateinit var navController: NavController
    private lateinit var binding: ActivityMyAccountBinding

    private var menu: Menu? = null

    private var killSessionsConfirmationDialog: AlertDialog? = null
    private var cancelSubscriptionsDialog: AlertDialog? = null
    private var cancelSubscriptionsConfirmationDialog: AlertDialog? = null
    private var confirmCancelAccountDialog: AlertDialog? = null
    private var confirmChangeEmailDialog: AlertDialog? = null
    private var confirmResetPasswordDialog: AlertDialog? = null
    private var cancelSubscriptionsFeedback: String? = null
    private val onBackPressCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (!navController.navigateUp()) {
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        if (shouldRefreshSessionDueToSDK()) {
            return
        }

        binding = ActivityMyAccountBinding.inflate(layoutInflater)
        consumeInsetsWithToolbar(customToolbar = binding.toolbar)
        setContentView(binding.root)
        onBackPressedDispatcher.addCallback(this, onBackPressCallback)
        setupView()
        setupObservers()
        manageIntentExtras()
        initLogoutConfirmationDialogComposeView()

        if (savedInstanceState != null) {
            when {
                savedInstanceState.getBoolean(KILL_SESSIONS_SHOWN, false) -> {
                    showConfirmationKillSessions()
                }

                savedInstanceState.getBoolean(CANCEL_SUBSCRIPTIONS_SHOWN, false) -> {
                    cancelSubscriptionsFeedback = savedInstanceState.getString(TYPED_FEEDBACK)
                    handleShowCancelSubscription()
                }

                savedInstanceState.getBoolean(CONFIRM_CANCEL_SUBSCRIPTIONS_SHOWN, false) -> {
                    showConfirmationCancelSubscriptions()
                }

                savedInstanceState.getBoolean(CONFIRM_CHANGE_EMAIL_SHOWN, false) -> {
                    showConfirmChangeEmailDialog()
                }

                savedInstanceState.getBoolean(CONFIRM_RESET_PASSWORD_SHOWN, false) -> {
                    showConfirmResetPasswordDialog()
                }
            }
        }
    }

    private fun initLogoutConfirmationDialogComposeView() {
        binding.logoutConfirmationDialogComposeView.apply {
            isVisible = true
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val uiState by viewModel.state.collectAsStateWithLifecycle()
                val themeMode by monitorThemeModeUseCase().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
                if (uiState.showLogoutConfirmationDialog) {
                    OriginalTheme(isDark = themeMode.isDarkMode()) {
                        LogoutConfirmationDialog(
                            onDismissed = { viewModel.dismissLogoutConfirmationDialog() },
                            logoutViewModel = logoutViewModel
                        )
                    }
                }
            }
        }
    }

    private fun manageIntentExtras() {
        val accountType = intent.getIntExtra(ExtraConstant.EXTRA_ACCOUNT_TYPE, INVALID_VALUE)
        if (accountType != INVALID_VALUE) {
            megaNavigator.openUpgradeAccount(
                context = this,
                source = UpgradeAccountSource.MY_ACCOUNT_SCREEN
            )

            viewModel.setOpenUpgradeFrom()

            intent.removeExtra(ExtraConstant.EXTRA_ACCOUNT_TYPE)
        }

        when (intent.action) {
            ACTION_OPEN_ACHIEVEMENTS -> {
                navController.navigate(R.id.action_my_account_to_achievements)
                intent.action = null
            }

            ACTION_CANCEL_ACCOUNT -> {
                intent.dataString?.let { link ->
                    viewModel.cancelAccount(accountCancellationLink = link)
                }

                intent.action = null
            }

            ACTION_CHANGE_MAIL -> {
                intent.dataString?.let { link ->
                    viewModel.beginChangeEmailProcess(changeEmailLink = link)
                }

                intent.action = null
            }

            ACTION_RESET_PASS -> {
                showConfirmResetPasswordDialog()
                intent.action = null
            }

            ACTION_PASS_CHANGED -> {
                viewModel.finishPasswordChange(
                    intent.getIntExtra(RESULT, API_OK),
                    ::showGeneralAlert,
                    ::showErrorAlert
                )

                intent.action = null
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(KILL_SESSIONS_SHOWN, isAlertDialogShown(killSessionsConfirmationDialog))

        if (isAlertDialogShown(cancelSubscriptionsDialog)) {
            outState.putBoolean(CANCEL_SUBSCRIPTIONS_SHOWN, true)
            outState.putString(TYPED_FEEDBACK, cancelSubscriptionsFeedback)
        }

        outState.putBoolean(
            CONFIRM_CANCEL_SUBSCRIPTIONS_SHOWN,
            isAlertDialogShown(cancelSubscriptionsConfirmationDialog)
        )

        outState.putBoolean(
            CONFIRM_CHANGE_EMAIL_SHOWN,
            isAlertDialogShown(confirmChangeEmailDialog)
        )

        outState.putBoolean(
            CONFIRM_RESET_PASSWORD_SHOWN,
            isAlertDialogShown(confirmResetPasswordDialog)
        )

        super.onSaveInstanceState(outState)
    }

    @Suppress("deprecation")
    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        viewModel.manageActivityResult(requestCode, resultCode, this)
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshAccountInfo()
        refreshMenuOptionsVisibility()
    }

    override fun onPostResume() {
        super.onPostResume()
        try {
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).cancel(
                NOTIFICATION_STORAGE_OVERQUOTA
            )
        } catch (e: Exception) {
            Timber.e(e, "Exception NotificationManager - remove all notifications")
        }
    }

    override fun onDestroy() {
        killSessionsConfirmationDialog?.dismiss()
        cancelSubscriptionsDialog?.dismiss()
        cancelSubscriptionsConfirmationDialog?.dismiss()
        confirmChangeEmailDialog?.dismiss()
        confirmResetPasswordDialog?.dismiss()
        super.onDestroy()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressedDispatcher.onBackPressed()
            R.id.action_kill_all_sessions -> showConfirmationKillSessions()
            R.id.action_change_pass -> navController.navigate(R.id.action_my_account_to_change_password)
            R.id.action_export_MK -> navController.navigate(R.id.action_my_account_to_export_recovery_key)
            R.id.action_refresh -> viewModel.refresh(this)
            R.id.action_upgrade_account -> {
                megaNavigator.openUpgradeAccount(
                    context = this,
                    source = UpgradeAccountSource.MY_ACCOUNT_SCREEN
                )
                viewModel.setOpenUpgradeFrom()
            }

            R.id.action_cancel_subscriptions -> {
                handleShowCancelSubscription()
            }

            R.id.action_logout -> viewModel.logout()
        }

        return super.onOptionsItemSelected(item)
    }

    /**
     * Handles the show cancel subscription action depending on the feature flag.
     */
    private fun handleShowCancelSubscription() {
        Analytics.tracker.trackEvent(CancelSubscriptionMenuToolbarEvent)
        navigateToCancelAccountPlan()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        Analytics.tracker.trackEvent(ToolbarOverflowMenuItemEvent)
        menuInflater.inflate(R.menu.activity_my_account, menu)
        this.menu = menu

        refreshMenuOptionsVisibility()

        return super.onCreateOptionsMenu(menu)
    }

    /**
     * Sets the right Toolbar options depending on current situation.
     */
    private fun refreshMenuOptionsVisibility() {
        val menu = this.menu ?: return

        if (!isOnline(this)) {
            menu.toggleAllMenuItemsVisibility(false)
            return
        }

        when (navController.currentDestination?.id) {
            R.id.my_account -> {
                menu.toggleAllMenuItemsVisibility(true)

                if (!viewModel.isProSubscription()) {
                    menu.findItem(R.id.action_cancel_subscriptions).isVisible = false
                }

                if (isBusinessAccount || isProFlexiAccount) {
                    menu.findItem(R.id.action_upgrade_account).isVisible = false
                }

                updateActionBar(ContextCompat.getColor(this, R.color.grey_020_grey_087))
            }

            else -> {
                menu.toggleAllMenuItemsVisibility(false)
                updateActionBar(ContextCompat.getColor(this, R.color.white))
            }
        }
    }

    private fun setupView() {
        updateInfo()
        setSupportActionBar(binding.toolbar)

        supportActionBar?.apply {
            title = null
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }

        navController =
            (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment)
                .navController

        navController.addOnDestinationChangedListener { _, _, _ ->
            refreshMenuOptionsVisibility()

            supportActionBar?.setHomeAsUpIndicator(
                ColorUtils.tintIcon(
                    this,
                    when (navController.currentDestination?.id) {
                        R.id.my_account -> R.drawable.ic_arrow_back_white
                        else -> R.drawable.ic_close_white
                    }
                )
            )
        }
    }

    /**
     * Updates the action bar by changing the Toolbar and status bar color.
     *
     * @param background Color to set as background.
     */
    private fun updateActionBar(background: Int) {
        if (!isDarkMode(this)) {
            binding.toolbar.setBackgroundColor(background)
        }
    }

    /**
     * Changes the ActionBar elevation depending on the withElevation value received.
     *
     * @param withElevation True if should set elevation, false otherwise.
     */
    private fun changeElevation(withElevation: Boolean) {
        val elevation = resources.getDimension(R.dimen.toolbar_elevation)

        supportActionBar?.elevation = if (withElevation) elevation else 0F
    }

    /**
     * Checks and refreshes account info.
     */
    private fun updateInfo() {
        viewModel.checkVersions()
        viewModel.refreshAccountInfo()
    }

    private fun setupObservers() {
        collectFlow(viewModel.numberOfSubscription) {
            refreshMenuOptionsVisibility()
        }

        viewModel.checkElevation().observe(this, ::changeElevation)

        lifecycleScope.launch {
            viewModel.dialogVisibleState.collect { state ->
                if (state is SubscriptionDialogState.Visible) {
                    state.result.platformInfo?.run {
                        showExistingSubscriptionDialog(this, state.result.typeID)
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.cancelAccountDialogState.collect { state ->
                if (state is CancelAccountDialogState.VisibleDefault) {
                    showConfirmCancelAccountDialog(R.string.delete_account_text_last_step)
                } else if (state is CancelAccountDialogState.VisibleWithSubscription) {
                    showConfirmCancelAccountDialog(R.string.delete_account_text_other_platform_last_step)
                }
            }
        }

        collectFlow(viewModel.state) { state ->
            if (state.openTestPasswordScreenEvent) {
                startActivity(
                    Intent(this, TestPasswordActivity::class.java)
                        .putExtra("logout", true)
                )
                viewModel.resetOpenTestPasswordScreenEvent()
            }
            state.errorMessageRes?.let {
                showErrorAlert(getString(it))
                viewModel.resetErrorMessageRes()
            }
            if (state.errorMessage.isNotBlank()) {
                showErrorAlert(state.errorMessage)
                viewModel.resetErrorMessage()
            }
            if (state.isBusinessAccount || isProFlexiAccount) {
                refreshMenuOptionsVisibility()
            }
            if (state.isProSubscription) {
                refreshMenuOptionsVisibility()
            }
            if (state.showInvalidChangeEmailLinkPrompt) {
                showAlert(
                    this,
                    getString(R.string.account_change_email_error_not_logged_with_correct_account_message),
                    getString(R.string.account_change_email_error_not_logged_with_correct_account_title),
                )
                viewModel.resetInvalidChangeEmailLinkPrompt()
            }
            if (state.showChangeEmailConfirmation) {
                showConfirmChangeEmailDialog()
                viewModel.resetChangeEmailConfirmation()
            }
        }
    }

    private fun navigateToCancelAccountPlan() {
        startActivity(
            Intent(this, CancelAccountPlanActivity::class.java).putExtra(
                CancelAccountPlanActivity.EXTRA_USED_STORAGE, viewModel.getUsedStorage()
            )
        )
    }

    /**
     * Shows a confirmation dialog before kill sessions.
     */
    private fun showConfirmationKillSessions() {
        if (isAlertDialogShown(killSessionsConfirmationDialog)) {
            return
        }

        killSessionsConfirmationDialog = MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.confirmation_close_sessions_title))
            .setMessage(getString(R.string.confirmation_close_sessions_text))
            .setPositiveButton(getString(R.string.contact_accept)) { _, _ ->
                viewModel.killOtherSessions()
            }.setNegativeButton(getString(sharedR.string.general_dialog_cancel_button), null)
            .show()
    }

    /**
     * Shows the dialog to fill before cancel subscriptions.
     */
    private fun showCancelSubscriptions() {
        if (isAlertDialogShown(cancelSubscriptionsDialog)) {
            return
        }

        val builder = MaterialAlertDialogBuilder(this)

        cancelSubscriptionsDialog =
            builder.setView(R.layout.dialog_cancel_subscriptions)
                .setPositiveButton(
                    getString(R.string.send_cancel_subscriptions),
                    null
                )
                .setNegativeButton(getString(R.string.general_dismiss), null)
                .create()

        cancelSubscriptionsDialog?.apply {
            setOnShowListener {
                val feedbackEditText = findViewById<EditText>(R.id.dialog_cancel_feedback)
                feedbackEditText?.apply {
                    setText(cancelSubscriptionsFeedback)

                    doAfterTextChanged {
                        cancelSubscriptionsFeedback = text.toString()
                    }
                }

                getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                    if (cancelSubscriptionsFeedback?.isEmpty() == true) {
                        showSnackbar(getString(R.string.reason_cancel_subscriptions))
                    } else {
                        showConfirmationCancelSubscriptions()
                    }
                }
            }

            show()
        }
    }

    /**
     * Shows a confirmation dialog before cancel subscriptions.
     */
    private fun showConfirmationCancelSubscriptions() {
        if (isAlertDialogShown(cancelSubscriptionsConfirmationDialog)) {
            return
        }

        cancelSubscriptionsConfirmationDialog = MaterialAlertDialogBuilder(this)
            .setMessage(getString(R.string.confirmation_cancel_subscriptions))
            .setPositiveButton(getString(R.string.general_yes)) { _, _ ->
                viewModel.cancelSubscriptions(cancelSubscriptionsFeedback)
            }.setNegativeButton(getString(R.string.general_no), null)
            .show()
    }

    /**
     * create the dialog if there is an active subscription
     * @param platformInfo The information of current subscription platform
     * @param type The type that which kind of dialog will be shown
     */
    private fun showExistingSubscriptionDialog(platformInfo: PlatformInfo, type: Int) {
        val message = when (type) {
            TYPE_ANDROID_PLATFORM,
            TYPE_ANDROID_PLATFORM_NO_NAVIGATION,
                ->
                getString(
                    R.string.message_android_platform_subscription,
                    platformInfo.platformName,
                    platformInfo.platformStoreName
                )

            TYPE_ITUNES ->
                getString(R.string.message_itunes_platform_subscription)

            else ->
                getString(R.string.message_other_platform_subscription)
        }

        MaterialAlertDialogBuilder(this).apply {
            setTitle(
                getString(
                    R.string.title_platform_subscription,
                    platformInfo.platformName
                )
            )
            setMessage(message)
            setPositiveButton(getString(R.string.general_ok)) { dialog, _ ->
                viewModel.setCancelAccountDialogState(isVisible = true)
                dialog.dismiss()
            }
            if (type == TYPE_ANDROID_PLATFORM) {
                setNegativeButton(
                    getString(
                        R.string.button_visit_platform,
                        platformInfo.platformStoreAbbrName
                    )
                ) { dialog, _ ->
                    if (BillingConstant.PAYMENT_GATEWAY ==
                        MegaApiJava.PAYMENT_METHOD_HUAWEI_WALLET && isAppStoreAvailable()
                    ) {
                        startActivity(Intent(BillingConstant.SUBSCRIPTION_LINK_FOR_APP_STORE))
                    } else {
                        startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(BillingConstant.SUBSCRIPTION_LINK_FOR_BROWSER)
                            )
                        )
                    }
                    viewModel.setCancelAccountDialogState(isVisible = true)
                    dialog.dismiss()
                }
            }
            setOnDismissListener {
                /* Clear the value of current state when the dialog is dismissed
                to avoid the dialog open again when the screen is rotated. */
                viewModel.restoreSubscriptionDialogState()
            }
        }
            .create()
            .show()
    }

    private fun showConfirmCancelAccountDialog(messageId: Int) {
        val errorInputBinding = DialogErrorPasswordInputEditTextBinding.inflate(layoutInflater)
        confirmCancelAccountDialog = MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.delete_account))
            .setMessage(getString(messageId))
            .setView(errorInputBinding.root)
            .setNegativeButton(getString(R.string.general_dismiss), null)
            .setPositiveButton(getString(R.string.delete_account), null)
            .setOnDismissListener {
                /* Clear the value of current CancelAccountDialogState when the dialog is dismissed
                to avoid the dialog open again when the screen is rotated. */
                viewModel.restoreCancelAccountDialogState()
            }
            .create()

        showConfirmDialog(
            confirmCancelAccountDialog,
            TYPE_CANCEL_ACCOUNT,
            errorInputBinding.editLayout,
            errorInputBinding.textField,
            errorInputBinding.errorIcon
        )
    }

    private fun showConfirmChangeEmailDialog() {
        val errorInputBinding = DialogErrorPasswordInputEditTextBinding.inflate(layoutInflater)
        confirmChangeEmailDialog = MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.change_mail_title_last_step))
            .setMessage(getString(R.string.change_mail_text_last_step))
            .setView(errorInputBinding.root)
            .setNegativeButton(getString(sharedR.string.general_dialog_cancel_button), null)
            .setPositiveButton(getString(R.string.change_pass), null)
            .create()

        showConfirmDialog(
            confirmChangeEmailDialog,
            TYPE_CHANGE_EMAIL,
            errorInputBinding.editLayout,
            errorInputBinding.textField,
            errorInputBinding.errorIcon
        )
    }

    private fun showConfirmDialog(
        dialog: AlertDialog?,
        dialogType: Int,
        editLayout: TextInputLayout,
        textField: EditText,
        errorIcon: ImageView,
    ) {
        dialog?.apply {
            setOnShowListener {
                quitEditTextError(editLayout, errorIcon)

                editLayout.hint =
                    getString(R.string.edit_text_insert_pass)
                        .replaceFirstChar { it.uppercase(Locale.ROOT) }

                textField.apply {
                    setOnEditorActionListener { _, actionId, _ ->
                        if (actionId == EditorInfo.IME_ACTION_DONE) {
                            getButton(AlertDialog.BUTTON_POSITIVE).performClick()
                        }

                        true
                    }

                    doAfterTextChanged {
                        quitEditTextError(editLayout, errorIcon)
                    }

                    requestFocus()
                    showKeyboardDelayed(this)
                }

                getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    val password = textField.text.toString()

                    if (password.isEmpty()) {
                        setEditTextError(
                            getString(R.string.invalid_string),
                            editLayout,
                            errorIcon
                        )
                    } else {
                        when (dialogType) {
                            TYPE_CANCEL_ACCOUNT -> viewModel.finishAccountCancellation(password)
                            TYPE_CHANGE_EMAIL -> viewModel.finishChangeEmailConfirmation(password)
                        }

                        textField.hideKeyboard()
                        dismiss()
                    }
                }
            }

            show()
        }
    }

    private fun showConfirmResetPasswordDialog() {
        confirmResetPasswordDialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.title_dialog_insert_MK)
            .setMessage(R.string.text_reset_pass_logged_in)
            .setPositiveButton(
                R.string.pin_lock_enter
            ) { _, _ ->
                lifecycleScope.launch {
                    startActivity(
                        Intent(this@MyAccountActivity, ChangePasswordActivity::class.java)
                            .setAction(ACTION_RESET_PASS_FROM_LINK)
                            .setData(intent.data)
                            .putExtra(EXTRA_MASTER_KEY, viewModel.getMasterKey())
                    )
                }
            }
            .setNegativeButton(sharedR.string.general_dialog_cancel_button, null)
            .show()
    }

    private fun showGeneralAlert(message: String) {
        showAlert(this, message, null)
    }

    private fun showErrorAlert(message: String) {
        showAlert(
            this,
            message,
            getString(R.string.general_error_word)
        )
    }

    /**
     * The app store of current platform that app is installed whether is available.
     * @return true is available
     */
    private fun isAppStoreAvailable(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(
                    BillingConstant.SUBSCRIPTION_PLATFORM_PACKAGE_NAME,
                    PackageManager.PackageInfoFlags.of(PackageManager.GET_ACTIVITIES.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(
                    BillingConstant.SUBSCRIPTION_PLATFORM_PACKAGE_NAME,
                    PackageManager.GET_ACTIVITIES
                )
            }
            true
        } catch (exception: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun showSnackbar(text: String) {
        showSnackbar(binding.root, text)
    }

    override fun showSnackbar(type: Int, content: String?, chatId: Long) {
        content?.let { showSnackbar(it) }
    }
}