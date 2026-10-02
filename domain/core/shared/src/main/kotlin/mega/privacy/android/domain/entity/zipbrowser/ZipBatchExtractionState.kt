package mega.privacy.android.domain.entity.zipbrowser

/**
 * Progress of extracting several entries at once.
 */
sealed interface ZipBatchExtractionState {

    /**
     * @property completed how many entries have been dealt with, whether they succeeded or not
     */
    data class InProgress(
        val completed: Int,
        val total: Int,
        val currentEntryPath: String,
    ) : ZipBatchExtractionState

    /**
     * The batch ran to the end. A batch is reported as finished even when some entries failed, so
     * the caller can tell the user which ones rather than losing the ones that worked.
     *
     * @property extractedPaths the local paths of the entries that came out
     * @property failures the entries that did not, by entry path
     */
    data class Finished(
        val extractedPaths: List<String>,
        val failures: Map<String, ZipArchiveException>,
    ) : ZipBatchExtractionState {
        val hasFailures: Boolean get() = failures.isNotEmpty()
    }

    /**
     * The batch could not start at all, for instance because the device has no room for it.
     */
    data class Failed(val cause: ZipArchiveException) : ZipBatchExtractionState
}
