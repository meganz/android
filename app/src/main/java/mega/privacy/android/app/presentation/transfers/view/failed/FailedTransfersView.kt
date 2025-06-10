package mega.privacy.android.app.presentation.transfers.view.failed

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.ImmutableList
import mega.privacy.android.app.presentation.transfers.model.image.CompletedTransferImageViewModel
import mega.privacy.android.app.presentation.transfers.view.EmptyTransfersView
import mega.privacy.android.app.presentation.transfers.view.TEST_TAG_FAILED_TAB
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.feature.transfers.components.FailedTransferItem
import mega.privacy.android.shared.resources.R as sharedR

@Composable
internal fun FailedTransfersView(
    failedTransfers: ImmutableList<CompletedTransfer>,
    selectedFailedTransfersIds: ImmutableList<Int>?,
    onFailedTransferSelected: (CompletedTransfer) -> Unit,
    lazyListState: LazyListState,
    modifier: Modifier = Modifier,
) {
    val selectMode =
        remember(selectedFailedTransfersIds) { selectedFailedTransfersIds != null }
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
                    isSelected = selectedFailedTransfersIds?.contains(item.id) == true,
                    modifier = Modifier.then(
                        if (selectMode) {
                            Modifier.clickable { onFailedTransferSelected(item) }
                        } else {
                            Modifier.combinedClickable(
                                onClick = { },
                                onLongClick = { onFailedTransferSelected(item) },
                            )
                        }
                    ),
                )
            }
        }
    }
}

@Composable
internal fun FailedTransferItem(
    failedTransfer: CompletedTransfer,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    viewModel: CompletedTransferImageViewModel = hiltViewModel(),
) = with(failedTransfer) {
    id?.let {
        val uiState by viewModel.getUiStateFlow(it).collectAsStateWithLifecycle()

        LaunchedEffect(key1 = it) {
            viewModel.addTransfer(failedTransfer)
        }

        FailedTransferItem(
            isDownload = true,
            fileTypeResId = uiState.fileTypeResId,
            previewUri = uiState.previewUri,
            fileName = fileName,
            isSelected = isSelected,
            error = error.takeIf { state != TransferState.STATE_CANCELLED },
            modifier = modifier,
        )
    }
}

internal const val TEST_TAG_FAILED_TRANSFERS_VIEW = "$TEST_TAG_FAILED_TAB:view"
internal const val TEST_TAG_FAILED_TRANSFERS_EMPTY_VIEW = "$TEST_TAG_FAILED_TAB:empty_view"