package mega.privacy.android.app.domain.usecase

import java.io.File

/**
 * The use case interface to get thumbnail
 */
fun interface GetThumbnail {
    /**
     * get thumbnail from local if exist, from server otherwise
     * @return File
     */
    suspend operator fun invoke(nodeId: Long): File?
}