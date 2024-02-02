package mega.privacy.android.app.presentation.transfers.startdownload.view

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.View
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import de.palm.composestateevents.EventEffect
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import mega.privacy.android.app.R
import mega.privacy.android.app.constants.IntentConstants
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.main.dialog.storagestatus.StorageStatusDialogView
import mega.privacy.android.app.myAccount.MyAccountActivity
import mega.privacy.android.app.presentation.permissions.NotificationsPermissionActivity
import mega.privacy.android.app.presentation.settings.SettingsActivity
import mega.privacy.android.app.presentation.settings.model.TargetPreference
import mega.privacy.android.app.presentation.transfers.startdownload.StartDownloadComponentViewModel
import mega.privacy.android.app.presentation.transfers.startdownload.model.StartDownloadTransferEvent
import mega.privacy.android.app.presentation.transfers.startdownload.model.StartDownloadTransferJobInProgress
import mega.privacy.android.app.presentation.transfers.startdownload.model.StartDownloadTransferViewState
import mega.privacy.android.app.presentation.transfers.startdownload.model.TransferTriggerEvent
import mega.privacy.android.app.presentation.transfers.view.TransferInProgressDialog
import mega.privacy.android.app.upgradeAccount.UpgradeAccountActivity
import mega.privacy.android.app.utils.AlertsAndWarnings
import mega.privacy.android.app.utils.Util
import mega.privacy.android.core.ui.controls.dialogs.ConfirmationDialog
import mega.privacy.android.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.core.ui.utils.MinimumTimeVisibility
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.exception.NotEnoughQuotaMegaException
import mega.privacy.android.domain.exception.QuotaExceededMegaException
import mega.privacy.android.shared.theme.MegaAppTheme
import timber.log.Timber

/**
 * Helper compose view to show UI related to starting a download transfer
 * (scanning in progress dialog, not enough space snackbar, start download snackbar, quota exceeded, etc.)
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
internal fun StartDownloadComponent(
    event: StateEventWithContent<TransferTriggerEvent>,
    onConsumeEvent: () -> Unit,
    snackBarHostState: SnackbarHostState,
    viewModel: StartDownloadComponentViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val notificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        null
    }
    EventEffect(
        event = event,
        onConsumed = onConsumeEvent,
        action = {
            notificationPermission?.status?.let { permissionStatus ->
                if (permissionStatus.shouldShowRationale) {
                    context.startActivity(
                        Intent(context, NotificationsPermissionActivity::class.java)
                    )
                } else if (!permissionStatus.isGranted) {
                    notificationPermission.launchPermissionRequest()
                }
            }
            viewModel.startDownload(it)
        })
    StartDownloadComponent(
        uiState = uiState,
        onOneOffEventConsumed = viewModel::consumeOneOffEvent,
        onCancelledConfirmed = viewModel::cancelCurrentJob,
        onDownloadConfirmed = { transferTriggerEventPrimary, saveDoNotAskAgain ->
            viewModel.startDownloadWithoutConfirmation(
                transferTriggerEventPrimary,
                saveDoNotAskAgain
            )
        },
        snackBarHostState = snackBarHostState,
    )
}

/**
 * Helper function to wrap [StartDownloadComponent] into a [ComposeView] so it can be used in screens using View system
 * @param activity the parent activity where this view will be added, it should implement [SnackbarShower] to show the generated Snackbars
 * @param downloadEventState flow that usually comes from the view model and triggers the download Transfer events
 * @param onConsumeEvent lambda to consume the download event, typically it will launch the corresponding consume event in the view model
 */
fun createStartDownloadView(
    activity: Activity,
    downloadEventState: Flow<StateEventWithContent<TransferTriggerEvent>>,
    onConsumeEvent: () -> Unit,
): View = ComposeView(activity).apply {
    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
    setContent {
        val downloadEvent by downloadEventState.collectAsStateWithLifecycle(
            (downloadEventState as? StateFlow)?.value ?: consumed()
        )
        MegaAppTheme(isDark = isSystemInDarkTheme()) {
            val snackbarHostState = remember { SnackbarHostState() }
            //if we need this view is because we are not using compose views, so we don't have a scaffold to show snack bars and need to launch a View snackbar
            LaunchedEffect(snackbarHostState.currentSnackbarData) {
                snackbarHostState.currentSnackbarData?.message?.let {
                    Util.showSnackbar(activity, it)
                }
            }
            StartDownloadComponent(
                downloadEvent,
                onConsumeEvent,
                snackBarHostState = snackbarHostState,
            )
        }
    }
}


@Composable
private fun StartDownloadComponent(
    uiState: StartDownloadTransferViewState,
    onOneOffEventConsumed: () -> Unit,
    onCancelledConfirmed: () -> Unit,
    onDownloadConfirmed: (TransferTriggerEvent, saveDoNotAskAgain: Boolean) -> Unit,
    snackBarHostState: SnackbarHostState,
) {
    val context = LocalContext.current
    val showOfflineAlertDialog = remember { mutableStateOf(false) }
    val showQuotaExceededDialog = remember { mutableStateOf<StorageState?>(null) }
    val showConfirmLargeTransfer =
        remember { mutableStateOf<StartDownloadTransferEvent.ConfirmLargeDownload?>(null) }

    EventEffect(
        event = uiState.oneOffViewEvent,
        onConsumed = onOneOffEventConsumed,
        action = {
            when (it) {
                is StartDownloadTransferEvent.FinishProcessing ->
                    consumeFinishProcessing(
                        event = it,
                        snackBarHostState = snackBarHostState,
                        showQuotaExceededDialog = showQuotaExceededDialog,
                        context = context,
                        transferTriggerEvent = uiState.transferTriggerEvent
                    )

                is StartDownloadTransferEvent.Message ->
                    consumeMessage(it, snackBarHostState, context)

                StartDownloadTransferEvent.NotConnected -> {
                    showOfflineAlertDialog.value = true
                }

                is StartDownloadTransferEvent.ConfirmLargeDownload -> {
                    showConfirmLargeTransfer.value = it
                }
            }
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
    showConfirmLargeTransfer.value?.let {
        // texts of this dialog will be updated in TRAN-280
        ConfirmationDialog(
            title = stringResource(id = R.string.transfers_confirm_large_download_title),
            text = stringResource(id = R.string.alert_larger_file, it.sizeString),
            buttonOption1Text = stringResource(id = R.string.transfers_confirm_large_download_button_start),
            buttonOption2Text = stringResource(id = R.string.transfers_confirm_large_download_button_start_always),
            cancelButtonText = stringResource(id = R.string.general_cancel),
            onOption1 = {
                onDownloadConfirmed(it.transferTriggerEvent, false)
                showConfirmLargeTransfer.value = null
            },
            onOption2 = {
                onDownloadConfirmed(it.transferTriggerEvent, true)
                showConfirmLargeTransfer.value = null
            },
            onDismiss = { showConfirmLargeTransfer.value = null },
        )
    }
}

private suspend fun consumeFinishProcessing(
    event: StartDownloadTransferEvent.FinishProcessing,
    snackBarHostState: SnackbarHostState,
    showQuotaExceededDialog: MutableState<StorageState?>,
    context: Context,
    transferTriggerEvent: TransferTriggerEvent?,
) {
    when (event.exception) {
        null -> {
            val message = when (transferTriggerEvent) {
                is TransferTriggerEvent.StartDownloadForPreview -> {
                    context.resources.getString(R.string.cloud_drive_snackbar_preparing_file_for_preview_context)
                }

                else -> context.resources.getQuantityString(
                    R.plurals.download_started,
                    event.totalNodes,
                    event.totalNodes,
                )
            }
            snackBarHostState.showSnackbar(message)
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

private suspend fun consumeMessage(
    event: StartDownloadTransferEvent.Message,
    snackBarHostState: SnackbarHostState,
    context: Context,
) {
    //show snack bar with an optional action
    val result = snackBarHostState.showSnackbar(
        context.getString(event.message),
        event.action?.let { context.getString(it) }
    )
    if (result == SnackbarResult.ActionPerformed && event.actionEvent != null) {
        consumeMessageAction(
            event.actionEvent,
            context
        )
    }
}

private fun consumeMessageAction(
    actionEvent: StartDownloadTransferEvent.Message.ActionEvent,
    context: Context,
) = when (actionEvent) {
    StartDownloadTransferEvent.Message.ActionEvent.GoToFileManagement -> {
        ContextCompat.startActivity(
            context,
            SettingsActivity.getIntent(context, TargetPreference.Storage),
            null
        )
    }
}

