package mega.privacy.android.app.presentation.transfers

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jeremyliao.liveeventbus.LiveEventBus
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.UploadService
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.constants.EventConstants.EVENT_SCANNING_TRANSFERS_CANCELLED
import mega.privacy.android.app.constants.EventConstants.EVENT_SHOW_SCANNING_TRANSFERS_DIALOG
import mega.privacy.android.app.main.managerSections.TransfersViewModel
import mega.privacy.android.app.utils.AlertDialogUtil.isAlertDialogShown
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Util
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.exception.QuotaExceededMegaException
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.navigation.MegaNavigator
import timber.log.Timber
import javax.inject.Inject

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

    private var scanningTransfersDialog: AlertDialog? = null
    private var cancelTransfersDialog: AlertDialog? = null

    protected val transfersManagementViewModel: TransfersManagementViewModel by viewModels()
    protected val transfersViewModel: TransfersViewModel by viewModels()

    private var scanningDialogTimer: CountDownTimer? = null

    /**
     * Application Theme Mode
     */
    @Inject
    lateinit var getThemeMode: GetThemeMode

    /**
     * [GetFeatureFlagValueUseCase]
     */
    @Inject
    lateinit var getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase

    /**
     * [MegaNavigator]
     */
    @Inject
    lateinit var navigator: MegaNavigator

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
        collectFlow(transfersManagementViewModel.online) { online ->
            if (online) {
                transfersManagement.resetNetworkTimer()
            } else {
                transfersManagement.startNetworkTimer()
            }
        }

        collectFlow(
            transfersViewModel.monitorTransferEvent,
            Lifecycle.State.CREATED
        ) {
            if (it is TransferEvent.TransferTemporaryErrorEvent) {
                it.error?.let { error ->
                    handleTransferTemporaryError(it.transfer, error)
                }
            }
        }
    }


    private fun handleTransferTemporaryError(transfer: Transfer, error: MegaException) {
        Timber.w("onTransferTemporaryError: ${transfer.nodeHandle} - ${transfer.tag}")
        if (error is QuotaExceededMegaException) {
            if (error.value != 0L) {
                Timber.d("TRANSFER OVER QUOTA ERROR: ${error.errorCode}")
            } else {
                Timber.w("STORAGE OVER QUOTA ERROR: ${error.errorCode}")
                //work around - SDK does not return over quota error for folder upload,
                //so need to be notified from global listener
                if (transfer.transferType == TransferType.GENERAL_UPLOAD) {
                    if (transfer.isForeignOverQuota) return
                    val uploadServiceIntent = Intent(this, UploadService::class.java).apply {
                        action = Constants.ACTION_OVERQUOTA_STORAGE
                    }
                    val isStarted =
                        ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
                    val shouldStartForegroundService =
                        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || isStarted
                    if (shouldStartForegroundService) {
                        ContextCompat.startForegroundService(this, uploadServiceIntent)
                    } else {
                        startService(uploadServiceIntent)
                    }
                }
            }
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
     * Shows a scanning transfers dialog.
     */
    private fun showScanningTransfersDialog() {
        if (isActivityInBackground || isAlertDialogShown(scanningTransfersDialog)) {
            return
        }

        scanningTransfersDialog = MaterialAlertDialogBuilder(this)
            .setView(R.layout.dialog_scanning_transfers)
            .setPositiveButton(
                getString(R.string.cancel_transfers)
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
            .setTitle(getString(R.string.cancel_transfers))
            .setMessage(getString(R.string.warning_cancel_transfers))
            .setPositiveButton(
                getString(R.string.button_proceed)
            ) { _, _ ->
                LiveEventBus.get(EVENT_SCANNING_TRANSFERS_CANCELLED, Boolean::class.java).post(true)
                transfersManagement.cancelScanningTransfers()
                Util.showSnackbar(
                    this,
                    getString(R.string.transfers_cancelled)
                )
            }
            .setNegativeButton(getString(R.string.general_dismiss)) { _, _ ->
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
}
