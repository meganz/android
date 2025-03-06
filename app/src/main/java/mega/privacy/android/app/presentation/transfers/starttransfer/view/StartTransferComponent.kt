package mega.privacy.android.app.presentation.transfers.starttransfer.view

import mega.privacy.android.shared.resources.R as sharedR
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
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import mega.privacy.android.app.presentation.node.action.HandleFileAction
import mega.privacy.android.app.presentation.permissions.NotificationsPermissionActivity
import mega.privacy.android.app.presentation.snackbar.LegacySnackBarWrapper
import mega.privacy.android.app.presentation.transfers.starttransfer.StartTransfersComponentViewModel
import mega.privacy.android.app.presentation.transfers.starttransfer.model.SaveDestinationInfo
import mega.privacy.android.app.presentation.transfers.starttransfer.model.StartTransferEvent
import mega.privacy.android.app.presentation.transfers.starttransfer.model.StartTransferViewState
import mega.privacy.android.app.presentation.transfers.starttransfer.model.TransferTriggerEvent
import mega.privacy.android.app.presentation.transfers.starttransfer.model.TransferTriggerEvent.StartUpload
import mega.privacy.android.app.presentation.transfers.starttransfer.model.TransferTriggerEvent.StartUpload.Files
import mega.privacy.android.app.presentation.transfers.starttransfer.view.dialog.ResumeChatTransfersDialog
import mega.privacy.android.app.presentation.transfers.starttransfer.view.dialog.ResumePreviewTransfersDialog
import mega.privacy.android.app.presentation.transfers.starttransfer.view.filespermission.FilesPermissionDialog
import mega.privacy.android.app.presentation.transfers.view.dialog.CancelTransferDialog
import mega.privacy.android.app.presentation.transfers.view.dialog.NotEnoughSpaceForUploadDialog
import mega.privacy.android.app.presentation.transfers.view.dialog.TransferInProgressDialog
import mega.privacy.android.app.upgradeAccount.UpgradeAccountActivity
import mega.privacy.android.app.usecase.exception.NotEnoughQuotaMegaException
import mega.privacy.android.app.usecase.exception.QuotaExceededMegaException
import mega.privacy.android.app.utils.AlertsAndWarnings
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.shared.original.core.ui.controls.dialogs.ConfirmationDialog
import mega.privacy.android.shared.original.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.shared.original.core.ui.navigation.launchFolderPicker
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.original.core.ui.utils.showAutoDurationSnackbar
import timber.log.Timber
import java.io.File

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
    onScanningFinished: (StartTransferEvent) -> Unit = {},
    viewModel: StartTransfersComponentViewModel = hiltViewModel(),
    navigateToStorageSettings: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showFilesPermissionRequest by rememberSaveable { mutableStateOf(false) }
    var showStorageOverQuotaWarning by rememberSaveable { mutableStateOf(false) }
    val notificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS) {
            // we don't need notifications granted to start, but sometimes we wait user response to start, check [TransferTriggerEvent.waitForNotificationPermissionResponseToStart]
            viewModel.startTransferAfterPermissionRequest()
        }
    } else {
        null
    }
    val mediaReadPermission = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
        rememberPermissionState(Manifest.permission.WRITE_EXTERNAL_STORAGE) { granted ->
            if (granted) {
                viewModel.startTransferAfterPermissionRequest()
            } else {
                viewModel.consumeRequestPermission()
            }
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
        action = { triggerEvent ->
            if (uiState.isStorageOverQuota && triggerEvent is StartUpload) {
                showStorageOverQuotaWarning = true
                return@EventEffect
            }

            notificationPermission?.status?.let { permissionStatus ->
                if (permissionStatus.shouldShowRationale) {
                    context.startActivity(
                        Intent(context, NotificationsPermissionActivity::class.java)
                    )
                } else if (!permissionStatus.isGranted) {
                    notificationPermission.launchPermissionRequest()
                    if (triggerEvent.waitNotificationPermissionResponseToStart) {
                        viewModel.transferEventWaitingForPermissionRequest(triggerEvent)
                        return@EventEffect
                    }
                }
            }
            when {
                triggerEvent !is TransferTriggerEvent.StartUpload.TextFile
                        && mediaReadPermission?.status?.isGranted == false -> {
                    viewModel.transferEventWaitingForPermissionRequest(triggerEvent)
                    mediaReadPermission.launchPermissionRequest()
                }

                else -> {
                    viewModel.startTransfer(triggerEvent)
                }
            }
        })

    if (showFilesPermissionRequest) {
        FilesPermissionDialog(
            onDoNotShowAgainClick = { viewModel.setRequestFilesPermissionDenied() },
            onStartTransferAndDismiss = {
                viewModel.startTransferAfterPermissionRequest()
                showFilesPermissionRequest = false
            }
        )
    }

    if (showStorageOverQuotaWarning) {
        NotEnoughSpaceForUploadDialog(onCancel = { showStorageOverQuotaWarning = false })
    }

    StartTransferComponent(
        uiState = uiState,
        onOneOffEventConsumed = viewModel::consumeOneOffEvent,
        onCancelled = viewModel::cancelCurrentTransfersJob,
        onLargeDownloadAnswered = viewModel::largeDownloadAnswered,
        onDestinationSet = viewModel::startDownloadWithDestination,
        onPromptSaveDestinationConsumed = viewModel::consumePromptSaveDestination,
        onSaveDestination = viewModel::saveDestination,
        onDoNotPromptToSaveDestinationAgain = viewModel::doNotPromptToSaveDestinationAgain,
        onResumeTransfers = viewModel::resumeTransfers,
        onAskedResumeTransfers = viewModel::setAskedResumeTransfers,
        snackBarHostState = snackBarHostState,
        onScanningFinished = onScanningFinished,
        navigateToStorageSettings = navigateToStorageSettings,
        onPreviewFile = viewModel::previewFile,
        onPreviewOpened = viewModel::consumePreviewFileOpened,
        onCancelTransferConfirmed = viewModel::cancelTransferConfirmed,
        onCancelTransferCancelled = viewModel::cancelTransferCancelled,
        onConsumeCancelTransferResult = viewModel::onConsumeCancelTransferResult,
    )
}

/**
 * Helper function to wrap [StartTransferComponent] into a [ComposeView] so it can be used in screens using View system
 * @param activity the parent activity where this view will be added, it should implement [SnackbarShower] to show the generated Snackbars
 * @param transferEventState flow that usually comes from the view model and triggers the download Transfer events
 * @param onConsumeEvent lambda to consume the download event, typically it will launch the corresponding consume event in the view model,
 * @param onScanningFinished lambda to be called when the scanning process is finished.
 */
internal fun createStartTransferView(
    activity: Activity,
    transferEventState: Flow<StateEventWithContent<TransferTriggerEvent>>,
    onConsumeEvent: () -> Unit,
    navigateToStorageSettings: () -> Unit,
    onScanningFinished: (StartTransferEvent) -> Unit = {},
): View = ComposeView(activity).apply {
    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
    setContent {
        val downloadEvent by transferEventState.collectAsStateWithLifecycle(
            (transferEventState as? StateFlow)?.value ?: consumed()
        )
        OriginalTheme(isDark = isSystemInDarkTheme()) {
            val snackbarHostState = remember { SnackbarHostState() }
            //if we need this view is because we are not using compose views, so we don't have a scaffold to show snack bars and need to launch a View snackbar
            LegacySnackBarWrapper(snackbarHostState = snackbarHostState, activity)
            StartTransferComponent(
                event = downloadEvent,
                onConsumeEvent = onConsumeEvent,
                snackBarHostState = snackbarHostState,
                onScanningFinished = onScanningFinished,
                navigateToStorageSettings = navigateToStorageSettings,
            )
        }
    }
}

@Composable
private fun StartTransferComponent(
    uiState: StartTransferViewState,
    onOneOffEventConsumed: () -> Unit,
    onCancelled: () -> Unit,
    onLargeDownloadAnswered: (TransferTriggerEvent.DownloadTriggerEvent?, saveDoNotAskAgain: Boolean) -> Unit,
    onDestinationSet: (destination: Uri?) -> Unit,
    onPromptSaveDestinationConsumed: () -> Unit,
    onSaveDestination: (String) -> Unit,
    onDoNotPromptToSaveDestinationAgain: () -> Unit,
    onResumeTransfers: () -> Unit,
    onAskedResumeTransfers: () -> Unit,
    snackBarHostState: SnackbarHostState,
    onScanningFinished: (StartTransferEvent) -> Unit = {},
    navigateToStorageSettings: () -> Unit,
    onPreviewFile: (File) -> Unit,
    onPreviewOpened: () -> Unit,
    onCancelTransferConfirmed: () -> Unit,
    onCancelTransferCancelled: () -> Unit,
    onConsumeCancelTransferResult: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showOfflineAlertDialog by rememberSaveable { mutableStateOf(false) }
    var showResumeChatUploadsAlertDialog by rememberSaveable { mutableStateOf(false) }
    var showResumePreviewDownloadsAlertDialog by rememberSaveable { mutableStateOf<String?>(null) }
    val showQuotaExceededDialog = rememberSaveable(stateSaver = storageStateSaver) {
        mutableStateOf(null)
    }
    var launchFolderPickerForDownloadDestination by rememberSaveable(uiState.askDestinationForDownload != null) {
        mutableStateOf(uiState.askDestinationForDownload != null)
    }

    val folderPicker = launchFolderPicker(
        onCancel = {
            onDestinationSet(null)
        },
        onFolderSelected = { uri ->
            onDestinationSet(uri)
        },
    )

    EventEffect(
        event = uiState.oneOffViewEvent,
        onConsumed = onOneOffEventConsumed,
        action = {
            when (it) {
                is StartTransferEvent.FinishDownloadProcessing -> {
                    it.exception?.let {
                        downloadScanningFinishedWithError(
                            exception = it,
                            snackBarHostState = snackBarHostState,
                            showQuotaExceededDialog = showQuotaExceededDialog,
                            context = context,
                        )
                    }
                    onScanningFinished(it)
                }

                is StartTransferEvent.FinishUploadProcessing -> {
                    val message = (it.triggerEvent as? Files)?.specificStartMessage
                        ?: context.resources.getQuantityString(
                            R.plurals.upload_began,
                            it.totalFiles,
                            it.totalFiles,
                        )
                    snackBarHostState.showAutoDurationSnackbar(message)
                    onScanningFinished(it)
                }

                is StartTransferEvent.Message -> {
                    consumeMessage(
                        it,
                        snackBarHostState,
                        context,
                        navigateToStorageSettings,
                        onPreviewFile,
                    )
                }

                StartTransferEvent.NotConnected -> {
                    showOfflineAlertDialog = true
                }

                is StartTransferEvent.PausedTransfers -> {
                    when {
                        it.triggerEvent is TransferTriggerEvent.StartChatUpload -> {
                            showResumeChatUploadsAlertDialog = true
                        }

                        it.triggerEvent is TransferTriggerEvent.StartDownloadForPreview -> {
                            showResumePreviewDownloadsAlertDialog =
                                it.triggerEvent.node?.name ?: run {
                                    Timber.w("Empty name for file preview")
                                    ""
                                }
                        }
                    }
                }

                is StartTransferEvent.FinishCopyOffline -> {
                    snackBarHostState.showAutoDurationSnackbar(
                        context.resources.getQuantityString(
                            R.plurals.download_complete,
                            it.totalFiles,
                            it.totalFiles,
                        )
                    )
                }
            }
        })

    var showPromptSaveDestinationDialog by rememberSaveable {
        mutableStateOf<SaveDestinationInfo?>(null)
    }
    EventEffect(
        event = uiState.promptSaveDestination,
        onConsumed = onPromptSaveDestinationConsumed,
        action = {
            showPromptSaveDestinationDialog = it
        }
    )

    TransferInProgressDialog(
        uiState.jobInProgressState,
        onCancel = onCancelled,
    )

    if (showOfflineAlertDialog) {
        MegaAlertDialog(
            text = stringResource(id = R.string.error_server_connection_problem),
            confirmButtonText = stringResource(id = R.string.general_ok),
            cancelButtonText = null,
            onConfirm = { showOfflineAlertDialog = false },
            onDismiss = { showOfflineAlertDialog = false },
        )
    }
    if (uiState.previewFileToOpen != null) {
        HandleFileAction(
            file = uiState.previewFileToOpen,
            isOpenWith = uiState.isOpenWithAction,
            snackBarHostState = snackBarHostState,
            onActionHandled = onPreviewOpened,
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
    uiState.confirmLargeDownload?.let {
        val textId = if (it.transferTriggerEvent is TransferTriggerEvent.StartDownloadForPreview) {
            sharedR.string.alert_larger_file_preview
        } else {
            R.string.alert_larger_file
        }

        ConfirmationDialog(
            title = stringResource(id = R.string.transfers_confirm_large_download_title),
            text = stringResource(id = textId, it.sizeString),
            buttonOption1Text = stringResource(id = R.string.transfers_confirm_large_download_button_start),
            buttonOption2Text = stringResource(id = R.string.transfers_confirm_large_download_button_start_always),
            cancelButtonText = stringResource(id = sharedR.string.general_dialog_cancel_button),
            onOption1 = {
                onLargeDownloadAnswered(it.transferTriggerEvent, false)
            },
            onOption2 = {
                onLargeDownloadAnswered(it.transferTriggerEvent, true)
            },
            onDismiss = { onLargeDownloadAnswered(null, false) },
        )
    }
    if (launchFolderPickerForDownloadDestination) {
        launchFolderPickerForDownloadDestination = false
        runCatching {
            folderPicker.launch(null)
        }.onFailure {
            coroutineScope.launch {
                snackBarHostState.showAutoDurationSnackbar(context.getString(R.string.general_warning_no_picker))
            }
        }
    }
    showPromptSaveDestinationDialog?.let { saveDestinationInfo ->
        //this dialog will be updated once we have a dialog defined for this case that follows our DS
        ConfirmationDialog(
            title = stringResource(
                id = sharedR.string.transfers_dialog_save_download_location_title,
                saveDestinationInfo.destinationName
            ),
            confirmButtonText = stringResource(id = sharedR.string.transfers_dialog_save_download_location_always_here_option),
            cancelButtonText = stringResource(id = sharedR.string.transfers_dialog_save_download_location_always_ask_option),
            onConfirm = {
                onSaveDestination(saveDestinationInfo.destination)
                showPromptSaveDestinationDialog = null
            },
            onDismiss = {
                showPromptSaveDestinationDialog = null
            },
            onCancel = {
                onDoNotPromptToSaveDestinationAgain()
                showPromptSaveDestinationDialog = null
            },
        )
    }
    if (showResumeChatUploadsAlertDialog) {
        ResumeChatTransfersDialog(
            onResume = {
                onResumeTransfers()
                showResumeChatUploadsAlertDialog = false
            },
            onDismiss = {
                onAskedResumeTransfers()
                showResumeChatUploadsAlertDialog = false
            })
    }
    showResumePreviewDownloadsAlertDialog?.let { fileName ->
        ResumePreviewTransfersDialog(
            fileName = fileName,
            onResume = {
                onResumeTransfers()
                showResumePreviewDownloadsAlertDialog = null
            },
            onDismiss = {
                onAskedResumeTransfers()
                showResumePreviewDownloadsAlertDialog = null
            })
    }
    uiState.transferTagToCancel?.let {
        CancelTransferDialog(
            title = stringResource(sharedR.string.transfers_cancel_download_warning_title),
            onCancelTransfer = onCancelTransferConfirmed,
            onDismiss = onCancelTransferCancelled
        )
    }
    EventEffect(
        event = uiState.cancelTransferResult,
        onConsumed = onConsumeCancelTransferResult,
        action = {
            snackBarHostState.showAutoDurationSnackbar(
                context.getString(
                    if (it.success) {
                        sharedR.string.transfers_cancel_download_success_message
                    } else {
                        sharedR.string.transfers_error_cancelling_download
                    }
                )
            )
        }
    )
}

private val storageStateSaver = Saver<StorageState?, Int>(
    save = { it?.ordinal },
    restore = { StorageState.entries.getOrNull(it) }
)

private suspend fun downloadScanningFinishedWithError(
    exception: Throwable,
    snackBarHostState: SnackbarHostState,
    showQuotaExceededDialog: MutableState<StorageState?>,
    context: Context,
) {
    when (exception) {
        is QuotaExceededMegaException -> {
            showQuotaExceededDialog.value = StorageState.Red
        }

        is NotEnoughQuotaMegaException -> {
            showQuotaExceededDialog.value = StorageState.Orange
        }

        else -> {
            Timber.e(exception)
            snackBarHostState.showAutoDurationSnackbar(context.getString(R.string.general_error))
        }
    }
}

private suspend fun consumeMessage(
    event: StartTransferEvent.Message,
    snackBarHostState: SnackbarHostState,
    context: Context,
    navigateToStorageSettings: () -> Unit,
    previewFile: (File) -> Unit,
) {
    //show snack bar with an optional action
    val result = snackBarHostState.showAutoDurationSnackbar(
        context.getString(event.message, *event.messageArgs),
        event.action?.let { context.getString(it) }
    )
    if (result == SnackbarResult.ActionPerformed && event.actionEvent != null) {
        consumeMessageAction(
            event.actionEvent,
            navigateToStorageSettings,
            previewFile,
        )
    }
}

private fun consumeMessageAction(
    actionEvent: StartTransferEvent.Message.ActionEvent,
    navigateToStorageSettings: () -> Unit,
    previewFile: (File) -> Unit,
) = when (actionEvent) {
    StartTransferEvent.Message.ActionEvent.GoToFileManagement -> {
        navigateToStorageSettings()
    }

    is StartTransferEvent.Message.ActionEvent.OpenPreview -> {
        previewFile(actionEvent.file)
    }
}
