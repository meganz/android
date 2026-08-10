package mega.privacy.android.domain.usecase.photos

import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.PhotosRepository
import javax.inject.Inject

/**
 * Resolves the [ImageNode] for [NodeId] so the viewer can load each page's node on demand.
 */
class GetTimelineImageNodeByIdUseCase @Inject constructor(
    private val photosRepository: PhotosRepository,
) {
    suspend operator fun invoke(nodeId: NodeId): ImageNode? =
        photosRepository.getImageNode(nodeId)
}
