package mega.privacy.android.app.presentation.transfers

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.RelativeLayout
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jeremyliao.liveeventbus.LiveEventBus
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.components.transferWidget.TransfersWidget
import mega.privacy.android.app.constants.EventConstants.EVENT_SCANNING_TRANSFERS_CANCELLED
import mega.privacy.android.app.constants.EventConstants.EVENT_SHOW_SCANNING_TRANSFERS_DIALOG
import mega.privacy.android.app.main.DrawerItem
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.main.ManagerActivity.TRANSFERS_TAB
import mega.privacy.android.app.presentation.manager.model.TransfersTab
import mega.privacy.android.app.utils.AlertDialogUtil.isAlertDialogShown
import mega.privacy.android.app.utils.Constants.ACTION_SHOW_TRANSFERS
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.domain.entity.transfer.TransferType
import timber.log.Timber

/**
 * Activity for showing concrete UI items related to transfers management.
 */
@AndroidEntryPoint
open class TransfersManagementActivity : PasscodeActivity() {

    companion object {
        private const val IS_CANCEL_TRANSFERS_SHOWN = "IS_CANCEL_TRANSFERS_SHOWN"
        private const val SHOW_SCANNING_DIALOG_TIMER = 800L
        private const val HIDE_SCANNING_DIALOG_TIMER = 1200L
    }

    private var transfersWidget: TransfersWidget? = null

    private var scanningTransfersDialog: AlertDialog? = null
    private var cancelTransfersDialog: AlertDialog? = null

    val transfersViewModel: TransfersManagementViewModel by viewModels()

    private var scanningDialogTimer: CountDownTimer? = null

    private val showScanTransferDialogObserver = Observer<Boolean> { show ->
        onShowScanningTransfersDialog(show)
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
        collectFlow(transfersViewModel.online) { online ->
            if (online) {
                transfersManagement.resetNetworkTimer()
            } else {
                transfersManagement.startNetworkTimer()
            }
        }

        transfersViewModel.onTransfersInfoUpdate().observe(this) { transfersInfo ->
            transfersWidget?.update(transfersInfo = transfersInfo)
        }

        transfersViewModel.onGetTransfersState().observe(this) { areAllTransfersPaused ->
            transfersWidget?.updateState(areAllTransfersPaused)
        }
    }

    /**
     * Shows or hides the scanning dialog.
     *
     * @param show  True if should show the dialog, false if should hide it.
     */
    private fun onShowScanningTransfersDialog(show: Boolean) {
        when {
            show && transfersManagement.shouldShowScanningTransfersDialog() -> {
                startShowScanningDialogTimer()
            }
            !show && !transfersManagement.shouldShowScanningTransfersDialog() -> {
                if (scanningDialogTimer == null) {
                    scanningTransfersDialog?.dismiss()
                }
            }
        }
    }

    /**
     * Starts a [CountDownTimer] in order to show the scanning dialog only if the processing time
     * exceeds [SHOW_SCANNING_DIALOG_TIMER].
     */
    private fun startShowScanningDialogTimer() {
        if (scanningDialogTimer == null) {
            scanningDialogTimer =
                object : CountDownTimer(SHOW_SCANNING_DIALOG_TIMER, SHOW_SCANNING_DIALOG_TIMER) {
                    override fun onTick(millisUntilFinished: Long) {}

                    override fun onFinish() {
                        scanningDialogTimer = null
                        if (transfersManagement.shouldShowScanningTransfersDialog()) {
                            showScanningTransfersDialog()
                            startHideScanningDialogTimer()
                        }
                    }
                }.start()
        }
    }

    /**
     * Starts a [CountDownTimer] in order to show the scanning dialog at least
     * [HIDE_SCANNING_DIALOG_TIMER].
     */
    private fun startHideScanningDialogTimer() {
        scanningDialogTimer =
            object : CountDownTimer(HIDE_SCANNING_DIALOG_TIMER, HIDE_SCANNING_DIALOG_TIMER) {
                override fun onTick(millisUntilFinished: Long) {}

                override fun onFinish() {
                    scanningDialogTimer = null
                    if (!transfersManagement.shouldShowScanningTransfersDialog()) {
                        scanningTransfersDialog?.dismiss()
                    }
                }
            }.start()
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
        context: Context?,
    ) {
        transfersWidget =
            TransfersWidget(context ?: this, megaApi, transfersWidgetLayout, transfersManagement)

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
            Timber.w("Not logged in, no action.")
            return
        }

        startActivity(
            Intent(this, ManagerActivity::class.java)
                .setAction(ACTION_SHOW_TRANSFERS)
                .putExtra(TRANSFERS_TAB, TransfersTab.PENDING_TAB)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )

        finish()
    }

    /**
     * Updates the state of the transfers widget.
     *
     * @param transferType Type of the transfer.
     */
    protected fun updateTransfersWidget(transferType: TransferType) {
        if (transfersManagement.isProcessingTransfers || transfersManagement.isProcessingFolders) {
            return
        }

        transfersViewModel.checkTransfersInfo(transferType)
    }

    /**
     * Shows a scanning transfers dialog.
     */
    private fun showScanningTransfersDialog() {
        if (isActivityInBackground || isAlertDialogShown(scanningTransfersDialog)) {
            return
        }

        scanningTransfersDialog = MaterialAlertDialogBuilder(this)
            .setView(R.layout.dialog_scanning_transfers)
            .setPositiveButton(
                StringResourcesUtils.getString(R.string.cancel_transfers)
            ) { _, _ ->
                if (transfersManagement.shouldShowScanningTransfersDialog()) {
                    showCancelTransfersDialog()
                }
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
        if (isActivityInBackground || isAlertDialogShown(cancelTransfersDialog)) {
            return
        }

        cancelTransfersDialog = MaterialAlertDialogBuilder(this)
            .setTitle(StringResourcesUtils.getString(R.string.cancel_transfers))
            .setMessage(StringResourcesUtils.getString(R.string.warning_cancel_transfers))
            .setPositiveButton(
                StringResourcesUtils.getString(R.string.button_proceed)
            ) { _, _ ->
                LiveEventBus.get(EVENT_SCANNING_TRANSFERS_CANCELLED, Boolean::class.java).post(true)
                transfersManagement.cancelScanningTransfers()
                Util.showSnackbar(
                    this,
                    StringResourcesUtils.getString(R.string.transfers_cancelled)
                )
            }
            .setNegativeButton(StringResourcesUtils.getString(R.string.general_dismiss)) { _, _ ->
                if (transfersManagement.shouldShowScanningTransfersDialog()) {
                    showScanningTransfersDialog()
                }
            }
            .create()
            .apply {
                setCancelable(false)
                setCanceledOnTouchOutside(false)
                show()
            }
    }

    override fun onPause() {
        super.onPause()

        LiveEventBus.get(EVENT_SHOW_SCANNING_TRANSFERS_DIALOG, Boolean::class.java)
            .removeObserver(showScanTransferDialogObserver)
    }

    override fun onResume() {
        super.onResume()
        updateTransfersWidget()
    }

    override fun onPostResume() {
        super.onPostResume()

        LiveEventBus.get(EVENT_SHOW_SCANNING_TRANSFERS_DIALOG, Boolean::class.java)
            .observeForever(showScanTransferDialogObserver)
    }

    override fun onDestroy() {
        super.onDestroy()

        scanningTransfersDialog?.dismiss()
        cancelTransfersDialog?.dismiss()
        LiveEventBus.get(EVENT_SHOW_SCANNING_TRANSFERS_DIALOG, Boolean::class.java)
            .removeObserver(showScanTransferDialogObserver)
    }

    /**
     * Updates the transfers widget.
     */
    fun updateTransfersWidget() {
        transfersViewModel.checkTransfersInfo(TransferType.NONE)
    }

    /**
     * Updates the state of the transfers widget.
     */
    fun updateTransfersWidgetState() {
        transfersViewModel.checkTransfersState()
    }

    /**
     * Hides the transfers widget.
     */
    fun hideTransfersWidget() {
        transfersWidget?.hide()
    }
}