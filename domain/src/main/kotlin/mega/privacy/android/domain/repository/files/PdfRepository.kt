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
     * @param thumbnail File in which the thumbnail will be created.
     * @param localPath Local path required for creating a PdfDocument.
     * @return Path of the thumbnail file if created successfully, null otherwise.
     */
    suspend fun createThumbnail(thumbnail: File, localPath: String): String?

    /**
     * Create PDF preview.
     *
     * @param preview File in which the preview will be created.
     * @param localPath Local path required for creating a PdfDocument.
     * @return Path of the thumbnail file if created successfully, null otherwise.
     */
    suspend fun createPreview(preview: File, localPath: String): String?
}