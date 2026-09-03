package mega.privacy.android.core.nodecomponents.scanner

import com.google.mlkit.vision.documentscanner.GmsDocumentScanner

/**
 * Interface for Document Scanning related functionalities
 */
interface DocumentScannerHandler {

    /**
     * Install the ML Kit Document Scanner from Google Play services
     *
     * @return the ML Kit Document Scanner if installation is successful
     */
    suspend fun prepareDocumentScanner(): GmsDocumentScanner
}
