package mega.privacy.android.app.presentation.transfers.view.active

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.ImmutableList
import mega.android.core.ui.components.list.MegaReorderableLazyColumn
import mega.privacy.android.app.presentation.extensions.transfers.getProgressPercentString
import mega.privacy.android.app.presentation.extensions.transfers.getProgressSizeString
import mega.privacy.android.app.presentation.extensions.transfers.getSpeedString
import mega.privacy.android.app.presentation.transfers.model.image.ActiveTransferImageViewModel
import mega.privacy.android.app.presentation.transfers.view.EmptyTransfersView
import mega.privacy.android.app.presentation.transfers.view.TEST_TAG_ACTIVE_TAB
import mega.privacy.android.domain.entity.transfer.InProgressTransfer
import mega.privacy.android.feature.transfers.components.ActiveTransferItem
import mega.privacy.android.shared.resources.R as sharedR

@Composable
internal fun ActiveTransfersView(
    activeTransfers: ImmutableList<InProgressTransfer>,
    selectedActiveTransfers: ImmutableList<InProgressTransfer>?,
    isOverQuota: Boolean,
    areTransfersPaused: Boolean,
    onPlayPauseClicked: (Int) -> Unit,
    onReorderPreview: suspend (from: Int, to: Int) -> Unit,
    onReorderConfirmed: (InProgressTransfer) -> Unit,
    onActiveTransferSelected: (InProgressTransfer) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectMode = remember(selectedActiveTransfers) { selectedActiveTransfers != null }
    if (activeTransfers.isEmpty()) {
        EmptyTransfersView(
            emptyStringId = sharedR.string.transfers_no_active_transfers_empty_text,
            modifier = Modifier.testTag(TEST_TAG_ACTIVE_TRANSFERS_EMPTY_VIEW)
        )
    } else {
        MegaReorderableLazyColumn(
            items = activeTransfers,
            key = { it.tag },
            modifier = modifier
                .fillMaxSize()
                .testTag(TEST_TAG_ACTIVE_TRANSFERS_VIEW),
            onMove = { from, to -> onReorderPreview(from.index, to.index) },
            onDragStopped = { onReorderConfirmed(it) },
            dragEnabled = { !selectMode }
        ) { item ->
            ActiveTransferItem(
                activeTransfer = item,
                isOverQuota = isOverQuota,
                areTransfersPaused = areTransfersPaused,
                onPlayPauseClicked = onPlayPauseClicked,
                isSelected = selectedActiveTransfers?.contains(item) == true,
                isDraggable = selectedActiveTransfers == null,
                modifier = Modifier.clickable(enabled = selectMode) {
                    onActiveTransferSelected(item)
                }
            )
        }
    }
}

@Composable
internal fun ActiveTransferItem(
    activeTransfer: InProgressTransfer,
    isOverQuota: Boolean,
    areTransfersPaused: Boolean,
    onPlayPauseClicked: (Int) -> Unit,
    isSelected: Boolean,
    isDraggable: Boolean,
    modifier: Modifier = Modifier,
    viewModel: ActiveTransferImageViewModel = hiltViewModel(),
) = with(activeTransfer) {
    val uiState by viewModel.getUiStateFlow(tag).collectAsStateWithLifecycle()

    LaunchedEffect(key1 = tag) {
        viewModel.addTransfer(activeTransfer)
    }

    ActiveTransferItem(
        tag = tag,
        isDownload = activeTransfer is InProgressTransfer.Download,
        fileTypeResId = uiState.fileTypeResId,
        previewUri = uiState.previewUri,
        fileName = fileName,
        progressSizeString = getProgressSizeString(),
        progressPercentageString = getProgressPercentString(),
        progress = progress.floatValue,
        speed = getSpeedString(areTransfersPaused),
        isPaused = isPaused,
        isOverQuota = isOverQuota,
        areTransfersPaused = areTransfersPaused,
        onPlayPauseClicked = { onPlayPauseClicked(tag) },
        modifier = modifier,
        isSelected = isSelected,
        isDraggable = isDraggable,
    )
}

internal const val TEST_TAG_ACTIVE_TRANSFERS_VIEW = "$TEST_TAG_ACTIVE_TAB:view"
internal const val TEST_TAG_ACTIVE_TRANSFERS_EMPTY_VIEW = "$TEST_TAG_ACTIVE_TAB:empty_view"