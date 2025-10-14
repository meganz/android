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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.android.core.ui.components.list.MegaReorderableLazyColumn
import mega.android.core.ui.components.scrollbar.fastscroll.FastScrollForLazyColumn
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.presentation.extensions.transfers.getProgressPercentString
import mega.privacy.android.app.presentation.extensions.transfers.getProgressSizeString
import mega.privacy.android.app.presentation.extensions.transfers.getSpeedString
import mega.privacy.android.app.presentation.snackbar.SnackbarHostStateWrapper
import mega.privacy.android.app.presentation.snackbar.showAutoDurationSnackbar
import mega.privacy.android.app.presentation.transfers.model.QuotaWarning
import mega.privacy.android.app.presentation.transfers.model.image.ActiveTransferImageViewModel
import mega.privacy.android.app.presentation.transfers.view.EmptyTransfersView
import mega.privacy.android.app.presentation.transfers.view.TEST_TAG_ACTIVE_TAB
import mega.privacy.android.domain.entity.transfer.InProgressTransfer
import mega.privacy.android.feature.transfers.components.ActiveTransferItem
import mega.privacy.android.feature.transfers.components.OverQuotaBanner
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.ActiveTransfersIndividualPauseButtonButtonPressedEvent
import mega.privacy.mobile.analytics.event.ActiveTransfersIndividualPlayButtonButtonPressedEvent

@Composable
internal fun ActiveTransfersView(
    activeTransfers: List<InProgressTransfer>,
    selectedActiveTransfersIds: List<Long>?,
    isTransferOverQuota: Boolean,
    isStorageOverQuota: Boolean,
    quotaWarning: QuotaWarning?,
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
    val context = LocalContext.current

    if (activeTransfers.isEmpty()) {
        EmptyTransfersView(
            emptyStringId = sharedR.string.transfers_no_active_transfers_empty_text,
            modifier = Modifier.testTag(TEST_TAG_ACTIVE_TRANSFERS_EMPTY_VIEW)
        )
    } else {
        Column {
            quotaWarning?.let {
                OverQuotaBanner(
                    modifier = Modifier.testTag(OVER_QUOTA_BANNER_TAG),
                    isTransferOverQuota = it is QuotaWarning.Transfer || it is QuotaWarning.StorageAndTransfer,
                    isStorageOverQuota = it is QuotaWarning.Storage || it is QuotaWarning.StorageAndTransfer,
                    onUpgradeClick = onUpgradeClick,
                    onCancelButtonClick = onConsumeQuotaWarning,
                )
            }
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
                        isTransferOverQuota = isTransferOverQuota,
                        isStorageOverQuota = isStorageOverQuota,
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
                                    context.getString(sharedR.string.transfers_transfer_cancelled),
                                    context.getString(sharedR.string.general_undo),
                                )
                                when (result) {
                                    SnackbarResult.ActionPerformed ->
                                        onUndoCancelActiveTransfer(item)

                                    SnackbarResult.Dismissed ->
                                        onCancelActiveTransfer(item)
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
    isTransferOverQuota: Boolean,
    isStorageOverQuota: Boolean,
    areTransfersPaused: Boolean,
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
            isTransferOverQuota = isTransferOverQuota && isDownload,
            isStorageOverQuota = isStorageOverQuota && isDownload.not(),
        ),
        isPaused = isPaused,
        isOverQuota = (isDownload && isTransferOverQuota) || (isDownload.not() && isStorageOverQuota),
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
            onSetToCancel()
//            Analytics.tracker.trackEvent()
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