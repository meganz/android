package mega.privacy.android.app.domain.usecase

/**
 * Get the fingerprint by file path
 */
fun interface GetFingerprint {
    /**
     * Get the fingerprint by file path
     *
     * @param filePath
     * @return fingerprint string or null
     */
    suspend operator fun invoke(filePath: String): String?
}
