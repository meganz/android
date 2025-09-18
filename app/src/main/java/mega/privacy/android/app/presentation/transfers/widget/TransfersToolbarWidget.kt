package mega.privacy.android.app.presentation.transfers.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.palm.composestateevents.EventEffect
import mega.privacy.android.app.presentation.transfers.TransfersActivity
import mega.privacy.android.feature.transfers.components.widget.TransfersToolbarWidgetViewAnimated

/**
 * Widget to show current transfers progress in the toolbar
 */
@Composable
fun TransfersToolbarWidget(
    modifier: Modifier = Modifier,
    viewModel: TransfersWidgetViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    EventEffect(
        event = state.openTransfersSectionEvent,
        onConsumed = viewModel::onConsumeOpenTransfersSectionEvent
    ) {
        context.startActivity(TransfersActivity.getIntent(context))
    }
    TransfersToolbarWidgetViewAnimated(
        transfersToolbarWidgetStatus = state.transfersToolbarWidgetStatus,
        totalSizeAlreadyTransferred = state.transfersInfo.totalSizeAlreadyTransferred,
        totalSizeToTransfer = state.transfersInfo.totalSizeToTransfer,
        modifier = modifier,
        onClick = viewModel::openTransfers,
    )
}