package mega.privacy.android.app.presentation.transfers.view.failed

import android.net.Uri
import android.os.Parcelable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.parcelize.Parcelize
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.transfers.model.image.CompletedTransferImageViewModel
import mega.privacy.android.app.presentation.transfers.view.EmptyTransfersView
import mega.privacy.android.app.presentation.transfers.view.TEST_TAG_FAILED_TAB
import mega.privacy.android.app.presentation.transfers.view.sheet.FailedTransferActionsBottomSheet
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.feature.transfers.components.FailedTransferItem
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.FailedTransfersItemMoreOptionsMenuItemEvent
import mega.privacy.mobile.analytics.event.FailedTransfersItemTapAndHoldSelectedEvent

@Composable
internal fun FailedTransfersView(
    failedTransfers: ImmutableList<CompletedTransfer>,
    selectedFailedTransfersIds: ImmutableList<Int>?,
    onFailedTransferSelected: (CompletedTransfer) -> Unit,
    onRetryTransfer: (CompletedTransfer) -> Unit,
    lazyListState: LazyListState,
    modifier: Modifier = Modifier,
) {
    val selectMode =
        remember(selectedFailedTransfersIds) { selectedFailedTransfersIds != null }
    var failedItemSelected by rememberSaveable { mutableStateOf<FailedItemSelected?>(null) }

    if (failedTransfers.isEmpty()) {
        EmptyTransfersView(
            emptyStringId = sharedR.string.transfers_no_failed_transfers_empty_text,
            modifier = Modifier.testTag(TEST_TAG_FAILED_TRANSFERS_EMPTY_VIEW)
        )
    } else {
        LazyColumn(
            state = lazyListState,
            modifier = modifier
                .fillMaxSize()
                .testTag(TEST_TAG_FAILED_TRANSFERS_VIEW)
        ) {
            items(items = failedTransfers, key = { it.id ?: 0 }) { item ->
                FailedTransferItem(
                    failedTransfer = item,
                    isSelected = selectedFailedTransfersIds?.contains(item.id),
                    modifier = Modifier.then(
                        if (selectMode) {
                            Modifier.clickable { onFailedTransferSelected(item) }
                        } else {
                            Modifier.combinedClickable(
                                onClick = { },
                                onLongClick = {
                                    Analytics.tracker.trackEvent(
                                        FailedTransfersItemTapAndHoldSelectedEvent
                                    )
                                    onFailedTransferSelected(item)
                                },
                            )
                        }
                    ),
                    onMoreClicked = {
                        Analytics.tracker.trackEvent(FailedTransfersItemMoreOptionsMenuItemEvent)
                        failedItemSelected = it
                    }
                )
            }
        }
    }

    failedItemSelected?.let { failedItem ->
        with(failedItem) {
            val failedTransfer = failedTransfers.firstOrNull { it.id == failedTransferId }
                ?: return@let

            FailedTransferActionsBottomSheet(
                failedTransfer = failedTransfer,
                fileTypeResId = fileTypeResId,
                previewUri = previewUri,
                onRetryTransfer = onRetryTransfer,
                onDismissSheet = { failedItemSelected = null },
            )
        }
    }
}

@Composable
internal fun FailedTransferItem(
    failedTransfer: CompletedTransfer,
    isSelected: Boolean?,
    modifier: Modifier = Modifier,
    viewModel: CompletedTransferImageViewModel = hiltViewModel(),
    onMoreClicked: (FailedItemSelected) -> Unit,
) = with(failedTransfer) {
    id?.let {
        val uiState by viewModel.getUiStateFlow(it).collectAsStateWithLifecycle()

        LaunchedEffect(key1 = it) {
            viewModel.addTransfer(failedTransfer)
        }

        FailedTransferItem(
            isDownload = type.isDownloadType(),
            fileTypeResId = uiState.fileTypeResId,
            previewUri = uiState.previewUri,
            fileName = fileName,
            isSelected = isSelected,
            error = String.format("%s: %s", stringResource(R.string.failed_label), error)
                .takeIf { state != TransferState.STATE_CANCELLED },
            modifier = modifier,
            onMoreClicked = {
                failedTransfer.id?.let { id ->
                    FailedItemSelected(
                        failedTransferId = id,
                        fileTypeResId = uiState.fileTypeResId,
                        previewUri = uiState.previewUri,
                    ).let { item -> onMoreClicked(item) }
                }
            }
        )
    }
}

@Parcelize
internal class FailedItemSelected(
    val failedTransferId: Int,
    val fileTypeResId: Int?,
    val previewUri: Uri?,
) : Parcelable

internal const val TEST_TAG_FAILED_TRANSFERS_VIEW = "$TEST_TAG_FAILED_TAB:view"
internal const val TEST_TAG_FAILED_TRANSFERS_EMPTY_VIEW = "$TEST_TAG_FAILED_TAB:empty_view"