package mega.privacy.android.app.presentation.transfers

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jeremyliao.liveeventbus.LiveEventBus
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.UploadService
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.components.transferWidget.TransfersWidgetView
import mega.privacy.android.app.constants.EventConstants.EVENT_SCANNING_TRANSFERS_CANCELLED
import mega.privacy.android.app.constants.EventConstants.EVENT_SHOW_SCANNING_TRANSFERS_DIALOG
import mega.privacy.android.app.main.DrawerItem
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.main.ManagerActivity.Companion.TRANSFERS_TAB
import mega.privacy.android.app.main.managerSections.TransfersViewModel
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.manager.model.TransfersTab
import mega.privacy.android.app.utils.AlertDialogUtil.isAlertDialogShown
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.ACTION_SHOW_TRANSFERS
import mega.privacy.android.app.utils.Util
import mega.privacy.android.shared.theme.MegaAppTheme
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.exception.QuotaExceededMegaException
import mega.privacy.android.domain.usecase.GetThemeMode
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
        private const val animationDuration = 300
        private const val animationScale = 0.2f
        private val animationSpecs = TweenSpec<Float>(durationMillis = animationDuration)
    }

    private var transfersWidget: ComposeView? = null

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

        collectFlow(monitorTransferOverQuotaUseCase(), Lifecycle.State.CREATED) {
            updateTransfersWidget(TransferType.NONE)
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

    /**
     * Checks if the widget is on a file management section in ManagerActivity.
     *
     * @return True if the widget is on a file management section (in ManagerActivity), false otherwise.
     */
    protected open val isOnFileManagementManagerSection = false


    private fun handleTransferTemporaryError(transfer: Transfer, error: MegaException) {
        Timber.w("onTransferTemporaryError: ${transfer.nodeHandle} - ${transfer.tag}")
        if (error is QuotaExceededMegaException) {
            if (error.value != 0L) {
                Timber.d("TRANSFER OVER QUOTA ERROR: ${error.errorCode}")
                updateTransfersWidget()
            } else {
                Timber.w("STORAGE OVER QUOTA ERROR: ${error.errorCode}")
                //work around - SDK does not return over quota error for folder upload,
                //so need to be notified from global listener
                if (transfer.transferType.isUploadType()) {
                    if (transfer.isForeignOverQuota) return
                    val uploadServiceIntent = Intent(this, UploadService::class.java).apply {
                        action = Constants.ACTION_OVERQUOTA_STORAGE
                    }
                    val isStarted =
                        ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
                    val shouldStartForegroundService =
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                                (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || isStarted)
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
     * Sets a view as transfers widget.
     *
     * @param transfersWidget RelativeLayout view to set
     */
    protected fun setTransfersWidgetLayout(transfersWidget: ComposeView) {
        this.transfersWidget = transfersWidget
        transfersWidget.setContent {
            val themeMode by getThemeMode()
                .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            val uiState by transfersManagementViewModel.state.collectAsStateWithLifecycle(
                TransferManagementUiState()
            )
            MegaAppTheme(isDark = themeMode.isDarkMode()) {
                @OptIn(ExperimentalAnimationApi::class)
                AnimatedVisibility(
                    visible = uiState.widgetVisible,
                    enter = scaleIn(animationSpecs, initialScale = animationScale) + fadeIn(
                        animationSpecs
                    ),
                    exit = scaleOut(animationSpecs, targetScale = animationScale) + fadeOut(
                        animationSpecs
                    ),
                ) {
                    TransfersWidgetView(
                        transfersData = uiState.transfersInfo,
                        onClick = this@TransfersManagementActivity::onTransfersWidgetClick,
                    )
                }
            }
        }
    }

    /**
     * Handle widget click
     */
    protected fun onTransfersWidgetClick() {
        transfersManagement.setAreFailedTransfers(false)
        if (this is ManagerActivity) {
            drawerItem = DrawerItem.TRANSFERS
            selectDrawerItem(this.drawerItem)
        } else {
            openTransfersSection()
        }

        if (transfersManagement.isOnTransferOverQuota()) {
            transfersManagement.setHasNotToBeShowDueToTransferOverQuota(true)
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
    private fun updateTransfersWidget(transferType: TransferType) {
        if (transfersManagement.isProcessingTransfers || transfersManagement.isProcessingFolders) {
            return
        }

        transfersManagementViewModel.checkTransfersInfo(
            transferType,
            isOnFileManagementManagerSection
        )
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
        if (isOnFileManagementManagerSection) {
            transfersManagementViewModel.checkTransfersInfo(
                TransferType.NONE,
                true
            )
        } else {
            hideTransfersWidget()
        }
    }

    /**
     * Updates the state of the transfers widget.
     */
    fun updateTransfersWidgetState() {
        transfersManagementViewModel.checkTransfersState()
    }

    /**
     * Hides the transfers widget.
     */
    fun hideTransfersWidget() {
        transfersManagementViewModel.hideTransfersWidget()
    }
}
