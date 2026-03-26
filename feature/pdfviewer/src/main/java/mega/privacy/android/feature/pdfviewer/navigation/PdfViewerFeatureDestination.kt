package mega.privacy.android.feature.pdfviewer.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.core.nodecomponents.sheet.options.NodeOptionsBottomSheetNavKey
import mega.privacy.android.feature.pdfviewer.presentation.pdfViewerScreen
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler

/**
 * Feature destination for the PDF Viewer module.
 *
 * Registers the PDF viewer screen and its related navigation destinations.
 * Note: The node options bottom sheet is registered separately in the app module.
 */
object PdfViewerFeatureDestination : FeatureDestination {

    override val navigationGraph: EntryProviderScope<NavKey>.(NavigationHandler, TransferHandler) -> Unit =
        { navigationHandler, transferHandler ->

            // Register the PDF viewer screen
            pdfViewerScreen(
                onBack = navigationHandler::back,
                onOpenNodeOptions = { handle, sourceType ->
                    navigationHandler.navigate(
                        NodeOptionsBottomSheetNavKey(
                            nodeHandle = handle,
                            nodeSourceType = sourceType,
                        )
                    )
                },
                navigationHandler = navigationHandler,
                onTransfer = transferHandler::setTransferEvent,
            )
        }
}
