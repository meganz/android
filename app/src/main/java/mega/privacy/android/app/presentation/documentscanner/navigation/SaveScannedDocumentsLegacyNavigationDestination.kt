package mega.privacy.android.app.presentation.documentscanner.navigation

import android.net.Uri
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.presentation.documentscanner.SaveScannedDocumentsActivity
import mega.privacy.android.navigation.contract.transparent.transparentMetadata
import mega.privacy.android.navigation.destination.SaveScannedDocumentsNavKey

/**
 * Navigation destination for SaveScannedDocumentsActivity that handles legacy navigation.
 *
 * Note: This navigates to the Activity only if no compose implementation exists.
 * If a compose implementation is available, it should be used instead.
 */
fun EntryProviderScope<NavKey>.saveScannedDocumentsLegacyDestination(
    removeDestination: () -> Unit,
) {
    entry<SaveScannedDocumentsNavKey>(
        metadata = transparentMetadata()
    ) { key ->
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            key.scanPdfUri.takeIf { it.isNotBlank() }?.let(Uri::parse)?.let { pdfUri ->
                val soloImageUri = key.scanSoloImageUri?.takeIf { it.isNotBlank() }?.let(Uri::parse)

                val intent = SaveScannedDocumentsActivity.getIntent(
                    context = context,
                    fromChat = key.originatedFromChat,
                    parentHandle = key.cloudDriveParentHandle,
                    pdfUri = pdfUri,
                    imageUris = soloImageUri?.let { listOf(it) } ?: emptyList(),
                )
                context.startActivity(intent)
            }

            // Immediately pop this destination from the back stack
            removeDestination()
        }
    }
}


