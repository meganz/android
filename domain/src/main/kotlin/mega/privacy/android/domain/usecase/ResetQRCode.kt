package mega.privacy.android.domain.usecase

/**
 * Use case to reset QR Code
 */
fun interface ResetQRCode {

    /**
     * invoke method
     *
     * @return new QR code
     */
    suspend operator fun invoke(): String
}