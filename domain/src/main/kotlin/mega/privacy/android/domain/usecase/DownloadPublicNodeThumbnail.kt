package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.ImageRepository
import javax.inject.Inject

/**
 * The use case interface to download thumbnail
 */
class DownloadPublicNodeThumbnail @Inject constructor(
    private val imageRepository: ImageRepository,
) {
    /**
     * Download thumbnail
     */
    suspend operator fun invoke(nodeId: Long) =
        imageRepository.downloadPublicNodeThumbnail(handle = nodeId)
}