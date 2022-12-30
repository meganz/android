package mega.privacy.android.domain.usecase

/**
 * Use case to delete QR Code and file
 */
fun interface DeleteQRCode {
    /**
     * Invoke method
     *
     * @param handle handle – Handle of the contact link to delete If the
     *  parameter is INVALID_HANDLE, the active contact link is deleted
     * @param fileName name of the QR code file in cache
     */
    suspend operator fun invoke(handle: Long, fileName: String)
}