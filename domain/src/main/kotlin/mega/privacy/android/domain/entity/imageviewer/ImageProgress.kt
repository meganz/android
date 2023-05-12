package mega.privacy.android.domain.entity.imageviewer

/**
 * Image Progress
 */
sealed interface ImageProgress {
    /**
     * Started
     * @param transferTag
     */
    data class Started(val transferTag: Int) : ImageProgress

    /**
     * InProgress
     *  @param totalBytes
     *  @param transferredBytes
     */
    data class InProgress(val totalBytes: Long?, val transferredBytes: Long?) : ImageProgress

    /**
     * Completed
     * @param path
     */
    data class Completed(val path: String) : ImageProgress
}
