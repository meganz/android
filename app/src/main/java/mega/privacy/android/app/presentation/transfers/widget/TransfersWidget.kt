package mega.privacy.android.app.presentation.transfers.widget

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.shared.original.core.ui.controls.widgets.TransfersWidgetViewAnimated
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme


/**
 * Widget to show current transfers progress with animated visibility
 */
@Composable
fun TransfersWidget(
    modifier: Modifier = Modifier,
    viewModel: TransfersWidgetViewModel = hiltViewModel(),
    onOpenTransferSection: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    TransfersWidgetViewAnimated(
        transfersInfo = state.transfersInfo,
        onClick = onOpenTransferSection,
        modifier = modifier,
    )
}

/**
 * Sets a transfers widget as the content of this ComposeView.
 *
 */
fun ComposeView.setTransfersWidgetContent(
    onOpenTransferSection: () -> Unit,
) {
    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
    setContent {
        OriginalTheme(isDark = isSystemInDarkTheme()) {
            TransfersWidget(
                onOpenTransferSection = onOpenTransferSection,
                modifier = Modifier.padding(bottom = 16.dp, end = 16.dp, start = 16.dp),
            )
        }
    }
}