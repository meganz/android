package mega.privacy.android.domain.usecase

/**
 * Use case that deletes old deprecated QR code file.
 */
fun interface CleanOldQRCodeFile {
    /**
     * invoke
     */
    suspend operator fun invoke()
}