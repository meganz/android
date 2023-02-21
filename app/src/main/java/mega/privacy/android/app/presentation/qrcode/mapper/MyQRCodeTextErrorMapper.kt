package mega.privacy.android.app.presentation.qrcode.mapper

/**
 * Map My QRCode exception to text
 */
fun interface MyQRCodeTextErrorMapper {
    /**
     * Invoke
     * @param error
     * @return the string to display
     */
    operator fun invoke(error: Throwable): String
}