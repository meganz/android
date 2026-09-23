package mega.privacy.android.app.presentation.pdfviewer

import androidx.compose.runtime.LaunchedEffect
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.core.nodecomponents.mapper.ViewTypeToNodeSourceTypeMapper
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.navOptions
import mega.privacy.android.navigation.contract.transparent.transparentMetadata
import mega.privacy.android.navigation.destination.LegacyPdfViewerNavKey
import mega.privacy.android.navigation.destination.PdfViewerNavKey

/**
 * Bridges callers that still emit a [LegacyPdfViewerNavKey] onto the Compose [PdfViewerNavKey]
 * destination. The legacy key is replaced on the back stack so pressing back leaves the viewer
 * instead of landing on this empty bridge.
 */
fun EntryProviderScope<NavKey>.legacyPdfViewerScreen(
    navigationHandler: NavigationHandler,
    viewTypeToNodeSourceTypeMapper: ViewTypeToNodeSourceTypeMapper,
) {
    entry<LegacyPdfViewerNavKey>(
        metadata = transparentMetadata()
    ) { key ->
        LaunchedEffect(key) {
            navigationHandler.navigate(
                destination = key.nodeContentUri.toPdfViewerNavKey(
                    nodeHandle = key.nodeHandle,
                    mimeType = key.mimeType,
                    nodeSourceType = legacyPdfNodeSourceType(
                        viewType = key.nodeSourceType,
                        viewTypeToNodeSourceTypeMapper = viewTypeToNodeSourceTypeMapper,
                    ),
                ),
                navOptions = navOptions {
                    popUpTo<LegacyPdfViewerNavKey> { inclusive = true }
                },
            )
        }
    }
}
