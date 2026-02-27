package mega.privacy.android.feature.pdfviewer.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler

/**
 * Feature destination for the PDF Viewer module.
 *
 * Registers the PDF viewer screen and its related navigation destinations.
 */
class PdfViewerFeatureDestination : FeatureDestination {

    override val navigationGraph: EntryProviderScope<NavKey>.(NavigationHandler, TransferHandler) -> Unit =
        { navigationHandler, transferHandler ->

            // Register the PDF viewer screen
        }
}
