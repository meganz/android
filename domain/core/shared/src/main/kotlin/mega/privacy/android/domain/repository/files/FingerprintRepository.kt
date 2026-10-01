package mega.privacy.android.domain.repository.files

/**
 * Repository for fingerprint related methods
 */
interface FingerprintRepository {
    /**
     * Get the fingerprint of a file by path
     *
     * @param filePath file path
     * @return fingerprint
     */
    suspend fun getFingerprint(filePath: String): String?
}