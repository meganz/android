package mega.privacy.android.app.presentation.documentscanner.model

import com.google.mlkit.vision.documentscanner.GmsDocumentScanner

/**
 * Sealed class representing different states of handling the Document Scanner
 */
sealed class HandleScanDocumentResult {

    /**
     * The legacy Document Scanner should be used
     */
    data object UseLegacyImplementation : HandleScanDocumentResult()

    /**
     * The new ML Kit Document Scanner should be used
     *
     * @property documentScanner The ML Kit Document Scanner for the caller's use
     */
    data class UseNewImplementation(val documentScanner: GmsDocumentScanner) :
        HandleScanDocumentResult()
}