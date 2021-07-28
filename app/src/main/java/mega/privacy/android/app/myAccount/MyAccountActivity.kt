package mega.privacy.android.app.myAccount

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.android.synthetic.main.activity_my_account.*
import kotlinx.android.synthetic.main.dialog_general_confirmation.*
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.constants.BroadcastConstants
import mega.privacy.android.app.constants.IntentConstants.Companion.EXTRA_ACCOUNT_TYPE
import mega.privacy.android.app.databinding.ActivityMyAccountBinding
import mega.privacy.android.app.upgradeAccount.UpgradeAccountActivity
import mega.privacy.android.app.utils.AlertDialogUtil.isAlertDialogShown
import mega.privacy.android.app.utils.ColorUtils
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.LogUtil.logError
import mega.privacy.android.app.utils.MenuUtils.toggleAllMenuItemsVisibility
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.Util.isOnline
import org.jetbrains.anko.contentView

class MyAccountActivity : PasscodeActivity(), MyAccountFragment.MessageResultCallback {

    companion object {
        private const val KILL_SESSIONS_SHOWN = "KILL_SESSIONS_SHOWN"
        private const val CANCEL_SUBSCRIPTIONS_SHOWN = "CANCEL_SUBSCRIPTIONS_SHOWN"
        private const val TYPED_FEEDBACK = "TYPED_FEEDBACK"
        private const val CONFIRM_CANCEL_SUBSCRIPTIONS_SHOWN = "CONFIRM_CANCEL_SUBSCRIPTIONS_SHOWN"
    }

    private val viewModel: MyAccountViewModel by viewModels()

    private lateinit var navController: NavController
    private lateinit var binding: ActivityMyAccountBinding

    private var menu: Menu? = null

    private var killSessionsConfirmationDialog: AlertDialog? = null
    private var cancelSubscriptionsDialog: AlertDialog? = null
    private var cancelSubscriptionsConfirmationDialog: AlertDialog? = null

    private var cancelSubscriptionsFeedback: String? = null

    private val updateMyAccountReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val actionType = intent.getIntExtra(
                BroadcastConstants.ACTION_TYPE,
                BroadcastConstants.INVALID_ACTION
            )

            when (actionType) {
                UPDATE_ACCOUNT_DETAILS -> viewModel.updateAccountDetails()
                UPDATE_CREDIT_CARD_SUBSCRIPTION -> refreshMenuOptionsVisibility()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupView()
        setupObservers()

        val accountType = intent.getIntExtra(EXTRA_ACCOUNT_TYPE, INVALID_VALUE)
        if (accountType != INVALID_VALUE) {
            startActivity(
                Intent(this, UpgradeAccountActivity::class.java)
                    .putExtra(EXTRA_ACCOUNT_TYPE, accountType)
            )

            intent.removeExtra(EXTRA_ACCOUNT_TYPE)
        }

        if (savedInstanceState != null) {
            when {
                savedInstanceState.getBoolean(KILL_SESSIONS_SHOWN, false) -> {
                    showConfirmationKillSessions()
                }
                savedInstanceState.getBoolean(CANCEL_SUBSCRIPTIONS_SHOWN, false) -> {
                    cancelSubscriptionsFeedback = savedInstanceState.getString(TYPED_FEEDBACK)
                    showCancelSubscriptions()
                }
                savedInstanceState.getBoolean(CONFIRM_CANCEL_SUBSCRIPTIONS_SHOWN, false) -> {
                    showConfirmationCancelSubscriptions()
                }
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

        super.onSaveInstanceState(outState)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val error = viewModel.manageActivityResult(this, requestCode, resultCode, data)
        if (!error.isNullOrEmpty()) showSnackbar(error)
    }

    override fun onBackPressed() {
        if (!navController.navigateUp()) {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        app.refreshAccountInfo()
    }

    override fun onPostResume() {
        super.onPostResume()
        try {
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).cancel(
                NOTIFICATION_STORAGE_OVERQUOTA
            )
        } catch (e: Exception) {
            logError("Exception NotificationManager - remove all notifications", e)
        }
    }

    override fun onDestroy() {
        unregisterReceiver(updateMyAccountReceiver)

        killSessionsConfirmationDialog?.dismiss()
        cancelSubscriptionsDialog?.dismiss()
        cancelSubscriptionsConfirmationDialog?.dismiss()
        super.onDestroy()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
            R.id.action_kill_all_sessions -> showConfirmationKillSessions()
            R.id.action_change_pass -> viewModel.changePassword(this)
            R.id.action_export_MK -> viewModel.exportMK(this)
            R.id.action_refresh -> viewModel.refresh(this)
            R.id.action_upgrade_account -> viewModel.upgradeAccount(this)
            R.id.action_cancel_subscriptions -> showCancelSubscriptions()
            R.id.action_logout -> viewModel.logout(this)
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
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

                if (viewModel.thereIsNoSubscription()) {
                    menu.findItem(R.id.action_cancel_subscriptions).isVisible = false
                }

                if (megaApi.isBusinessAccount) {
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
        if (!Util.isDarkMode(this)) {
            window?.statusBarColor = background
            toolbar.setBackgroundColor(background)
        }
    }

    /**
     * Changes the ActionBar elevation depending on the withElevation value received.
     *
     * @param withElevation True if should set elevation, false otherwise.
     */
    private fun changeElevation(withElevation: Boolean) {
        val isDark = Util.isDarkMode(this)
        val darkAndElevation = withElevation && isDark
        val background = ContextCompat.getColor(this, R.color.grey_020_grey_087)

        if (darkAndElevation) {
            ColorUtils.changeStatusBarColorForElevation(this, true)
        } else {
            window?.statusBarColor = background
        }

        val elevation = resources.getDimension(R.dimen.toolbar_elevation)

        binding.toolbar.setBackgroundColor(
            if (darkAndElevation) ColorUtils.getColorForElevation(this, elevation)
            else background
        )

        supportActionBar?.elevation =
            if (withElevation && !isDark) elevation else 0F
    }

    /**
     * Checks and refreshes account info.
     */
    private fun updateInfo() {
        viewModel.checkVersions()
        app.refreshAccountInfo()
    }

    private fun setupObservers() {
        registerReceiver(
            updateMyAccountReceiver, IntentFilter(
                BROADCAST_ACTION_INTENT_UPDATE_ACCOUNT_DETAILS
            )
        )

        viewModel.checkElevation().observe(this, ::changeElevation)
    }

    /**
     * Shows the result of the kill sessions action.
     *
     * @param success True if the request finishes with success, false otherwise.
     */
    private fun showKillSessionsResult(success: Boolean) {
        showSnackbar(
            StringResourcesUtils.getString(
                if (success) R.string.success_kill_all_sessions
                else R.string.error_kill_all_sessions
            )
        )
    }

    /**
     * Shows the result of the cancel subscriptions action.
     *
     * @param success True if the request finishes with success, false otherwise.
     */
    private fun showCancelSubscriptionsResult(success: Boolean) {
        showSnackbar(
            StringResourcesUtils.getString(
                if (success) R.string.cancel_subscription_ok
                else R.string.cancel_subscription_error
            )
        )

        app.askForCCSubscriptions()
    }

    /**
     * Shows a confirmation dialog before kill sessions.
     */
    private fun showConfirmationKillSessions() {
        if (isAlertDialogShown(killSessionsConfirmationDialog)) {
            return
        }

        killSessionsConfirmationDialog = MaterialAlertDialogBuilder(this)
            .setTitle(StringResourcesUtils.getString(R.string.confirmation_close_sessions_title))
            .setMessage(StringResourcesUtils.getString(R.string.confirmation_close_sessions_text))
            .setPositiveButton(StringResourcesUtils.getString(R.string.contact_accept)) { _, _ ->
                viewModel.killSessions { success -> showKillSessionsResult(success) }
            }.setNegativeButton(StringResourcesUtils.getString(R.string.general_cancel), null)
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
                    StringResourcesUtils.getString(R.string.send_cancel_subscriptions),
                    null
                )
                .setNegativeButton(StringResourcesUtils.getString(R.string.general_dismiss), null)
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

                this.positive_button.setOnClickListener {
                    if (cancelSubscriptionsFeedback?.isEmpty() == true) {
                        showSnackbar(StringResourcesUtils.getString(R.string.reason_cancel_subscriptions))
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
            .setMessage(StringResourcesUtils.getString(R.string.confirmation_cancel_subscriptions))
            .setPositiveButton(StringResourcesUtils.getString(R.string.general_yes)) { _, _ ->
                viewModel.cancelSubscriptions(cancelSubscriptionsFeedback) { success ->
                    showCancelSubscriptionsResult(success)
                }
            }.setNegativeButton(StringResourcesUtils.getString(R.string.general_no), null)
            .show()
    }

    fun showSnackbar(text: String) {
        showSnackbar(contentView?.findViewById(R.id.container), text)
    }

    override fun show(message: String) {
        showSnackbar(message)
    }
}