package mega.privacy.android.domain.usecase

/**
 * Pass through use case to set storage download location path
 */
fun interface SetStorageDownloadLocation {
    /**
     * Invoke
     * @param location as folder path [String]
     */
    suspend operator fun invoke(location: String)
}