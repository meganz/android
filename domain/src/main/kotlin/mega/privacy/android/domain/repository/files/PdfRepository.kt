package mega.privacy.android.domain.repository.files

import java.io.File

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
    suspend fun createThumbnail(nodeHandle: Long, localFile: File): String?

    /**
     * Create PDF preview.
     *
     * @param nodeHandle Node handle of the file already in the Cloud.
     * @param localFile Local file.
     * @return Path of the thumbnail file if created successfully, null otherwise.
     */
    suspend fun createPreview(nodeHandle: Long, localFile: File): String?
}