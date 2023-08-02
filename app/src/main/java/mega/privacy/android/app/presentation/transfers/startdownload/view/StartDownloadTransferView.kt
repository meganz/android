package mega.privacy.android.app.presentation.transfers.startdownload.view

import android.content.Context
import android.content.Intent
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.palm.composestateevents.EventEffect
import de.palm.composestateevents.StateEventWithContent
import mega.privacy.android.app.R
import mega.privacy.android.app.constants.IntentConstants
import mega.privacy.android.app.main.dialog.storagestatus.StorageStatusDialogView
import mega.privacy.android.app.myAccount.MyAccountActivity
import mega.privacy.android.app.presentation.settings.SettingsActivity
import mega.privacy.android.app.presentation.settings.model.TargetPreference
import mega.privacy.android.app.presentation.transfers.startdownload.StartDownloadTransfersViewModel
import mega.privacy.android.app.presentation.transfers.startdownload.model.StartDownloadTransferEvent
import mega.privacy.android.app.presentation.transfers.startdownload.model.StartDownloadTransferJobInProgress
import mega.privacy.android.app.presentation.transfers.startdownload.model.StartDownloadTransferViewState
import mega.privacy.android.app.presentation.transfers.startdownload.model.TransferTriggerEvent
import mega.privacy.android.app.presentation.transfers.startdownload.model.TransferTriggerEvent.StartDownloadForOffline
import mega.privacy.android.app.presentation.transfers.startdownload.model.TransferTriggerEvent.StartDownloadNode
import mega.privacy.android.app.presentation.transfers.view.TransferInProgressDialog
import mega.privacy.android.app.upgradeAccount.UpgradeAccountActivity
import mega.privacy.android.app.utils.AlertsAndWarnings
import mega.privacy.android.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.core.ui.utils.MinimumTimeVisibility
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.exception.NotEnoughQuotaMegaException
import mega.privacy.android.domain.exception.QuotaExceededMegaException
import timber.log.Timber

/**
 * Helper compose view to show UI related to starting a download transfer
 * (scanning in progress dialog, not enough space snackbar, start download snackbar, quota exceeded, etc.)
 */
@Composable
internal fun StartDownloadTransferView(
    event: StateEventWithContent<TransferTriggerEvent>,
    onConsumeEvent: () -> Unit,
    snackBarHostState: SnackbarHostState,
    viewModel: StartDownloadTransfersViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    EventEffect(
        event = event,
        onConsumed = onConsumeEvent,
        action = {
            when (it) {
                is StartDownloadForOffline -> {
                    viewModel.startDownloadForOffline(it.typedNode)
                }

                is StartDownloadNode -> {
                    viewModel.startDownloadNode(it.typedNodes)
                }
            }
        })
    StartDownloadTransferView(
        uiState = uiState,
        onConsumed = viewModel::consumeOneOffEvent,
        onCancelledConfirmed = viewModel::cancelCurrentJob,
        snackBarHostState = snackBarHostState,
    )
}

@Composable
private fun StartDownloadTransferView(
    uiState: StartDownloadTransferViewState,
    onConsumed: () -> Unit,
    onCancelledConfirmed: () -> Unit,
    snackBarHostState: SnackbarHostState,
) {
    val context = LocalContext.current
    val showOfflineAlertDialog = remember { mutableStateOf(false) }
    val showQuotaExceededDialog = remember { mutableStateOf<StorageState?>(null) }

    EventEffect(
        event = uiState.oneOffViewEvent,
        onConsumed = onConsumed,
        action = {
            consumeEvent(
                it,
                snackBarHostState,
                showOfflineAlertDialog,
                showQuotaExceededDialog,
                context
            )
        })
    MinimumTimeVisibility(visible = uiState.jobInProgressState == StartDownloadTransferJobInProgress.ProcessingFiles) {
        TransferInProgressDialog(onCancelConfirmed = onCancelledConfirmed)
    }
    if (showOfflineAlertDialog.value) {
        MegaAlertDialog(
            text = stringResource(id = R.string.error_server_connection_problem),
            confirmButtonText = stringResource(id = R.string.general_ok),
            cancelButtonText = null,
            onConfirm = { showOfflineAlertDialog.value = false },
            onDismiss = { showOfflineAlertDialog.value = false },
        )
    }
    showQuotaExceededDialog.value?.let {
        StorageStatusDialogView(
            storageState = it,
            preWarning = it != StorageState.Red,
            overQuotaAlert = true,
            onUpgradeClick = {
                context.startActivity(Intent(context, UpgradeAccountActivity::class.java))
            },
            onCustomizedPlanClick = { email, accountType ->
                AlertsAndWarnings.askForCustomizedPlan(context, email, accountType)
            },
            onAchievementsClick = {
                context.startActivity(
                    Intent(context, MyAccountActivity::class.java)
                        .setAction(IntentConstants.ACTION_OPEN_ACHIEVEMENTS)
                )
            },
            onClose = { showQuotaExceededDialog.value = null },
        )
    }
}

private suspend fun consumeEvent(
    event: StartDownloadTransferEvent,
    snackBarHostState: SnackbarHostState,
    showOfflineAlertDialog: MutableState<Boolean>,
    showQuotaExceededDialog: MutableState<StorageState?>,
    context: Context,
) {
    when (event) {
        is StartDownloadTransferEvent.FinishProcessing -> {
            when (event.exception) {
                null -> {
                    val msg = context.resources.getQuantityString(
                        R.plurals.download_started,
                        event.totalNodes,
                        event.totalNodes,
                    )
                    snackBarHostState.showSnackbar(msg)
                }

                is QuotaExceededMegaException -> {
                    showQuotaExceededDialog.value = StorageState.Red
                }

                is NotEnoughQuotaMegaException -> {
                    showQuotaExceededDialog.value = StorageState.Orange
                }

                else -> {
                    Timber.e(event.exception)
                    snackBarHostState.showSnackbar(context.getString(R.string.general_error))
                }
            }
        }

        is StartDownloadTransferEvent.Message -> {
            //show snack bar with an optional action
            val result = snackBarHostState.showSnackbar(
                context.getString(event.message),
                event.action?.let { context.getString(it) }
            )
            if (result == SnackbarResult.ActionPerformed && event.actionEvent != null) {
                consumeEvent(
                    event.actionEvent,
                    snackBarHostState,
                    showOfflineAlertDialog,
                    showQuotaExceededDialog,
                    context
                )
            }
        }

        StartDownloadTransferEvent.GoToFileManagement -> {
            startActivity(
                context,
                SettingsActivity.getIntent(context, TargetPreference.Storage),
                null
            )
        }

        StartDownloadTransferEvent.NotConnected -> {
            showOfflineAlertDialog.value = true
        }
    }
}

