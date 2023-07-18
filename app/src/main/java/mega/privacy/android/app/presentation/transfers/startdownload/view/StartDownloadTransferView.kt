package mega.privacy.android.app.presentation.transfers.startdownload.view

import android.content.Context
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat.startActivity
import de.palm.composestateevents.EventEffect
import mega.privacy.android.app.R
import mega.privacy.android.app.myAccount.StorageStatusDialogState
import mega.privacy.android.app.myAccount.StorageStatusDialogView
import mega.privacy.android.app.presentation.settings.SettingsActivity
import mega.privacy.android.app.presentation.settings.model.TargetPreference
import mega.privacy.android.app.presentation.transfers.startdownload.model.StartDownloadTransferEvent
import mega.privacy.android.app.presentation.transfers.startdownload.model.StartDownloadTransferJobInProgress
import mega.privacy.android.app.presentation.transfers.startdownload.model.StartDownloadTransferViewState
import mega.privacy.android.app.presentation.transfers.view.TransferInProgressDialog
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
fun StartDownloadTransferView(
    uiState: StartDownloadTransferViewState,
    onConsumed: () -> Unit,
    onCancelledConfirmed: () -> Unit,
    snackBarHostState: SnackbarHostState,
) {
    val context = LocalContext.current
    val showOfflineAlertDialog = remember { mutableStateOf(false) }
    val showQuotaExceededDialog = remember { mutableStateOf<StorageStatusDialogState?>(null) }

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
        //This view will be replaced with another will full StorageStatusViewModel logic in task: TRAN-181
        StorageStatusDialogView(
            onDismissRequest = { showQuotaExceededDialog.value = null },
            dismissClickListener = { showQuotaExceededDialog.value = null },
            state = it,
            horizontalActionButtonClickListener = {},
            verticalActionButtonClickListener = {},
            achievementButtonClickListener = {},
        )
    }
}

private suspend fun consumeEvent(
    event: StartDownloadTransferEvent,
    snackBarHostState: SnackbarHostState,
    showOfflineAlertDialog: MutableState<Boolean>,
    showQuotaExceededDialog: MutableState<StorageStatusDialogState?>,
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
                    showQuotaExceededDialog.value = StorageStatusDialogState(
                        storageState = StorageState.Red,
                        overQuotaAlert = true,
                        preWarning = false,
                    )
                }

                is NotEnoughQuotaMegaException -> {
                    showQuotaExceededDialog.value = StorageStatusDialogState(
                        storageState = StorageState.Orange,
                        overQuotaAlert = true,
                        preWarning = true,
                    )
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

