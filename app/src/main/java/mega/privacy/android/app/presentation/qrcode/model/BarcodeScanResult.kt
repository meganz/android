package mega.privacy.android.app.presentation.qrcode.model

/**
 * Sealed class containing different Results from successfully installing the ML Kit Barcode Scanner
 * and the User attempting to scan a Barcode
 */
sealed class BarcodeScanResult {

    /**
     * The User successfully scanned a Barcode
     *
     * @property rawValue The raw Barcode value
     */
    class Success(val rawValue: String?) : BarcodeScanResult()

    /**
     * The User cancelled the Barcode scanning process
     */
    data object Cancelled : BarcodeScanResult()
}
