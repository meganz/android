package mega.privacy.android.domain.usecase

/**
 * The use case interface to download thumbnail
 */
fun interface DownloadThumbnail {
    /**
     * Download thumbnail
     * @param callback success true, fail false
     */
    suspend operator fun invoke(nodeId: Long, callback: (success: Boolean) -> Unit)
}