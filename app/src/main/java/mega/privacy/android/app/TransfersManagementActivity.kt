package mega.privacy.android.app

import android.content.*
import android.os.Bundle
import android.view.View
import android.widget.RelativeLayout
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jeremyliao.liveeventbus.LiveEventBus
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.components.transferWidget.TransferWidget
import mega.privacy.android.app.constants.BroadcastConstants.*
import mega.privacy.android.app.constants.EventConstants.EVENT_SHOW_SCANNING_TRANSFERS_DIALOG
import mega.privacy.android.app.lollipop.DrawerItem
import mega.privacy.android.app.lollipop.ManagerActivity
import mega.privacy.android.app.lollipop.ManagerActivity.PENDING_TAB
import mega.privacy.android.app.lollipop.ManagerActivity.TRANSFERS_TAB
import mega.privacy.android.app.utils.AlertDialogUtil.isAlertDialogShown
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.LogUtil
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.Util

open class TransfersManagementActivity : PasscodeActivity() {

    companion object {
        const val IS_CANCEL_TRANSFERS_SHOWN = "IS_CANCEL_TRANSFERS_SHOWN"
    }

    var transfersWidget: TransferWidget? = null

    private var scanningTransfersDialog: AlertDialog? = null
    private var cancelTransfersDialog: AlertDialog? = null

    /**
     * Broadcast to update the transfers widget when a change in network connection is detected.
     */
    private var networkUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != BROADCAST_ACTION_INTENT_CONNECTIVITY_CHANGE) {
                return
            }

            when (intent.getIntExtra(ACTION_TYPE, INVALID_ACTION)) {
                GO_ONLINE -> transfersManagement.resetNetworkTimer()
                GO_OFFLINE -> transfersManagement.startNetworkTimer()
            }
        }
    }

    /**
     * Broadcast to update the transfers widget.
     */
    private var transfersUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateWidget(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupObservers()

        if (savedInstanceState != null) {
            when {
                savedInstanceState.getBoolean(IS_CANCEL_TRANSFERS_SHOWN, false) -> {
                    showCancelTransfersDialog()
                }
                transfersManagement.shouldShowScanningTransfersDialog() -> {
                    showScanningTransfersDialog()
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(IS_CANCEL_TRANSFERS_SHOWN, isAlertDialogShown(cancelTransfersDialog))
        super.onSaveInstanceState(outState)
    }

    /**
     * Registers the transfers BroadcastReceivers and observers.
     */
    private fun setupObservers() {
        registerReceiver(
            transfersUpdateReceiver,
            IntentFilter(BROADCAST_ACTION_INTENT_TRANSFER_UPDATE)
        )

        registerReceiver(
            networkUpdateReceiver,
            IntentFilter(BROADCAST_ACTION_INTENT_CONNECTIVITY_CHANGE)
        )

        LiveEventBus.get(EVENT_SHOW_SCANNING_TRANSFERS_DIALOG, Boolean::class.java)
            .observe(this) { show ->
                when {
                    show && transfersManagement.shouldShowScanningTransfersDialog() -> {
                        showScanningTransfersDialog()
                    }
                    !show && !transfersManagement.shouldShowScanningTransfersDialog() -> {
                        scanningTransfersDialog?.dismiss()
                    }
                }
            }
    }

    /**
     * Sets a view as transfers widget.
     *
     * @param transfersWidgetLayout RelativeLayout view to set
     */
    protected fun setTransfersWidgetLayout(transfersWidgetLayout: RelativeLayout) {
        setTransfersWidgetLayout(transfersWidgetLayout, null)
    }

    /**
     * Sets a view as transfers widget.
     *
     * @param transfersWidgetLayout RelativeLayout view to set
     * @param context               Current Context.
     *                              Only used to identify if the view belongs to the ManagerActivity.
     */
    protected fun setTransfersWidgetLayout(
        transfersWidgetLayout: RelativeLayout,
        context: Context?
    ) {
        transfersWidget = TransferWidget(this, transfersWidgetLayout, transfersManagement)

        transfersWidgetLayout.findViewById<View>(R.id.transfers_button)
            .setOnClickListener {
                if (context is ManagerActivity) {
                    context.drawerItem = DrawerItem.TRANSFERS
                    context.selectDrawerItem(context.drawerItem)
                } else {
                    openTransfersSection()
                }

                if (transfersManagement.isOnTransferOverQuota()) {
                    transfersManagement.setHasNotToBeShowDueToTransferOverQuota(true)
                }
            }
    }

    /**
     * Defines the click action of the transfers widget.
     * Launches an Intent to navigate to In progress tab in Transfers section.
     */
    protected fun openTransfersSection() {
        if (megaApi.isLoggedIn == 0 || dbH.credentials == null) {
            LogUtil.logWarning("No logged in, no action.")
            return
        }

        startActivity(
            Intent(this, ManagerActivity::class.java)
                .setAction(ACTION_SHOW_TRANSFERS)
                .putExtra(TRANSFERS_TAB, PENDING_TAB)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )

        finish()
    }

    /**
     * Updates the state of the transfers widget when the correspondent LocalBroadcast has been received.
     *
     * @param intent    Intent received in the LocalBroadcast
     */
    protected fun updateWidget(intent: Intent?) {
        if (intent == null || transfersWidget == null) {
            return
        }

        val transferType = intent.getIntExtra(TRANSFER_TYPE, EXTRA_BROADCAST_INVALID_VALUE)

        if (transferType == EXTRA_BROADCAST_INVALID_VALUE) {
            transfersWidget?.update()
        } else {
            transfersWidget?.update(transferType)
        }
    }

    /**
     * Shows a scanning transfers dialog.
     */
    private fun showScanningTransfersDialog() {
        if (isAlertDialogShown(scanningTransfersDialog)) {
            return
        }

        scanningTransfersDialog = MaterialAlertDialogBuilder(this)
            .setView(R.layout.dialog_scanning_transfers)
            .setPositiveButton(
                StringResourcesUtils.getString(R.string.cancel_transfers)
            ) { _, _ ->
                showCancelTransfersDialog()
            }
            .create()
            .apply {
                setCancelable(false)
                setCanceledOnTouchOutside(false)
                show()
            }
    }

    /**
     * Shows a confirmation dialog before cancel all scanning transfers.
     */
    private fun showCancelTransfersDialog() {
        cancelTransfersDialog = MaterialAlertDialogBuilder(this)
            .setTitle(StringResourcesUtils.getString(R.string.cancel_transfers))
            .setMessage(StringResourcesUtils.getString(R.string.warning_cancel_transfers))
            .setPositiveButton(
                StringResourcesUtils.getString(R.string.button_proceed)
            ) { _, _ ->
                transfersManagement.cancelScanningTransfers()
                Util.showSnackbar(this, StringResourcesUtils.getString(R.string.transfers_cancelled))
            }
            .setNegativeButton(StringResourcesUtils.getString(R.string.general_dismiss), null)
            .create()
            .apply {
                setCancelable(false)
                setCanceledOnTouchOutside(false)
                show()
            }
    }

    override fun onResume() {
        super.onResume()

        transfersWidget?.update()
    }

    override fun onDestroy() {
        super.onDestroy()

        unregisterReceiver(transfersUpdateReceiver)
        unregisterReceiver(networkUpdateReceiver)

        scanningTransfersDialog?.dismiss()
        cancelTransfersDialog?.dismiss()
    }

    fun updateWidget() {
        transfersWidget?.update()
    }

    fun hideWidget() {
        transfersWidget?.hide()
    }
}