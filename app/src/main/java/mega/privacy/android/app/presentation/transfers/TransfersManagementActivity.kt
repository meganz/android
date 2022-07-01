package mega.privacy.android.app.presentation.transfers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import android.widget.RelativeLayout
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jeremyliao.liveeventbus.LiveEventBus
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.components.transferWidget.TransfersWidget
import mega.privacy.android.app.components.transferWidget.TransfersWidget.Companion.NO_TYPE
import mega.privacy.android.app.constants.EventConstants.EVENT_SCANNING_TRANSFERS_CANCELLED
import mega.privacy.android.app.constants.EventConstants.EVENT_SHOW_SCANNING_TRANSFERS_DIALOG
import mega.privacy.android.app.constants.EventConstants.EVENT_TRANSFER_UPDATE
import mega.privacy.android.app.main.DrawerItem
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.main.ManagerActivity.PENDING_TAB
import mega.privacy.android.app.main.ManagerActivity.TRANSFERS_TAB
import mega.privacy.android.app.usecase.GetNetworkConnectionUseCase
import mega.privacy.android.app.utils.AlertDialogUtil.isAlertDialogShown
import mega.privacy.android.app.utils.Constants.ACTION_SHOW_TRANSFERS
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.Util
import timber.log.Timber
import javax.inject.Inject

/**
 * Activity for showing concrete UI items related to transfers management.
 */
@AndroidEntryPoint
open class TransfersManagementActivity : PasscodeActivity() {

    companion object {
        const val IS_CANCEL_TRANSFERS_SHOWN = "IS_CANCEL_TRANSFERS_SHOWN"
    }

    @Inject
    lateinit var getNetworkConnectionUseCase: GetNetworkConnectionUseCase

    private var transfersWidget: TransfersWidget? = null

    private var scanningTransfersDialog: AlertDialog? = null
    private var cancelTransfersDialog: AlertDialog? = null

    val transfersViewModel: TransfersManagementViewModel by viewModels()

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
        getNetworkConnectionUseCase.getConnectionUpdates()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onNext = { online ->
                    if (online) {
                        transfersManagement.resetNetworkTimer()
                    } else {
                        transfersManagement.startNetworkTimer()
                    }
                },
                onError = { error -> Timber.e(error, "Network update error") }
            )
            .addTo(composite)

        LiveEventBus.get(EVENT_TRANSFER_UPDATE, Int::class.java)
            .observe(this, ::updateTransfersWidget)

        LiveEventBus.get(EVENT_SHOW_SCANNING_TRANSFERS_DIALOG, Boolean::class.java)
            .observeForever(::onShowScanningTransfersDialog)

        transfersViewModel.onTransfersInfoUpdate().observe(this) { (transferType, transfersInfo) ->
            transfersWidget?.update(transferType = transferType, transfersInfo = transfersInfo)
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
                showScanningTransfersDialog()
            }
            !show && !transfersManagement.shouldShowScanningTransfersDialog() -> {
                scanningTransfersDialog?.dismiss()
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
                .putExtra(TRANSFERS_TAB, PENDING_TAB)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )

        finish()
    }

    /**
     * Updates the state of the transfers widget.
     *
     * @param transferType  Type of the transfer.
     */
    protected fun updateTransfersWidget(transferType: Int) {
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
            .removeObserver(::onShowScanningTransfersDialog)
    }

    override fun onResume() {
        super.onResume()

        if (transfersManagement.shouldShowScanningTransfersDialog()) {
            showScanningTransfersDialog()
        }

        updateTransfersWidget()
    }

    override fun onPostResume() {
        super.onPostResume()

        LiveEventBus.get(EVENT_SHOW_SCANNING_TRANSFERS_DIALOG, Boolean::class.java)
            .observeForever(::onShowScanningTransfersDialog)
    }

    override fun onDestroy() {
        super.onDestroy()

        scanningTransfersDialog?.dismiss()
        cancelTransfersDialog?.dismiss()
    }

    /**
     * Updates the transfers widget.
     */
    fun updateTransfersWidget() {
        transfersViewModel.checkTransfersInfo(NO_TYPE)
    }

    /**
     * Updates the state of the transfers widget.
     */
    fun updateTransfersWidgetState() {
        transfersWidget?.updateState()
    }

    /**
     * Hides the transfers widget.
     */
    fun hideTransfersWidget() {
        transfersWidget?.hide()
    }
}