package mega.privacy.android.domain.usecase

/**
 * The use case interface to download preview
 */
fun interface DownloadPreview {
    /**
     * Download preview
     * @param callback success true, fail false
     */
    suspend operator fun invoke(nodeId: Long, callback: (success: Boolean) -> Unit)
}