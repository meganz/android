package mega.privacy.android.core.transfers.widget

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
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
    viewModel: TransfersToolbarWidgetViewModel = hiltViewModel(
        LocalActivity.current as? ComponentActivity
            ?: checkNotNull(LocalViewModelStoreOwner.current) {
                "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
            }),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    state.status?.let { status ->
        TransfersToolbarWidgetViewAnimated(
            transfersToolbarWidgetStatus = status,
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
}