package mega.privacy.android.feature.pdfviewer.presentation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.android.core.ui.components.MegaText
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.navigation.destination.PdfViewerNavKey

/**
 * Extension function to register the PDF viewer screen in the navigation graph.
 *
 * @param onBack Callback for back navigation
 * @param onNavigateToFileInfo Callback to navigate to file info screen
 * @param onOpenNodeOptions Callback to open node options bottom sheet
 * @param onTransfer Callback to handle transfer events
 */
fun EntryProviderScope<NavKey>.pdfViewerScreen(
    onBack: () -> Unit,
    onNavigateToFileInfo: (Long) -> Unit,
    onOpenNodeOptions: (Long, NodeSourceType) -> Unit,
    onTransfer: (TransferTriggerEvent) -> Unit,
) {
    entry<PdfViewerNavKey> { navKey ->
        MegaText(
            text = "PDF Viewer for node: ${navKey.nodeHandle}",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
    }
}
