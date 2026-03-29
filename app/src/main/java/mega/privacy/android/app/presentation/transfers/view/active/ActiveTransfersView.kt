package mega.privacy.android.app.presentation.transfers.view.active

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.android.core.ui.components.list.MegaReorderableLazyColumn
import mega.android.core.ui.components.scrollbar.fastscroll.FastScrollForLazyColumn
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.presentation.snackbar.SnackbarHostStateWrapper
import mega.privacy.android.app.presentation.snackbar.showAutoDurationSnackbar
import mega.privacy.android.app.presentation.transfers.model.image.ActiveTransferImageViewModel
import mega.privacy.android.app.presentation.transfers.view.EmptyTransfersView
import mega.privacy.android.app.presentation.transfers.view.TEST_TAG_ACTIVE_TAB
import mega.privacy.android.shared.account.overquota.model.OverQuotaStatus
import mega.privacy.android.shared.account.overquota.view.OverQuotaBanner
import mega.privacy.android.core.transfers.extension.getProgressPercentString
import mega.privacy.android.core.transfers.extension.getProgressSizeString
import mega.privacy.android.core.transfers.extension.getSpeedString
import mega.privacy.android.domain.entity.transfer.InProgressTransfer
import mega.privacy.android.feature.transfers.components.ActiveTransferItem
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.ActiveTransfersIndividualPauseButtonButtonPressedEvent
import mega.privacy.mobile.analytics.event.ActiveTransfersIndividualPlayButtonButtonPressedEvent
import mega.privacy.mobile.analytics.event.ActiveTransfersSwipeToCancelEvent
import mega.privacy.mobile.analytics.event.ActiveTransfersUndoSwipeToCancelSnackbarActionEvent

@Composable
internal fun ActiveTransfersView(
    activeTransfers: List<InProgressTransfer>,
    selectedActiveTransfersIds: List<Long>?,
    hasInternetConnection: Boolean,
    overQuotaStatus: OverQuotaStatus,
    areTransfersPaused: Boolean,
    enableSwipeToDismiss: Boolean,
    onPlayPauseClicked: (tag: Int) -> Unit,
    onReorderPreview: suspend (from: Int, to: Int) -> Unit,
    onReorderConfirmed: (InProgressTransfer) -> Unit,
    onActiveTransferSelected: (InProgressTransfer) -> Unit,
    onUpgradeClick: () -> Unit,
    onConsumeQuotaWarning: () -> Unit,
    onCancelActiveTransfer: (InProgressTransfer) -> Unit,
    onSetActiveTransferToCancel: (InProgressTransfer) -> Unit,
    onUndoCancelActiveTransfer: (InProgressTransfer) -> Unit,
    isTabSelected: Boolean,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState(),
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
) {
    val selectMode = remember(selectedActiveTransfersIds) { selectedActiveTransfersIds != null }
    var draggedTransfer by remember { mutableStateOf<InProgressTransfer?>(null) }
    val localSnackbarHostState = LocalSnackBarHostState.current
    val snackBarHostState = LocalSnackBarHostState.current?.let {
        SnackbarHostStateWrapper(it)
    }
    val resources = LocalResources.current

    if (activeTransfers.isEmpty()) {
        EmptyTransfersView(
            emptyStringId = sharedR.string.transfers_no_active_transfers_empty_text,
            modifier = Modifier.testTag(TEST_TAG_ACTIVE_TRANSFERS_EMPTY_VIEW)
        )
    } else {
        Column {
            OverQuotaBanner(
                overQuotaStatus = overQuotaStatus,
                modifier = Modifier.testTag(OVER_QUOTA_BANNER_TAG),
                isBlockingAware = true,
                forceRiceTopAppBar = isTabSelected,
                onUpgradeClicked = onUpgradeClick,
                onDismissed = onConsumeQuotaWarning,
            )
            FastScrollForLazyColumn(
                totalItems = activeTransfers.size,
                modifier = modifier.fillMaxSize(),
                state = lazyListState,
            ) { state ->
                MegaReorderableLazyColumn(
                    lazyListState = state,
                    items = activeTransfers,
                    key = { it.tag },
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(TEST_TAG_ACTIVE_TRANSFERS_VIEW),
                    onMove = { from, to -> onReorderPreview(from.index, to.index) },
                    onDragStarted = { dragged, _ ->
                        draggedTransfer = dragged
                    },
                    onDragStopped = {
                        draggedTransfer = null
                        onReorderConfirmed(it)
                    },
                    dragEnabled = { !selectMode }
                ) { item ->
                    ActiveTransferItem(
                        activeTransfer = item,
                        overQuotaStatus = overQuotaStatus,
                        hasInternetConnection = hasInternetConnection,
                        areTransfersPaused = areTransfersPaused,
                        enableSwipeToDismiss = enableSwipeToDismiss,
                        isSelected = selectedActiveTransfersIds?.contains(item.uniqueId),
                        isDraggable = selectedActiveTransfersIds == null,
                        isBeingDragged = item == draggedTransfer,
                        onPlayPauseClicked = { onPlayPauseClicked(item.tag) },
                        onSetToCancel = {
                            onSetActiveTransferToCancel(item)

                            coroutineScope.launch {
                                localSnackbarHostState?.currentSnackbarData?.dismiss()
                                val result = snackBarHostState.showAutoDurationSnackbar(
                                    resources.getString(sharedR.string.transfers_transfer_cancelled),
                                    resources.getString(sharedR.string.general_undo),
                                )
                                when (result) {
                                    SnackbarResult.ActionPerformed -> {
                                        Analytics.tracker.trackEvent(
                                            ActiveTransfersUndoSwipeToCancelSnackbarActionEvent
                                        )
                                        onUndoCancelActiveTransfer(item)
                                    }

                                    SnackbarResult.Dismissed -> {
                                        onCancelActiveTransfer(item)
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .animateItem()
                            .clickable(enabled = selectMode) {
                                onActiveTransferSelected(item)
                            }
                    )
                }
            }
        }
    }
}

@Composable
internal fun ActiveTransferItem(
    activeTransfer: InProgressTransfer,
    overQuotaStatus: OverQuotaStatus,
    areTransfersPaused: Boolean,
    hasInternetConnection: Boolean,
    isSelected: Boolean?,
    isDraggable: Boolean,
    isBeingDragged: Boolean,
    enableSwipeToDismiss: Boolean,
    onPlayPauseClicked: () -> Unit,
    onSetToCancel: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ActiveTransferImageViewModel = hiltViewModel(),
) = with(activeTransfer) {
    val uiState by viewModel.getUiStateFlow(tag).collectAsStateWithLifecycle()
    val isDownload = activeTransfer is InProgressTransfer.Download

    LaunchedEffect(key1 = tag) {
        viewModel.addTransfer(activeTransfer)
    }

    ActiveTransferItem(
        tag = tag,
        isDownload = isDownload,
        fileTypeResId = uiState.fileTypeResId,
        previewUri = uiState.previewUri,
        fileName = fileName,
        progressSizeString = getProgressSizeString(),
        progressPercentageString = getProgressPercentString(),
        progress = progress.floatValue,
        speed = getSpeedString(
            areTransfersPaused = areTransfersPaused,
            isTransferOverQuota = overQuotaStatus.isDownloadBlocked && isDownload,
            isStorageOverQuota = overQuotaStatus.isUploadBlocked && isDownload.not(),
        ),
        isPaused = isPaused,
        hasIssues = (isDownload && overQuotaStatus.hasTransferIssue) || (isDownload.not() && overQuotaStatus.hasStorageIssue) || !hasInternetConnection,
        areTransfersPaused = areTransfersPaused,
        enableSwipeToDismiss = enableSwipeToDismiss,
        onPlayPauseClicked = {
            onPlayPauseClicked()
            Analytics.tracker.trackEvent(
                if (isPaused) ActiveTransfersIndividualPlayButtonButtonPressedEvent
                else ActiveTransfersIndividualPauseButtonButtonPressedEvent
            )
        },
        onSetToCancel = {
            Analytics.tracker.trackEvent(ActiveTransfersSwipeToCancelEvent)
            onSetToCancel()
        },
        modifier = modifier,
        isSelected = isSelected,
        isDraggable = isDraggable,
        isBeingDragged = isBeingDragged,
    )
}

internal const val TEST_TAG_ACTIVE_TRANSFERS_VIEW = "$TEST_TAG_ACTIVE_TAB:view"
internal const val TEST_TAG_ACTIVE_TRANSFERS_EMPTY_VIEW = "$TEST_TAG_ACTIVE_TAB:empty_view"
internal const val OVER_QUOTA_BANNER_TAG = "transfers_view:over_quota_banner"