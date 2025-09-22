package mega.privacy.android.core.transfers.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.feature.transfers.components.widget.TransfersToolbarWidgetViewAnimated

/**
 * Widget to show current transfers progress in the toolbar
 */
@Composable
fun TransfersToolbarWidget(
    modifier: Modifier = Modifier,
    viewModel: TransfersToolbarWidgetViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    TransfersToolbarWidgetViewAnimated(
        transfersToolbarWidgetStatus = state.status,
        totalSizeAlreadyTransferred = state.totalSizeAlreadyTransferred,
        totalSizeToTransfer = state.totalSizeToTransfer,
        modifier = modifier,
        onClick = {
            if (state.isUserLoggedIn) {
                //TODO navigation to transfers:
                // context.startActivity(TransfersActivity.getIntent(context))
            }
        },
    )
}