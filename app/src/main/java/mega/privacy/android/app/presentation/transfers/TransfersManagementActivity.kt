package mega.privacy.android.app.presentation.transfers

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.main.managerSections.TransfersViewModel
import mega.privacy.android.app.utils.AlertDialogUtil.isAlertDialogShown
import mega.privacy.android.app.utils.Util
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.navigation.MegaNavigator
import javax.inject.Inject

/**
 * Activity for showing concrete UI items related to transfers management.
 */
@AndroidEntryPoint
open class TransfersManagementActivity : PasscodeActivity() {

    companion object {
        private const val IS_CANCEL_TRANSFERS_SHOWN = "IS_CANCEL_TRANSFERS_SHOWN"
    }

    private var scanningTransfersDialog: AlertDialog? = null
    private var cancelTransfersDialog: AlertDialog? = null

    protected val transfersManagementViewModel: TransfersManagementViewModel by viewModels()
    protected val transfersViewModel: TransfersViewModel by viewModels()

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

    override fun onDestroy() {
        super.onDestroy()

        scanningTransfersDialog?.dismiss()
        cancelTransfersDialog?.dismiss()
    }
}
