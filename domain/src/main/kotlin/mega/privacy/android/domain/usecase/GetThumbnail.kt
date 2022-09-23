package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.exception.MegaException
import java.io.File

/**
 * The use case interface to get thumbnail
 */
fun interface GetThumbnail {
    /**
     * get thumbnail from local if exist, from server otherwise
     * @return File
     */
    @Throws(MegaException::class)
    suspend operator fun invoke(nodeId: Long): File?
}