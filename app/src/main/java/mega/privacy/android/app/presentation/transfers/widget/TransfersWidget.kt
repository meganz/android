package mega.privacy.android.app.presentation.transfers.widget

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.palm.composestateevents.EventEffect
import mega.privacy.android.app.presentation.transfers.TransfersActivity
import mega.privacy.android.shared.original.core.ui.controls.widgets.TransfersWidgetViewAnimated
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme


/**
 * Widget to show current transfers progress with animated visibility
 */
@Composable
fun TransfersWidget(
    modifier: Modifier = Modifier,
    viewModel: TransfersWidgetViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    EventEffect(event = state.openTransfersSectionEvent, onConsumed = viewModel::onConsumeOpenTransfersSectionEvent) {
        context.startActivity(TransfersActivity.getIntent(context))
    }
    TransfersWidgetViewAnimated(
        transfersInfo = state.transfersInfo,
        onClick = viewModel::openTransfers,
        modifier = modifier,
    )
}

/**
 * Sets a transfers widget as the content of this ComposeView.
 *
 */
fun ComposeView.setTransfersWidgetContent() {
    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
    setContent {
        OriginalTheme(isDark = isSystemInDarkTheme()) {
            TransfersWidget(
                modifier = Modifier.padding(bottom = 16.dp, end = 16.dp, start = 16.dp),
            )
        }
    }
}