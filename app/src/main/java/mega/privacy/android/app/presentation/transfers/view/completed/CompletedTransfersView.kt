package mega.privacy.android.app.presentation.transfers.view.completed

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
import mega.privacy.android.app.presentation.transfers.model.image.CompletedTransferImageViewModel
import mega.privacy.android.app.presentation.transfers.view.TEST_TAG_COMPLETED_TAB
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.feature.transfers.components.CompletedTransferItem

@Composable
internal fun CompletedTransfersView(
    completedTransfers: ImmutableList<CompletedTransfer>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag(TEST_TAG_COMPLETED_TRANSFERS_VIEW)
    ) {
        items(items = completedTransfers, key = { it.id ?: 0 }) { item ->
            CompletedTransferItem(completedTransfer = item)
        }
    }
}

@Composable
internal fun CompletedTransferItem(
    completedTransfer: CompletedTransfer,
    viewModel: CompletedTransferImageViewModel = hiltViewModel(),
) = with(completedTransfer) {
    id?.let {
        val uiState by viewModel.getUiStateFlow(it).collectAsStateWithLifecycle()

        LaunchedEffect(key1 = id) {
            viewModel.addTransfer(completedTransfer)
        }

        CompletedTransferItem(
            isDownload = true,
            fileTypeResId = uiState.fileTypeResId,
            previewUri = uiState.previewUri,
            fileName = fileName,
            location = path,
            error = error,
        )
    }
}

internal const val TEST_TAG_COMPLETED_TRANSFERS_VIEW = "$TEST_TAG_COMPLETED_TAB:view"