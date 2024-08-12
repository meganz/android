package mega.privacy.android.app.presentation.transfers.view.inprogress

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.ImmutableList
import mega.privacy.android.app.presentation.extensions.transfers.getProgressString
import mega.privacy.android.app.presentation.extensions.transfers.getSpeedString
import mega.privacy.android.app.presentation.transfers.model.image.InProgressTransferImageViewModel
import mega.privacy.android.app.presentation.transfers.view.TEST_TAG_IN_PROGRESS_TAB
import mega.privacy.android.domain.entity.transfer.InProgressTransfer
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.shared.original.core.ui.controls.transfers.InProgressTransferItem

@Composable
internal fun InProgressTransfersView(
    inProgressTransfers: ImmutableList<InProgressTransfer>,
    isOverQuota: Boolean,
    areTransfersPaused: Boolean,
    onPlayPauseClicked: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag(TEST_TAG_IN_PROGRESS_TRANSFERS_VIEW)
    ) {
        items(
            items = inProgressTransfers,
            key = { it.tag },
        ) { item ->
            InProgressTransferItem(
                inProgressTransfer = item,
                isOverQuota = isOverQuota,
                areTransfersPaused = areTransfersPaused,
                onPlayPauseClicked = onPlayPauseClicked,
            )
        }
    }
}

@Composable
internal fun InProgressTransferItem(
    inProgressTransfer: InProgressTransfer,
    isOverQuota: Boolean,
    areTransfersPaused: Boolean,
    onPlayPauseClicked: (Int) -> Unit,
    viewModel: InProgressTransferImageViewModel = hiltViewModel(),
) = with(inProgressTransfer) {
    val context = LocalContext.current
    val uiState by viewModel.getUiStateFlow(tag).collectAsStateWithLifecycle()

    LaunchedEffect(key1 = tag) {
        viewModel.addTransfer(inProgressTransfer)
    }

    InProgressTransferItem(
        tag = tag,
        isDownload = inProgressTransfer is InProgressTransfer.Download,
        fileTypeResId = uiState.fileTypeResId,
        previewUri = uiState.previewUri,
        fileName = fileName,
        progress = getProgressString(context, isOverQuota),
        speed = getSpeedString(context, areTransfersPaused),
        isPaused = isPaused,
        isQueued = state == TransferState.STATE_QUEUED,
        isOverQuota = isOverQuota,
        areTransfersPaused = areTransfersPaused,
        onPlayPauseClicked = { onPlayPauseClicked(tag) })
}

internal const val TEST_TAG_IN_PROGRESS_TRANSFERS_VIEW = "$TEST_TAG_IN_PROGRESS_TAB:view"