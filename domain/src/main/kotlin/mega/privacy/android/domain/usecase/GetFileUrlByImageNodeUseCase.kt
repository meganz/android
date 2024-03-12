package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.repository.PhotosRepository
import javax.inject.Inject

/**
 * The use case for get local link by http server
 */
class GetFileUrlByImageNodeUseCase @Inject constructor(
    private val photosRepository: PhotosRepository,
) {

    /**
     * Get file url by imageNode
     *
     * @param imageNode
     * @return local link
     */
    suspend operator fun invoke(imageNode: ImageNode) =
        photosRepository.getHttpServerLocalLink(imageNode = imageNode)
}