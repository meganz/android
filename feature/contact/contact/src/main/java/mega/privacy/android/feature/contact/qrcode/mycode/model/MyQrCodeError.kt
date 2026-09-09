package mega.privacy.android.feature.contact.qrcode.mycode.model

/**
 * Reason a "My QR code" operation failed, resolved to a user-facing message by the UI layer.
 */
enum class MyQrCodeError {
    /**
     * Resetting the QR code failed.
     */
    ResetFailed,

    /**
     * Deleting the QR code failed.
     */
    DeleteFailed,

    /**
     * A generic failure not covered by a more specific reason.
     */
    General,
}
