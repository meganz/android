package mega.privacy.android.app.middlelayer.scanner

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
}