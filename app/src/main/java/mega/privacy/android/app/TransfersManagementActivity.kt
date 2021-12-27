package mega.privacy.android.app

import android.content.*
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jeremyliao.liveeventbus.LiveEventBus
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.components.transferWidget.TransferWidget
import mega.privacy.android.app.constants.BroadcastConstants.*
import mega.privacy.android.app.constants.EventConstants.EVENT_SHOW_SCANNING_FOLDER_DIALOG
import mega.privacy.android.app.globalmanagement.TransfersManagement.Companion.shouldUpdateScanningFolderDialog
import mega.privacy.android.app.lollipop.ManagerActivityLollipop
import mega.privacy.android.app.lollipop.ManagerActivityLollipop.PENDING_TAB
import mega.privacy.android.app.lollipop.ManagerActivityLollipop.TRANSFERS_TAB
import mega.privacy.android.app.utils.AlertDialogUtil.dismissAlertDialogIfExists
import mega.privacy.android.app.utils.AlertDialogUtil.isAlertDialogShown
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.LogUtil
import mega.privacy.android.app.utils.StringResourcesUtils
import nz.mega.sdk.MegaTransfer
import nz.mega.sdk.MegaTransfer.*

open class TransfersManagementActivity : PasscodeActivity() {

    companion object {
        private const val IS_SCANNING_FOLDER_SHOWN = "IS_SCANNING_FOLDER_SHOWN"
        private const val SCANNING_FOLDER_TAG = "SCANNING_FOLDER_TAG"
    }

    var transfersWidget: TransferWidget? = null

    private var scanningFolderDialog: AlertDialog? = null
    private var scanningFolderTag = INVALID_VALUE

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
            if (savedInstanceState.getBoolean(IS_SCANNING_FOLDER_SHOWN, false)) {
                val tag = savedInstanceState.getInt(SCANNING_FOLDER_TAG, INVALID_VALUE)
                val transfer = megaApi.getTransferByTag(tag)

                if (shouldUpdateScanningFolderDialog(transfer)) {
                    showScanningFolderDialog(transfer)
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(IS_SCANNING_FOLDER_SHOWN, isAlertDialogShown(scanningFolderDialog))
        outState.putInt(SCANNING_FOLDER_TAG, scanningFolderTag)
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

        LiveEventBus.get(EVENT_SHOW_SCANNING_FOLDER_DIALOG, MegaTransfer::class.java)
            .observe(this) { transfer ->
                showScanningFolderDialog(transfer)
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
                if (context is ManagerActivityLollipop) {
                    context.drawerItem = ManagerActivityLollipop.DrawerItem.TRANSFERS
                    context.selectDrawerItemLollipop(context.drawerItem)
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

    /**
     * Creates a scanning folder dialog or updates it if already exists.
     *
     * @param transfer  MegaTransfer to show the scanning folder dialog.
     */
    private fun showScanningFolderDialog(transfer: MegaTransfer) {
        if (isAlertDialogShown(scanningFolderDialog)) {
            scanningFolderDialog?.updateScanningFolderDialog(transfer)
        } else {
            scanningFolderDialog = MaterialAlertDialogBuilder(this)
                .setTitle(StringResourcesUtils.getString(R.string.title_scanning_folder))
                .setView(R.layout.dialog_scanning_folder)
                .setPositiveButton(
                    StringResourcesUtils.getString(R.string.option_cancel_transfer)
                ) { _, _ ->
                    transfersManagement.cancelFolderTransfer(transfer)
                }
                .create()
                .apply {
                    setCancelable(false)
                    setCanceledOnTouchOutside(false)
                    setOnShowListener { updateScanningFolderDialog(transfer) }
                    show()
                }
        }
    }

    /**
     * Updates the scanning folder dialog or dismisses it depending on the transfer stage.
     *
     * @param transfer Folder transfer.
     */
    private fun AlertDialog.updateScanningFolderDialog(transfer: MegaTransfer) {
        val stage = transfer.stage

        if (stage.toInt() == STAGE_TRANSFERRING_FILES) {
            transfersManagement.scanningFolderFinish(transfer)
            dismiss()
            return
        }

        findViewById<TextView>(R.id.transfer_stage)?.text = getFolderTransferStage(stage)

        findViewById<ProgressBar>(R.id.scanning_progress)?.progress =
            getFolderTransferProgress(stage)
    }

    /**
     * Gets the text to show as folder transfer stage.
     *
     * @param stage The folder transfer stage as numeric value.
     * @return The text to show.
     */
    private fun getFolderTransferStage(stage: Long): String? =
        StringResourcesUtils.getString(
            when (stage.toInt()) {
                STAGE_SCAN -> R.string.scanning_folder_stage
                STAGE_CREATE_TREE -> R.string.creating_tree_folder_stage
                STAGE_GEN_TRANSFERS,
                STAGE_PROCESS_TRANSFER_QUEUE -> R.string.starting_transfer_folder_stage
                else -> INVALID_VALUE
            }
        )

    /**
     * Gets the progress to show as per the folder transfer stage.
     *
     * Note that the progress values has been assigned programmatically. This is because currently
     * the SDK does not provide the progress, but only the stage, so we can to do it only like this
     * to fill the design requirements.
     *
     * @param stage The folder transfer stage as numeric value.
     * @return The progress to show.
     */
    private fun getFolderTransferProgress(stage: Long): Int =
        when (stage.toInt()) {
            STAGE_SCAN -> 25
            STAGE_CREATE_TREE -> 50
            STAGE_GEN_TRANSFERS -> 67
            STAGE_PROCESS_TRANSFER_QUEUE -> 84
            else -> INVALID_VALUE
        }


    override fun onResume() {
        super.onResume()

        transfersWidget?.update()
    }

    override fun onDestroy() {
        super.onDestroy()

        unregisterReceiver(transfersUpdateReceiver)
        unregisterReceiver(networkUpdateReceiver)

        dismissAlertDialogIfExists(scanningFolderDialog)
    }

    fun updateWidget() {
        transfersWidget?.update()
    }

    fun hideWidget() {
        transfersWidget?.hide()
    }
}