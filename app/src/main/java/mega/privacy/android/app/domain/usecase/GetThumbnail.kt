package mega.privacy.android.app.domain.usecase

import java.io.File

/**
 * The use case interface to get thumbnail
 */
interface GetThumbnail {
    /**
     * get thumbnail from server
     * @return File
     */
    suspend operator fun invoke(nodeId: Long): File
}