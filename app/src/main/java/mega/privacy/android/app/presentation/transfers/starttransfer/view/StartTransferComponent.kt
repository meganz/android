package mega.privacy.android.app.presentation.transfers.starttransfer.view

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.View
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
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
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.OverDiskQuotaPaywallActivity
import mega.privacy.android.app.constants.IntentConstants
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.main.dialog.storagestatus.StorageStatusDialogView
import mega.privacy.android.app.myAccount.MyAccountActivity
import mega.privacy.android.app.presentation.node.action.HandleFileAction
import mega.privacy.android.app.presentation.permissions.NotificationsPermissionActivity
import mega.privacy.android.app.presentation.snackbar.LegacySnackBarWrapper
import mega.privacy.android.app.presentation.snackbar.SnackbarHostStateWrapper
import mega.privacy.android.app.presentation.snackbar.showAutoDurationSnackbar
import mega.privacy.android.app.presentation.transfers.preview.LoadingPreviewActivity
import mega.privacy.android.app.presentation.transfers.preview.LoadingPreviewActivity.Companion.EXTRA_ERROR
import mega.privacy.android.app.presentation.transfers.preview.LoadingPreviewActivity.Companion.EXTRA_FILE_PATH
import mega.privacy.android.app.presentation.transfers.preview.LoadingPreviewActivity.Companion.EXTRA_TRANSFER_UNIQUE_ID
import mega.privacy.android.app.presentation.transfers.starttransfer.StartTransfersComponentViewModel
import mega.privacy.android.app.presentation.transfers.starttransfer.model.SaveDestinationInfo
import mega.privacy.android.app.presentation.transfers.starttransfer.model.StartTransferEvent
import mega.privacy.android.app.presentation.transfers.starttransfer.model.StartTransferViewState
import mega.privacy.android.app.presentation.transfers.starttransfer.view.dialog.ResumeChatTransfersDialog
import mega.privacy.android.app.presentation.transfers.starttransfer.view.dialog.ResumePreviewTransfersDialog
import mega.privacy.android.app.presentation.transfers.starttransfer.view.filespermission.FilesPermissionDialog
import mega.privacy.android.app.presentation.transfers.transferoverquota.view.dialog.TransferOverQuotaDialog
import mega.privacy.android.app.presentation.transfers.view.dialog.CancelPreviewDownloadDialog
import mega.privacy.android.app.presentation.transfers.view.dialog.LargeDownloadConfirmationDialog
import mega.privacy.android.app.presentation.transfers.view.dialog.NotEnoughSpaceForUploadDialog
import mega.privacy.android.app.presentation.transfers.view.dialog.TransferInProgressDialog
import mega.privacy.android.app.utils.AlertsAndWarnings
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent.StartUpload
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent.StartUpload.Files
import mega.privacy.android.domain.exception.NotEnoughQuotaMegaException
import mega.privacy.android.domain.exception.QuotaExceededMegaException
import mega.privacy.android.navigation.megaNavigator
import mega.privacy.android.shared.original.core.ui.controls.dialogs.ConfirmationDialog
import mega.privacy.android.shared.original.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.shared.original.core.ui.controls.layouts.LocalSnackBarHostStateOriginal
import mega.privacy.android.shared.original.core.ui.navigation.launchFolderPicker
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.resources.R as sharedR
import timber.log.Timber
import java.io.File
import mega.privacy.android.shared.resources.R as sharedResR

/**
 * Helper compose view to show UI related to starting a download transfer
 * (scanning in progress dialog, not enough space snackbar, start download snackbar, quota exceeded, etc.)
 * @param snackBarHostState optional snackbar to show messages, typically null because it should be injected via LocalSnackBarHostState or LocalSnackBarHostStateM2
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
internal fun StartTransferComponent(
    event: StateEventWithContent<TransferTriggerEvent>,
    onConsumeEvent: () -> Unit,
    areTransferOverQuotaWarningsAllowed: Boolean = true,
    snackBarHostState: SnackbarHostState? = null,
    onScanningFinished: (StartTransferEvent) -> Unit = {},
    viewModel: StartTransfersComponentViewModel = hiltViewModel(),
    onCancelNotEnoughSpaceForUploadDialog: () -> Unit = {},
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
                triggerEvent !is StartUpload.TextFile
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
        NotEnoughSpaceForUploadDialog(onCancel = {
            onCancelNotEnoughSpaceForUploadDialog()
            showStorageOverQuotaWarning = false
        })
    }

    if (areTransferOverQuotaWarningsAllowed) {
        TransferOverQuotaDialog()
    }

    StartTransferComponent(
        uiState = uiState,
        onOneOffEventConsumed = viewModel::consumeOneOffEvent,
        onCancelled = viewModel::cancelCurrentTransfersJob,
        onLargeDownloadAnswered = viewModel::largeDownloadAnswered,
        onDestinationSet = viewModel::startDownloadWithDestination,
        onPromptSaveDestinationConsumed = viewModel::consumePromptSaveDestination,
        onSaveDestination = viewModel::saveDestination,
        onAlwaysAskForDestination = viewModel::alwaysAskForDestination,
        onResumeTransfers = viewModel::resumeTransfers,
        onAskedResumeTransfers = viewModel::setAskedResumeTransfers,
        snackBarHostState = snackBarHostState.orProvided(),
        onScanningFinished = onScanningFinished,
        navigateToStorageSettings = navigateToStorageSettings,
        onPreviewFile = viewModel::previewFile,
        onPreviewOpened = viewModel::consumePreviewFileOpened,
        onCancelTransferConfirmed = viewModel::cancelTransferConfirmed,
        onCancelTransferCancelled = viewModel::cancelTransferCancelled,
        onConsumeCancelTransferResult = viewModel::onConsumeCancelTransferFailure,
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
    onCancelNotEnoughSpaceForUploadDialog: () -> Unit = {},
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
                onCancelNotEnoughSpaceForUploadDialog = onCancelNotEnoughSpaceForUploadDialog,
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
    onAlwaysAskForDestination: () -> Unit,
    onResumeTransfers: () -> Unit,
    onAskedResumeTransfers: () -> Unit,
    snackBarHostState: SnackbarHostStateWrapper?,
    navigateToStorageSettings: () -> Unit,
    onPreviewFile: (File) -> Unit,
    onPreviewOpened: () -> Unit,
    onCancelTransferConfirmed: () -> Unit,
    onCancelTransferCancelled: () -> Unit,
    onConsumeCancelTransferResult: () -> Unit,
    onScanningFinished: (StartTransferEvent) -> Unit = {},
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showOfflineAlertDialog by rememberSaveable { mutableStateOf(false) }
    var showResumeChatUploadsAlertDialog by rememberSaveable { mutableStateOf(false) }
    var showResumePreviewDownloadsAlertDialog by rememberSaveable { mutableStateOf<String?>(null) }
    val showQuotaExceededDialog = rememberSaveable(stateSaver = storageStateSaver) {
        mutableStateOf(null)
    }
    var showErrorMessage: String? by rememberSaveable { mutableStateOf(null) }
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

    val loadingPreviewLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            Timber.d("Fake preview activity result OK")

            result.data?.let {
                it.getStringExtra(EXTRA_FILE_PATH)?.let { path ->
                    onPreviewFile(File(path))
                }
                it.getStringExtra(EXTRA_ERROR)?.let { error ->
                    if (error.isNotEmpty()) {
                        showErrorMessage = error
                        Timber.e("Error in fake preview activity")
                    }
                }
            } ?: Timber.e("Fake preview activity result data is null")
        }
    }

    EventEffect(
        event = uiState.oneOffViewEvent,
        onConsumed = onOneOffEventConsumed,
        action = { event ->
            when (event) {
                is StartTransferEvent.FinishDownloadProcessing -> {
                    onFinishProcessing(
                        event = event,
                        snackBarHostState = snackBarHostState,
                        showQuotaExceededDialog = showQuotaExceededDialog,
                        context = context,
                    )
                    onScanningFinished(event)
                }

                is StartTransferEvent.FinishUploadProcessing -> {
                    val message = (event.triggerEvent as? Files)?.specificStartMessage
                        ?: context.resources.getQuantityString(
                            R.plurals.upload_began,
                            event.totalFiles,
                            event.totalFiles,
                        )
                    snackBarHostState.showAutoDurationSnackbar(message)
                    onScanningFinished(event)
                }

                is StartTransferEvent.Message -> {
                    consumeMessage(
                        event,
                        snackBarHostState,
                        context,
                        navigateToStorageSettings,
                    )
                }

                StartTransferEvent.NotConnected -> {
                    showOfflineAlertDialog = true
                }

                is StartTransferEvent.PausedTransfers -> {
                    when {
                        event.triggerEvent is TransferTriggerEvent.StartChatUpload -> {
                            showResumeChatUploadsAlertDialog = true
                        }

                        event.triggerEvent is TransferTriggerEvent.StartDownloadForPreview -> {
                            showResumePreviewDownloadsAlertDialog =
                                event.triggerEvent.node?.name ?: run {
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
                            event.totalFiles,
                            event.totalFiles,
                        )
                    )
                }

                is StartTransferEvent.SlowDownloadPreviewInProgress -> {
                    loadingPreviewLauncher.launch(
                        Intent(context, LoadingPreviewActivity::class.java).also {
                            it.putExtra(EXTRA_TRANSFER_UNIQUE_ID, event.transferUniqueId)
                            it.putExtra(EXTRA_FILE_PATH, event.transferPath)
                        }
                    )
                }

                StartTransferEvent.PayWall -> {
                    context.startActivity(
                        Intent(context, OverDiskQuotaPaywallActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                        })
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
            confirmButtonText = stringResource(id = sharedResR.string.general_ok),
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
                context.megaNavigator.openUpgradeAccount(context = context)
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
        val isPreview = it.transferTriggerEvent is TransferTriggerEvent.StartDownloadForPreview
        LargeDownloadConfirmationDialog(
            isPreviewDownload = isPreview,
            sizeString = it.sizeString,
            onAllow = {
                onLargeDownloadAnswered(it.transferTriggerEvent, false)
            },
            onAlwaysAllow = {
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
                onAlwaysAskForDestination()
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
        CancelPreviewDownloadDialog(
            onCancelTransfer = onCancelTransferConfirmed,
            onDismiss = onCancelTransferCancelled
        )
    }
    EventEffect(
        event = uiState.cancelTransferFailure,
        onConsumed = onConsumeCancelTransferResult,
        action = {
            snackBarHostState.showAutoDurationSnackbar(
                context.getString(sharedR.string.transfers_error_cancelling_preview_download)
            )
        }
    )
    LaunchedEffect(showErrorMessage) {
        showErrorMessage?.let {
            snackBarHostState.showAutoDurationSnackbar(it)
            showErrorMessage = null
        }
    }
}

private val storageStateSaver = Saver<StorageState?, Int>(
    save = { it?.ordinal },
    restore = { StorageState.entries.getOrNull(it) }
)

private suspend fun onFinishProcessing(
    event: StartTransferEvent.FinishDownloadProcessing,
    snackBarHostState: SnackbarHostStateWrapper?,
    showQuotaExceededDialog: MutableState<StorageState?>,
    context: Context,
) {
    event.exception?.let { exception ->
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
    } ?: run {
        if (event.triggerEvent is TransferTriggerEvent.DownloadTriggerEvent && event.triggerEvent.withStartMessage) {
            snackBarHostState.showAutoDurationSnackbar(
                context.getString(sharedR.string.transfers_download_started_snackbar)
            )
        }
    }

}

private suspend fun consumeMessage(
    event: StartTransferEvent.Message,
    snackBarHostState: SnackbarHostStateWrapper?,
    context: Context,
    navigateToStorageSettings: () -> Unit,
) {
    //show snack bar with an optional action
    val result = snackBarHostState.showAutoDurationSnackbar(
        context.getString(event.message, *event.messageArgs),
        event.action?.let { context.getString(it) }
    )
    if (result == SnackbarResult.ActionPerformed && event.actionEvent != null) {
        consumeMessageAction(event.actionEvent, navigateToStorageSettings)
    }
}

private fun consumeMessageAction(
    actionEvent: StartTransferEvent.Message.ActionEvent,
    navigateToStorageSettings: () -> Unit,
) = when (actionEvent) {
    StartTransferEvent.Message.ActionEvent.GoToFileManagement -> {
        navigateToStorageSettings()
    }
}

@Composable
private fun SnackbarHostState?.orProvided() =
    (this ?: LocalSnackBarHostStateOriginal.current)?.let {
        SnackbarHostStateWrapper(it)
    } ?: LocalSnackBarHostState.current?.let {
        SnackbarHostStateWrapper(it)
    }
