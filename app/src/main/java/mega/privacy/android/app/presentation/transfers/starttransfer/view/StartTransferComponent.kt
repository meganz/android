package mega.privacy.android.app.presentation.transfers.starttransfer.view

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
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
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.constants.IntentConstants
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.main.dialog.storagestatus.StorageStatusDialogView
import mega.privacy.android.app.myAccount.MyAccountActivity
import mega.privacy.android.app.presentation.permissions.NotificationsPermissionActivity
import mega.privacy.android.app.presentation.settings.SettingsActivity
import mega.privacy.android.app.presentation.settings.model.TargetPreference
import mega.privacy.android.app.presentation.transfers.starttransfer.StartTransfersComponentViewModel
import mega.privacy.android.app.presentation.transfers.starttransfer.model.StartTransferEvent
import mega.privacy.android.app.presentation.transfers.starttransfer.model.StartTransferJobInProgress
import mega.privacy.android.app.presentation.transfers.starttransfer.model.StartTransferViewState
import mega.privacy.android.app.presentation.transfers.starttransfer.model.TransferTriggerEvent
import mega.privacy.android.app.presentation.transfers.view.TransferInProgressDialog
import mega.privacy.android.app.upgradeAccount.UpgradeAccountActivity
import mega.privacy.android.app.usecase.exception.NotEnoughQuotaMegaException
import mega.privacy.android.app.usecase.exception.QuotaExceededMegaException
import mega.privacy.android.app.utils.AlertsAndWarnings
import mega.privacy.android.app.utils.Util
import mega.privacy.android.core.ui.controls.dialogs.ConfirmationDialog
import mega.privacy.android.core.ui.controls.dialogs.ConfirmationDialogWithRadioButtons
import mega.privacy.android.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.core.ui.navigation.launchFolderPicker
import mega.privacy.android.core.ui.utils.MinimumTimeVisibility
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.shared.theme.MegaAppTheme
import timber.log.Timber

/**
 * Helper compose view to show UI related to starting a download transfer
 * (scanning in progress dialog, not enough space snackbar, start download snackbar, quota exceeded, etc.)
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
internal fun StartTransferComponent(
    event: StateEventWithContent<TransferTriggerEvent>,
    onConsumeEvent: () -> Unit,
    snackBarHostState: SnackbarHostState,
    viewModel: StartTransfersComponentViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var eventWithoutWritePermission by remember {
        mutableStateOf<TransferTriggerEvent?>(null)
    }
    val notificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        null
    }
    val mediaReadPermission = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
        rememberPermissionState(Manifest.permission.WRITE_EXTERNAL_STORAGE) { granted ->
            if (granted) {
                eventWithoutWritePermission?.let {
                    viewModel.startTransfer(it)
                }
            }
            eventWithoutWritePermission = null
        }
    } else {
        null
    }

    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            lifecycleOwner.lifecycle.addObserver(viewModel)
        }
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
            if (mediaReadPermission?.status?.isGranted == false) {
                eventWithoutWritePermission = it
                mediaReadPermission.launchPermissionRequest()
            } else {
                viewModel.startTransfer(it)
            }
        })
    StartTransferComponent(
        uiState = uiState,
        onOneOffEventConsumed = viewModel::consumeOneOffEvent,
        onCancelledConfirmed = viewModel::cancelCurrentJob,
        onDownloadConfirmed = { transferTriggerEventPrimary, saveDoNotAskAgain ->
            viewModel.startDownloadWithoutConfirmation(
                transferTriggerEventPrimary,
                saveDoNotAskAgain
            )
        },
        onDestinationSet = viewModel::startDownloadWithDestination,
        onPromptSaveDestinationConsumed = viewModel::consumePromptSaveDestination,
        onSaveDestination = viewModel::saveDestination,
        onDoNotPromptToSaveDestinationAgain = viewModel::doNotPromptToSaveDestinationAgain,
        snackBarHostState = snackBarHostState,
    )
}

/**
 * Helper function to wrap [StartTransferComponent] into a [ComposeView] so it can be used in screens using View system
 * @param activity the parent activity where this view will be added, it should implement [SnackbarShower] to show the generated Snackbars
 * @param transferEventState flow that usually comes from the view model and triggers the download Transfer events
 * @param onConsumeEvent lambda to consume the download event, typically it will launch the corresponding consume event in the view model
 */
fun createStartTransferView(
    activity: Activity,
    transferEventState: Flow<StateEventWithContent<TransferTriggerEvent>>,
    onConsumeEvent: () -> Unit,
): View = ComposeView(activity).apply {
    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
    setContent {
        val downloadEvent by transferEventState.collectAsStateWithLifecycle(
            (transferEventState as? StateFlow)?.value ?: consumed()
        )
        MegaAppTheme(isDark = isSystemInDarkTheme()) {
            val snackbarHostState = remember { SnackbarHostState() }
            //if we need this view is because we are not using compose views, so we don't have a scaffold to show snack bars and need to launch a View snackbar
            LaunchedEffect(snackbarHostState.currentSnackbarData) {
                snackbarHostState.currentSnackbarData?.message?.let {
                    Util.showSnackbar(activity, it)
                }
                snackbarHostState.currentSnackbarData?.dismiss()
            }
            StartTransferComponent(
                downloadEvent,
                onConsumeEvent,
                snackBarHostState = snackbarHostState,
            )
        }
    }
}


@Composable
private fun StartTransferComponent(
    uiState: StartTransferViewState,
    onOneOffEventConsumed: () -> Unit,
    onCancelledConfirmed: () -> Unit,
    onDownloadConfirmed: (TransferTriggerEvent.DownloadTriggerEvent, saveDoNotAskAgain: Boolean) -> Unit,
    onDestinationSet: (TransferTriggerEvent.StartDownloadNode, destination: Uri) -> Unit,
    onPromptSaveDestinationConsumed: () -> Unit,
    onSaveDestination: (String) -> Unit,
    onDoNotPromptToSaveDestinationAgain: () -> Unit,
    snackBarHostState: SnackbarHostState,
) {
    val context = LocalContext.current
    var showOfflineAlertDialog by rememberSaveable { mutableStateOf(false) }
    val showQuotaExceededDialog = remember { mutableStateOf<StorageState?>(null) }
    var showConfirmLargeTransfer by remember {
        mutableStateOf<StartTransferEvent.ConfirmLargeDownload?>(null)
    }
    var showAskDestinationDialog by remember {
        mutableStateOf<TransferTriggerEvent.StartDownloadNode?>(null)
    }
    val folderPicker = launchFolderPicker { uri ->
        showAskDestinationDialog?.let { event ->
            onDestinationSet(event, uri)
            showAskDestinationDialog = null
        }
    }

    EventEffect(
        event = uiState.oneOffViewEvent,
        onConsumed = onOneOffEventConsumed,
        action = {
            when (it) {
                is StartTransferEvent.FinishProcessing ->
                    consumeFinishProcessing(
                        event = it,
                        snackBarHostState = snackBarHostState,
                        showQuotaExceededDialog = showQuotaExceededDialog,
                        context = context,
                        transferTriggerEvent = it.triggerEvent,
                    )

                is StartTransferEvent.Message ->
                    consumeMessage(it, snackBarHostState, context)

                is StartTransferEvent.MessagePlural ->
                    consumeMessage(it, snackBarHostState, context)

                StartTransferEvent.NotConnected -> {
                    showOfflineAlertDialog = true
                }

                is StartTransferEvent.ConfirmLargeDownload -> {
                    showConfirmLargeTransfer = it
                }

                is StartTransferEvent.AskDestination -> {
                    showAskDestinationDialog = it.originalEvent
                }
            }
        })

    var showPromptSaveDestinationDialog by rememberSaveable { mutableStateOf<String?>(null) }
    var doNotPromptSaveDestinationAgain by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(showPromptSaveDestinationDialog) {
        doNotPromptSaveDestinationAgain = false
    }
    EventEffect(
        event = uiState.promptSaveDestination,
        onConsumed = onPromptSaveDestinationConsumed,
        action = {
            showPromptSaveDestinationDialog = it
        }
    )
    MinimumTimeVisibility(visible = uiState.jobInProgressState == StartTransferJobInProgress.ScanningTransfers) {
        TransferInProgressDialog(onCancelConfirmed = onCancelledConfirmed)
    }
    if (showOfflineAlertDialog) {
        MegaAlertDialog(
            text = stringResource(id = R.string.error_server_connection_problem),
            confirmButtonText = stringResource(id = R.string.general_ok),
            cancelButtonText = null,
            onConfirm = { showOfflineAlertDialog = false },
            onDismiss = { showOfflineAlertDialog = false },
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
    showConfirmLargeTransfer?.let {
        ConfirmationDialog(
            title = stringResource(id = R.string.transfers_confirm_large_download_title),
            text = stringResource(id = R.string.alert_larger_file, it.sizeString),
            buttonOption1Text = stringResource(id = R.string.transfers_confirm_large_download_button_start),
            buttonOption2Text = stringResource(id = R.string.transfers_confirm_large_download_button_start_always),
            cancelButtonText = stringResource(id = R.string.general_cancel),
            onOption1 = {
                onDownloadConfirmed(it.transferTriggerEvent, false)
                showConfirmLargeTransfer = null
            },
            onOption2 = {
                onDownloadConfirmed(it.transferTriggerEvent, true)
                showConfirmLargeTransfer = null
            },
            onDismiss = { showConfirmLargeTransfer = null },
        )
    }
    if (showAskDestinationDialog != null) {
        folderPicker.launch(null)
    }
    showPromptSaveDestinationDialog?.let { destination ->
        val radioOption = stringResource(id = R.string.general_do_not_show)
        //this dialog will be updated once we have a dialog defined for this case that follows our DS
        ConfirmationDialogWithRadioButtons(
            radioOptions = listOf(radioOption),
            cancelButtonText = stringResource(id = R.string.general_negative_button),
            confirmButtonText = stringResource(id = R.string.general_yes),
            titleText = stringResource(id = R.string.confirmation_download_location),
            initialSelectedOption = if (doNotPromptSaveDestinationAgain) radioOption else "",
            onOptionSelected = {
                doNotPromptSaveDestinationAgain = doNotPromptSaveDestinationAgain.not()
            },
            onDismissRequest = {
                if (doNotPromptSaveDestinationAgain) {
                    onDoNotPromptToSaveDestinationAgain()
                }
                showPromptSaveDestinationDialog = null
            },
            onConfirmRequest = {
                onSaveDestination(destination)
                showPromptSaveDestinationDialog = null
            }
        )
    }
}

private suspend fun consumeFinishProcessing(
    event: StartTransferEvent.FinishProcessing,
    snackBarHostState: SnackbarHostState,
    showQuotaExceededDialog: MutableState<StorageState?>,
    context: Context,
    transferTriggerEvent: TransferTriggerEvent?,
) {
    when (event.exception) {
        null -> {
            val message = when {
                transferTriggerEvent is TransferTriggerEvent.StartDownloadForPreview -> {
                    context.resources.getString(R.string.cloud_drive_snackbar_preparing_file_for_preview_context)
                }

                event.totalAlreadyDownloaded == 0 -> {
                    context.resources.getQuantityString(
                        R.plurals.download_started,
                        event.totalNodes,
                        event.totalNodes,
                    )
                }

                event.filesToDownload == 0 -> {
                    context.resources.getQuantityString(
                        R.plurals.already_downloaded_service,
                        event.totalFiles,
                        event.totalFiles,
                    )
                }

                event.totalAlreadyDownloaded == 1 -> {
                    context.resources.getQuantityString(
                        R.plurals.file_already_downloaded_and_files_pending_download,
                        event.filesToDownload,
                        event.filesToDownload
                    )
                }

                event.filesToDownload == 1 -> {
                    context.resources.getQuantityString(
                        R.plurals.files_already_downloaded_and_file_pending_download,
                        event.totalAlreadyDownloaded,
                        event.totalAlreadyDownloaded
                    )
                }

                else -> {
                    StringBuilder().append(
                        context.resources.getQuantityString(
                            R.plurals.file_already_downloaded,
                            event.totalAlreadyDownloaded,
                            event.totalAlreadyDownloaded
                        )
                    ).append(" ").append(
                        context.resources.getQuantityString(
                            R.plurals.file_pending_download,
                            event.filesToDownload,
                            event.filesToDownload
                        )
                    ).toString()
                }
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
    event: StartTransferEvent.Message,
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

private suspend fun consumeMessage(
    event: StartTransferEvent.MessagePlural,
    snackBarHostState: SnackbarHostState,
    context: Context,
) {
    //show snack bar
    snackBarHostState.showSnackbar(
        context.resources.getQuantityString(event.message, event.quantity, event.quantity)
    )
}

private fun consumeMessageAction(
    actionEvent: StartTransferEvent.Message.ActionEvent,
    context: Context,
) = when (actionEvent) {
    StartTransferEvent.Message.ActionEvent.GoToFileManagement -> {
        ContextCompat.startActivity(
            context,
            SettingsActivity.getIntent(context, TargetPreference.Storage),
            null
        )
    }
}

