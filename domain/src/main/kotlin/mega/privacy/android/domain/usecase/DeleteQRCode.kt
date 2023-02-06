package mega.privacy.android.domain.usecase

/**
 * Use case to delete QR Code and file
 */
fun interface DeleteQRCode {
    /**
     * Invoke method
     *
     * @param contactLink handle – Contact link to be deleted. It is the URL starts with "https://"
     * @param fileName name of the QR code file in cache
     */
    suspend operator fun invoke(contactLink: String, fileName: String)
}