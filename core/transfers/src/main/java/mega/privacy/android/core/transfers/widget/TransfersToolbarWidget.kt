package mega.privacy.android.core.transfers.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.feature.transfers.components.widget.TransfersToolbarWidgetViewAnimated
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.destination.Transfers

/**
 * Widget to show current transfers progress in the toolbar
 */
@Composable
fun TransfersToolbarWidget(
    navigationHandler: NavigationHandler,
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
                navigationHandler.navigate(Transfers())
            }
        },
    )
}