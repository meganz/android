package mega.privacy.android.domain.entity.transfer

/**
 * Transfer data class containing information about transfer queues.
 *
 * @property numDownloads Number of downloads.
 * @property numUploads Number of uploads.
 * @property downloadTags List of download tags.
 * @property uploadTags List of upload tags.
 */
data class TransferData(
    val numDownloads: Int,
    val numUploads: Int,
    val downloadTags: List<Int>,
    val uploadTags: List<Int>,
)
