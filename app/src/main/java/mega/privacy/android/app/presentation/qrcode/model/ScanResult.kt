package mega.privacy.android.app.presentation.qrcode.model

/**
 * Result of code scanner
 */
sealed class ScanResult {
    /**
     * Result Success
     * @property rawValue
     */
    class Success(val rawValue: String?) : ScanResult()

    /**
     * Result Cancel
     */
    object Cancel : ScanResult()
}
