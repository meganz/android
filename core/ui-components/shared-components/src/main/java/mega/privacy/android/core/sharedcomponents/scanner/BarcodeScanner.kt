package mega.privacy.android.core.sharedcomponents.scanner

/**
 * Interface for Barcode (QR Code) scanning functionality
 */
interface BarcodeScanner {

    /**
     * Attempts to install the ML Kit Barcode Scanner from Google Play services. Once installed,
     * the User can start scanning Barcodes
     *
     * @return the Barcode scanning result
     */
    suspend fun scanBarcode(): BarcodeScanResult
}
