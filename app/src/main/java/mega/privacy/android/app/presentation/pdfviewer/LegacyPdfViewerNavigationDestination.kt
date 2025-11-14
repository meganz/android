package mega.privacy.android.app.presentation.pdfviewer

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.core.nodecomponents.mapper.NodeContentUriIntentMapper
import mega.privacy.android.navigation.contract.transparent.transparentMetadata
import mega.privacy.android.navigation.destination.LegacyPdfViewerNavKey

fun EntryProviderScope<NavKey>.legacyPdfViewerScreen(
    removeDestination: () -> Unit,
    nodeContentUriIntentMapper: NodeContentUriIntentMapper,
) {
    entry<LegacyPdfViewerNavKey>(
        metadata = transparentMetadata()
    ) { key ->
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            val intent = PdfViewerActivity.createIntent(
                context = context,
                nodeHandle = key.nodeHandle,
                nodeSourceType = key.nodeSourceType,
            )
            nodeContentUriIntentMapper(
                intent = intent,
                content = key.nodeContentUri,
                mimeType = key.mimeType,
            )
            context.startActivity(intent)

            // Immediately pop this destination from the back stack
            removeDestination()
        }
    }
}