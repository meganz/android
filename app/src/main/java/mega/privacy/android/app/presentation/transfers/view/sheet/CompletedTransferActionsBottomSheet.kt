package mega.privacy.android.app.presentation.transfers.view.sheet

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.android.core.ui.components.sheets.MegaModalBottomSheet
import mega.android.core.ui.components.sheets.MegaModalBottomSheetBackground
import mega.privacy.android.app.R
import mega.privacy.android.app.getLink.GetLinkActivity
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.filestorage.FileStorageActivity
import mega.privacy.android.app.presentation.transfers.model.completed.CompletedTransferActionsUiState
import mega.privacy.android.app.presentation.transfers.model.completed.CompletedTransferActionsViewModel
import mega.privacy.android.app.presentation.transfers.model.completed.OpenWithEvent
import mega.privacy.android.app.presentation.transfers.model.completed.ShareLinkEvent
import mega.privacy.android.app.presentation.transfers.view.completed.TEST_TAG_COMPLETED_TRANSFERS_VIEW
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.MegaApiUtils
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.feature.shared.components.BottomSheetAction
import mega.privacy.android.feature.transfers.components.CompletedTransferBottomSheetHeader
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.resources.R as sharedR
import timber.log.Timber
import kotlin.time.Duration.Companion.milliseconds

/**
 * Bottom sheet for a completed transfer actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompletedTransferActionsBottomSheet(
    completedTransfer: CompletedTransfer,
    fileTypeResId: Int?,
    previewUri: Uri?,
    onDismissSheet: () -> Unit,
) {
    val viewModel = hiltViewModel<CompletedTransferActionsViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(key1 = completedTransfer.id) {
        viewModel.checkCompletedTransferActions(completedTransfer)
    }

    CompletedTransferActionsBottomSheet(
        completedTransfer = completedTransfer,
        fileTypeResId = fileTypeResId,
        previewUri = previewUri,
        uiState = uiState,
        onOpenWith = viewModel::openWith,
        onShareLink = viewModel::shareLink,
        onClearTransfer = viewModel::clearTransfer,
        onConsumeOpenWithEvent = viewModel::onConsumeOpenWithEvent,
        onConsumeShareLinkEvent = viewModel::onConsumeShareLinkEvent,
        onDismissSheet = onDismissSheet,
        modifier = Modifier,
        sheetState = rememberModalBottomSheetState(),
    )
}

/**
 * Bottom sheet for a completed transfer actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompletedTransferActionsBottomSheet(
    completedTransfer: CompletedTransfer,
    fileTypeResId: Int?,
    previewUri: Uri?,
    uiState: CompletedTransferActionsUiState,
    onOpenWith: (CompletedTransfer) -> Unit,
    onShareLink:(Long) -> Unit,
    onClearTransfer: (CompletedTransfer) -> Unit,
    onConsumeOpenWithEvent: () -> Unit,
    onConsumeShareLinkEvent: () -> Unit,
    onDismissSheet: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
) = with(completedTransfer) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = LocalSnackBarHostState.current

    MegaModalBottomSheet(
        bottomSheetBackground = MegaModalBottomSheetBackground.Surface1,
        onDismissRequest = onDismissSheet,
        modifier = modifier
            .semantics { testTagsAsResourceId = true }
            .testTag(TEST_TAG_COMPLETED_TRANSFER_ACTIONS_PANEL)
            .fillMaxWidth(),
        sheetState = sheetState,
    ) {
        CompletedTransferBottomSheetHeader(
            fileName = fileName,
            size = size,
            date = TimeUtils.formatLongDateTime(timestamp.milliseconds.inWholeSeconds),
            fileTypeResId = fileTypeResId,
            previewUri = previewUri,
            modifier = Modifier.testTag(TEST_TAG_COMPLETED_TRANSFER),
        )
        if (uiState.canViewInFolder) {
            BottomSheetAction(
                modifier = Modifier.testTag(TEST_TAG_VIEW_IN_FOLDER_ACTION),
                iconPainter = IconPack.Medium.Thin.Outline.FileSearch02,
                name = stringResource(id = R.string.view_in_folder_label),
                onClick = {
                    if (uiState.isOnline(coroutineScope, snackbarHostState, context)) {
                        activity?.let {
                            onViewInFolder(completedTransfer, uiState.parentUri, it)
                        }
                    }
                    onDismissSheet()
                },
            )
        }
        if (uiState.canOpenWith) {
            BottomSheetAction(
                modifier = Modifier.testTag(TEST_TAG_OPEN_WITH_ACTION),
                iconPainter = IconPack.Medium.Thin.Outline.ExternalLink,
                name = stringResource(id = R.string.external_play),
                onClick = { onOpenWith(completedTransfer) },
            )
        }
        if (uiState.canShareLink) {
            BottomSheetAction(
                modifier = Modifier.testTag(TEST_TAG_SHARE_LINK_ACTION),
                iconPainter = IconPack.Medium.Thin.Outline.Link01,
                name = stringResource(id = R.string.context_get_link),
                onClick = {
                    if (uiState.isOnline(coroutineScope, snackbarHostState, context)) {
                        onShareLink(handle)
                    }
                },
            )
        }
        BottomSheetAction(
            modifier = Modifier.testTag(TEST_TAG_CLEAR_ACTION),
            iconPainter = IconPack.Medium.Thin.Outline.Eraser,
            name = stringResource(id = R.string.general_clear),
            onClick = {
                onClearTransfer(completedTransfer)
                onDismissSheet()
            },
        )
    }

    EventEffect(
        event = uiState.openWithEvent,
        onConsumed = onConsumeOpenWithEvent,
        action = { event ->
            activity?.let {
                onOpenWith(
                    openWithEvent = event,
                    fileName = fileName,
                    activity = it,
                    coroutineScope = coroutineScope,
                    snackbarHostState = snackbarHostState
                )
            }
            onDismissSheet()
        },
    )

    EventEffect(
        event = uiState.shareLinkEvent,
        onConsumed = onConsumeShareLinkEvent,
        action = { event ->
            activity?.let {
                shareLink(
                    shareLinkEvent = event,
                    activity = it,
                    coroutineScope = coroutineScope,
                    snackbarHostState = snackbarHostState
                )
            }
            onDismissSheet()
        },
    )
}

internal fun CompletedTransferActionsUiState.isOnline(
    coroutineScope: CoroutineScope,
    snackbarHostState: SnackbarHostState?,
    context: Context,
) = if (isOnline) {
    true
} else {
    coroutineScope.launch {
        snackbarHostState?.showSnackbar(context.getString(R.string.error_server_connection_problem))
    }
    false
}

private fun onOpenWith(
    openWithEvent: OpenWithEvent,
    fileName: String,
    activity: Activity,
    coroutineScope: CoroutineScope,
    snackbarHostState: SnackbarHostState?,
) {
    with(openWithEvent) {
        if (isValid) {
            val uri = file?.let {
                runCatching {
                    FileProvider.getUriForFile(
                        activity,
                        Constants.AUTHORITY_STRING_FILE_PROVIDER,
                        it
                    )
                }.onFailure {
                    Timber.e(it)
                }.getOrNull()
            } ?: uri

            if (uri != null) {
                Intent(Intent.ACTION_VIEW).apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    runCatching {
                        setDataAndType(uri, fileType)
                        val chooserTitle =
                            activity.getString(sharedR.string.open_with_os_dialog_title, fileName)
                        val intent = Intent.createChooser(this, chooserTitle)

                        if (MegaApiUtils.isIntentAvailable(activity, intent)) {
                            activity.startActivity(intent)
                            return
                        }
                    }.onFailure { Timber.e(it) }
                }

                coroutineScope.launch {
                    snackbarHostState?.showSnackbar(activity.getString(R.string.intent_not_available))
                }
            }

            return
        }

        coroutineScope.launch {
            snackbarHostState?.showSnackbar(activity.getString(R.string.corrupt_video_dialog_text))
        }
    }
}

private fun onViewInFolder(
    completedTransfer: CompletedTransfer,
    parentUri: Uri?,
    activity: Activity,
) {
    with(completedTransfer) {
        Intent(activity, ManagerActivity::class.java).apply {
            if (completedTransfer.type.isDownloadType()) {
                val path = parentUri.toString().takeUnless { it.isBlank() } ?: path
                action = Constants.ACTION_LOCATE_DOWNLOADED_FILE
                putExtra(Constants.INTENT_EXTRA_IS_OFFLINE_PATH, isOffline == true)
                putExtra(FileStorageActivity.EXTRA_PATH, path)
                putStringArrayListExtra(FileStorageActivity.EXTRA_FILE_NAMES, arrayListOf(fileName))

            } else {
                action = Constants.ACTION_OPEN_FOLDER
                putExtra(Constants.INTENT_EXTRA_KEY_PARENT_HANDLE, handle)
                putStringArrayListExtra(FileStorageActivity.EXTRA_FILE_NAMES, arrayListOf(fileName))
            }
        }.let { activity.startActivity(it) }
    }
}

private fun shareLink(
    shareLinkEvent: ShareLinkEvent,
    activity: Activity,
    coroutineScope: CoroutineScope,
    snackbarHostState: SnackbarHostState?,
) {
    with(shareLinkEvent) {
        when {
            isTakenDown -> coroutineScope.launch {
                snackbarHostState?.showSnackbar(activity.getString(R.string.error_download_takendown_node))
            }

            isValid -> activity.startActivity(
                Intent(activity, GetLinkActivity::class.java)
                    .putExtra(Constants.HANDLE, node?.id?.longValue)
            )

            else -> coroutineScope.launch {
                snackbarHostState?.showSnackbar(activity.getString(R.string.warning_node_not_exists_in_cloud))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@CombinedThemePreviews
@Composable
private fun CompletedTransferActionsBottomSheetPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        CompletedTransferActionsBottomSheet(
            completedTransfer = CompletedTransfer(
                id = 0,
                fileName = "2023-03-24 00.13.20_1.pdf",
                type = TransferType.GENERAL_UPLOAD,
                state = TransferState.STATE_COMPLETED,
                size = "3.57 MB",
                handle = 27169983390750L,
                path = "Cloud drive/Camera uploads",
                displayPath = "Cloud drive/Camera uploads",
                isOffline = false,
                timestamp = 1684228012974L,
                error = "No error",
                errorCode = 0,
                originalPath = "/original/path/2023-03-24 00.13.20_1.pdf",
                parentHandle = 11622336899311L,
                appData = emptyList(),
            ),
            fileTypeResId = iconPackR.drawable.ic_pdf_medium_solid,
            previewUri = null,
            uiState = CompletedTransferActionsUiState(),
            onOpenWith = {},
            onShareLink = {},
            onClearTransfer = {},
            onConsumeOpenWithEvent = {},
            onConsumeShareLinkEvent = {},
            onDismissSheet = {},
        )
    }
}

internal const val TEST_TAG_COMPLETED_TRANSFER_ACTIONS_PANEL =
    "$TEST_TAG_COMPLETED_TRANSFERS_VIEW:transfer_actions_panel"
internal const val TEST_TAG_COMPLETED_TRANSFER =
    "$TEST_TAG_COMPLETED_TRANSFER_ACTIONS_PANEL:completed_transfer"
internal const val TEST_TAG_VIEW_IN_FOLDER_ACTION =
    "$TEST_TAG_COMPLETED_TRANSFER_ACTIONS_PANEL:view_in_folder"
internal const val TEST_TAG_OPEN_WITH_ACTION =
    "$TEST_TAG_COMPLETED_TRANSFER_ACTIONS_PANEL:open_with"
internal const val TEST_TAG_SHARE_LINK_ACTION =
    "$TEST_TAG_COMPLETED_TRANSFER_ACTIONS_PANEL:share_link"
internal const val TEST_TAG_CLEAR_ACTION =
    "$TEST_TAG_COMPLETED_TRANSFER_ACTIONS_PANEL:clear"