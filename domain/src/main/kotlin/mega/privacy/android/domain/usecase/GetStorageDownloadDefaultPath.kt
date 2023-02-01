package mega.privacy.android.domain.usecase

/**
 * Pass through use case to get storage default download location path
 */
fun interface GetStorageDownloadDefaultPath {
    /**
     * Invoke
     * @return Default Download Location Path as [String]
     */
    suspend operator fun invoke(): String
}