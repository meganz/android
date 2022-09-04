package mega.privacy.android.domain.usecase

import java.io.File

/**
 * The use case interface to get preview
 */
fun interface GetPreview {
    /**
     * get preview from local if exist, from server otherwise
     * @return File
     */
    suspend operator fun invoke(nodeId: Long): File?
}