package mega.privacy.android.domain.usecase

/**
 * Pass through use case to get storage download location path
 */
fun interface GetStorageDownloadLocation {
    /**
     * Invoke
     * @return Download Location Path as [String]
     */
    suspend operator fun invoke(): String?
}