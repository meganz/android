package mega.privacy.android.domain.entity.zipbrowser

/**
 * Progress of extracting a single entry out of an archive.
 */
sealed interface ZipExtractionState {

    data class InProgress(val bytesExtracted: Long, val totalBytes: Long) : ZipExtractionState {
        val fraction: Float
            get() = if (totalBytes <= 0L) 0f else (bytesExtracted.toFloat() / totalBytes)
    }

    data class Completed(val absolutePath: String) : ZipExtractionState

    data class Failed(val cause: ZipArchiveException) : ZipExtractionState
}
