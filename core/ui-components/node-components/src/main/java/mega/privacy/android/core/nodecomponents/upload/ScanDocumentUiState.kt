package mega.privacy.android.core.nodecomponents.upload

import com.google.mlkit.vision.documentscanner.GmsDocumentScanner
import mega.privacy.android.core.nodecomponents.scanner.DocumentScanningError

/**
 * UI state for scan document functionality.
 *
 * @property gmsDocumentScanner The ML Kit Document Scanner instance, null if not prepared
 * @property documentScanningError The document scanning error, null if no error
 */
data class ScanDocumentUiState(
    val gmsDocumentScanner: GmsDocumentScanner? = null,
    val documentScanningError: DocumentScanningError? = null,
)

