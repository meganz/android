package mega.privacy.android.domain.repository.files

import mega.privacy.android.domain.entity.pdf.LastPageViewedInPdf
import mega.privacy.android.domain.entity.uri.UriPath

/**
 * Pdf repository
 *
 * @constructor Create empty Pdf repository
 */
interface PdfRepository {

    /**
     * Create PDF thumbnail.
     *
     * @param nodeHandle Node handle of the file already in the Cloud.
     * @param localFile Local file.
     * @return Path of the thumbnail file if created successfully, null otherwise.
     */
    suspend fun createThumbnail(nodeHandle: Long, uriPath: UriPath): String?

    /**
     * Create PDF preview.
     *
     * @param nodeHandle Node handle of the file already in the Cloud.
     * @param localFile Local file.
     * @return Path of the thumbnail file if created successfully, null otherwise.
     */
    suspend fun createPreview(nodeHandle: Long, uriPath: UriPath): String?

    /**
     * Get the last page viewed in PDF.
     */
    suspend fun getLastPageViewedInPdf(nodeHandle: Long): Long?

    /**
     * Set or update the last page viewed in PDF.
     */
    suspend fun setOrUpdateLastPageViewedInPdf(lastPageViewedInPdf: LastPageViewedInPdf)

    /**
     * Delete the last page viewed in PDF for a specific node handle.
     */
    suspend fun deleteLastPageViewedInPdf(nodeHandle: Long)

    /**
     * Delete all last page viewed in PDF records.
     */
    suspend fun deleteAllLastPageViewedInPdf()
}