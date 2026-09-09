package mega.privacy.android.feature.contact.qrcode.mycode.model

/**
 * One-shot user-facing message emitted by [mega.privacy.android.feature.contact.qrcode.mycode.MyQrCodeViewModel].
 * The UI layer resolves each case to the appropriate localized string.
 */
sealed interface MyQrCodeMessage {

    /**
     * The contact link has been copied to the clipboard.
     */
    data object ContactLinkCopied : MyQrCodeMessage

    /**
     * The QR code could not be saved to the device.
     */
    data object SaveError : MyQrCodeMessage

    /**
     * There is not enough free space to save the QR code.
     */
    data object NotEnoughSpace : MyQrCodeMessage

    /**
     * The QR code was saved to the device.
     *
     * @property destinationPath the human-readable destination the file was saved to
     */
    data class SaveSuccess(val destinationPath: String) : MyQrCodeMessage

    /**
     * A generic failure occurred while creating or saving the QR code.
     */
    data object GeneralError : MyQrCodeMessage

    /**
     * The QR code was successfully reset and a new contact link generated.
     */
    data object ResetDone : MyQrCodeMessage

    /**
     * A QR code reset or delete operation failed.
     *
     * @property error the reason the operation failed
     */
    data class OperationError(val error: MyQrCodeError) : MyQrCodeMessage
}
