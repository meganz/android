package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.PhotosRepository
import javax.inject.Inject

/**
 * Get image node by id use case
 */
class GetImageNodeByIdUseCase @Inject constructor(
    private val photosRepository: PhotosRepository,
) {

    /**
     * Get image node by id
     *
     * @param id [NodeId] of the node
     * @return the [ImageNode] with the given [NodeId] or null if the node was not found
     */
    suspend operator fun invoke(id: NodeId): ImageNode? {
        return photosRepository.getImageNodeFromCache(id) ?: photosRepository.fetchImageNode(
            nodeId = id,
            includeRubbishBin = true,
        )
    }
}