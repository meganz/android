package mega.privacy.android.app.presentation.transfers.view.active

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.ImmutableList
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
    isOverQuota: Boolean,
    areTransfersPaused: Boolean,
    onPlayPauseClicked: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (activeTransfers.isEmpty()) {
        EmptyTransfersView(
            emptyStringId = sharedR.string.transfers_no_active_transfers_empty_text,
            modifier = Modifier.testTag(TEST_TAG_ACTIVE_TRANSFERS_EMPTY_VIEW)
        )
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .testTag(TEST_TAG_ACTIVE_TRANSFERS_VIEW)
        ) {
            items(
                items = activeTransfers,
                key = { it.tag },
            ) { item ->
                ActiveTransferItem(
                    activeTransfer = item,
                    isOverQuota = isOverQuota,
                    areTransfersPaused = areTransfersPaused,
                    onPlayPauseClicked = onPlayPauseClicked,
                )
            }
        }
    }
}

@Composable
internal fun ActiveTransferItem(
    activeTransfer: InProgressTransfer,
    isOverQuota: Boolean,
    areTransfersPaused: Boolean,
    onPlayPauseClicked: (Int) -> Unit,
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
        onPlayPauseClicked = { onPlayPauseClicked(tag) })
}

internal const val TEST_TAG_ACTIVE_TRANSFERS_VIEW = "$TEST_TAG_ACTIVE_TAB:view"
internal const val TEST_TAG_ACTIVE_TRANSFERS_EMPTY_VIEW = "$TEST_TAG_ACTIVE_TAB:empty_view"