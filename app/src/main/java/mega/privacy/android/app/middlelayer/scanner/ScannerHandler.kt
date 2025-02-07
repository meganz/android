package mega.privacy.android.app.middlelayer.scanner

import com.google.mlkit.vision.documentscanner.GmsDocumentScanner
import mega.privacy.android.app.presentation.qrcode.model.BarcodeScanResult

/**
 * Interface for all Scanning related functionalities
 */
interface ScannerHandler {

    /**
     * Attempts to install the ML Kit Barcode Scanner from Google Play services. Once installed,
     * the User can start scanning Barcodes
     *
     * @return the Barcode scanning result
     */
    suspend fun scanBarcode(): BarcodeScanResult

    /**
     * Evaluates the appropriate type of Document Scanner to use based on the feature flag
     *
     * @return the result dictating what type of Document Scanner to use
     */

    /**
     * Install the ML Kit Document Scanner from Google Play services
     *
     * @return the ML Kit Document Scanner if installation is successful
     */
    suspend fun prepareDocumentScanner(): GmsDocumentScanner
}