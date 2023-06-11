package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.ImageRepository
import javax.inject.Inject

/**
 * The use case interface to download preview
 */
class DownloadPublicNodePreview @Inject constructor(
    private val imageRepository: ImageRepository,
) {
    /**
     * Download preview
     */
    suspend operator fun invoke(nodeId: Long) =
        imageRepository.downloadPublicNodePreview(handle = nodeId)
}