package mega.privacy.android.core.nodecomponents.upload

import com.google.mlkit.vision.documentscanner.GmsDocumentScanner
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed
import mega.privacy.android.core.nodecomponents.scanner.DocumentScanningError

/**
 * UI state for scan document functionality.
 *
 * @property gmsDocumentScanner The ML Kit Document Scanner instance, null if not prepared
 * @property documentScanningError The document scanning error, null if no error
 * @property navigateToCustomScannerEvent Event to navigate to the custom continuous scanner
 */
data class ScanDocumentUiState(
    val gmsDocumentScanner: GmsDocumentScanner? = null,
    val documentScanningError: DocumentScanningError? = null,
    val navigateToCustomScannerEvent: StateEvent = consumed,
)
