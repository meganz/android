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
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.android.synthetic.main.dialog_general_confirmation.*
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.myAccount.fragments.MyAccountFragment
import mega.privacy.android.app.constants.BroadcastConstants
import mega.privacy.android.app.utils.AlertsAndWarnings.dismissAlertDialogIfShown
import mega.privacy.android.app.utils.AlertsAndWarnings.isAlertDialogShown
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.LogUtil
import mega.privacy.android.app.utils.MenuUtils.toggleAllMenuItemsVisibility
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.Util.isOnline
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaError.API_OK
import org.jetbrains.anko.contentView

class MyAccountActivity : PasscodeActivity() {

    companion object {
        private const val KILL_SESSIONS_SHOWN = "KILL_SESSIONS_SHOWN"
        private const val CANCEL_SUBSCRIPTIONS_SHOWN = "CANCEL_SUBSCRIPTIONS_SHOWN"
        private const val TYPED_FEEDBACK = "TYPED_FEEDBACK"
        private const val CONFIRM_CANCEL_SUBSCRIPTIONS_SHOWN = "CONFIRM_CANCEL_SUBSCRIPTIONS_SHOWN"
    }

    private val viewModel: MyAccountViewModel by viewModels()

    private lateinit var myAccountFragment: MyAccountFragment

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
                UPDATE_ACCOUNT_DETAILS -> if (this@MyAccountActivity::myAccountFragment.isInitialized) {
                    myAccountFragment.setUpAccountDetails()
                }
                UPDATE_CREDIT_CARD_SUBSCRIPTION -> refreshMenuOptionsVisibility()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_account)

        updateInfo()
        setUpObservers()

        if (savedInstanceState == null) {
            myAccountFragment = MyAccountFragment()
            supportFragmentManager.beginTransaction()
                .add(R.id.container, myAccountFragment)
                .commit()
        } else {
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
        viewModel.manageActivityResult(requestCode, resultCode, data)
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
            LogUtil.logError("Exception NotificationManager - remove all notifications", e)
        }
    }

    override fun onDestroy() {
        unregisterReceiver(updateMyAccountReceiver)

        dismissAlertDialogIfShown(killSessionsConfirmationDialog)
        dismissAlertDialogIfShown(cancelSubscriptionsDialog)
        dismissAlertDialogIfShown(cancelSubscriptionsConfirmationDialog)
        super.onDestroy()
    }

    override fun onBackPressed() {
        if (viewModel.isMyAccountFragment()) {
            super.onBackPressed()
        }
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

        if (viewModel.isMyAccountFragment()) {
            menu.toggleAllMenuItemsVisibility(true)

            if (viewModel.thereIsNoSubscription()) {
                menu.findItem(R.id.action_cancel_subscriptions).isVisible = false
            }

            if (megaApi.isBusinessAccount) {
                menu.findItem(R.id.action_upgrade_account).isVisible = false
            }
        } else {
            menu.toggleAllMenuItemsVisibility(false)
            menu.findItem(R.id.action_refresh).isVisible = true
            menu.findItem(R.id.action_logout).isVisible = true
        }
    }

    private fun updateInfo() {
        viewModel.checkVersions()
        app.refreshAccountInfo()
    }

    private fun setUpObservers() {
        registerReceiver(
            updateMyAccountReceiver, IntentFilter(
                BROADCAST_ACTION_INTENT_UPDATE_ACCOUNT_DETAILS
            )
        )

        viewModel.onKillSessionsFinished().observe(this, ::showKillSessionsResult)
        viewModel.onCancelSubscriptions().observe(this, ::showCancelSubscriptionsResult)
    }

    private fun showKillSessionsResult(error: MegaError) {
        showSnackbar(
            StringResourcesUtils.getString(
                if (error.errorCode == API_OK) R.string.success_kill_all_sessions
                else R.string.error_kill_all_sessions
            )
        )
    }

    private fun showCancelSubscriptionsResult(error: MegaError) {
        showSnackbar(
            StringResourcesUtils.getString(
                if (error.errorCode == API_OK) R.string.cancel_subscription_ok
                else R.string.cancel_subscription_error
            )
        )

        app.askForCCSubscriptions()
    }

    private fun showConfirmationKillSessions() {
        if (isAlertDialogShown(killSessionsConfirmationDialog)) {
            return
        }

        killSessionsConfirmationDialog = MaterialAlertDialogBuilder(this)
            .setTitle(StringResourcesUtils.getString(R.string.confirmation_close_sessions_title))
            .setMessage(StringResourcesUtils.getString(R.string.confirmation_close_sessions_text))
            .setPositiveButton(StringResourcesUtils.getString(R.string.contact_accept)) { _, _ ->
                viewModel.killSessions()
            }.setNegativeButton(StringResourcesUtils.getString(R.string.general_cancel), null)
            .show()
    }

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

    private fun showConfirmationCancelSubscriptions() {
        if (isAlertDialogShown(cancelSubscriptionsConfirmationDialog)) {
            return
        }

        cancelSubscriptionsConfirmationDialog = MaterialAlertDialogBuilder(this)
            .setMessage(StringResourcesUtils.getString(R.string.confirmation_cancel_subscriptions))
            .setPositiveButton(StringResourcesUtils.getString(R.string.general_yes)) { _, _ ->
                viewModel.cancelSubscriptions(cancelSubscriptionsFeedback!!)
            }.setNegativeButton(StringResourcesUtils.getString(R.string.general_no), null)
            .show()
    }

    fun showSnackbar(text: String) {
        showSnackbar(contentView?.findViewById(R.id.container), text)
    }
}