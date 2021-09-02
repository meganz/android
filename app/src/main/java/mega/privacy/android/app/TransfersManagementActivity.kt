package mega.privacy.android.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.View
import android.widget.RelativeLayout
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.components.transferWidget.TransferWidget
import mega.privacy.android.app.components.transferWidget.TransfersManagement
import mega.privacy.android.app.constants.BroadcastConstants.*
import mega.privacy.android.app.lollipop.ManagerActivityLollipop
import mega.privacy.android.app.lollipop.ManagerActivityLollipop.PENDING_TAB
import mega.privacy.android.app.lollipop.ManagerActivityLollipop.TRANSFERS_TAB
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.LogUtil

open class TransfersManagementActivity : PasscodeActivity() {

    var transfersWidget: TransferWidget? = null

    /**
     * Broadcast to update the transfers widget when a change in network connection is detected.
     */
    private var networkUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != BROADCAST_ACTION_INTENT_CONNECTIVITY_CHANGE) {
                return
            }

            val transfersManagement = MegaApplication.getTransfersManagement()

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

    /**
     * Registers the transfers BroadcastReceivers.
     */
    protected fun registerTransfersReceiver() {
        registerReceiver(
            transfersUpdateReceiver,
            IntentFilter(BROADCAST_ACTION_INTENT_TRANSFER_UPDATE)
        )

        registerReceiver(
            networkUpdateReceiver,
            IntentFilter(BROADCAST_ACTION_INTENT_CONNECTIVITY_CHANGE)
        )
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
        transfersWidget = TransferWidget(this, transfersWidgetLayout)

        transfersWidgetLayout.findViewById<View>(R.id.transfers_button)
            .setOnClickListener {
                if (context is ManagerActivityLollipop) {
                    context.drawerItem = ManagerActivityLollipop.DrawerItem.TRANSFERS
                    context.selectDrawerItemLollipop(context.drawerItem)
                } else {
                    openTransfersSection()
                }

                if (TransfersManagement.isOnTransferOverQuota()) {
                    MegaApplication.getTransfersManagement()
                        .setHasNotToBeShowDueToTransferOverQuota(true)
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
            Intent(this, ManagerActivityLollipop::class.java)
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

    override fun onResume() {
        super.onResume()

        transfersWidget?.update()
    }

    override fun onDestroy() {
        super.onDestroy()

        unregisterReceiver(transfersUpdateReceiver)
        unregisterReceiver(networkUpdateReceiver)
    }
}