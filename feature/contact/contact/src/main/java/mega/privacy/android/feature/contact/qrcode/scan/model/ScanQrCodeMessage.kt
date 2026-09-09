package mega.privacy.android.feature.contact.qrcode.scan.model

/**
 * One-shot user-facing message emitted by [mega.privacy.android.feature.contact.qrcode.scan.ScanQrCodeViewModel].
 * The UI layer resolves each case to the appropriate localized string.
 */
enum class ScanQrCodeMessage {
    /**
     * The scanned code is not a valid MEGA contact link.
     */
    InvalidCode,

    /**
     * A generic error occurred while scanning.
     */
    GeneralError,
}
