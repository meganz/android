package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.AlbumRepository
import javax.inject.Inject

/**
 * The use case for get album photo local link by http server
 */
class GetAlbumPhotoFileUrlByNodeIdUseCase @Inject constructor(
    private val albumRepository: AlbumRepository,
) {

    /**
     * Get album photo file url by node id
     *
     * @param nodeId nodeId
     * @return local link
     */
    suspend operator fun invoke(nodeId: NodeId) =
        albumRepository.getAlbumPhotoFileUrlByNodeHandle(nodeId)
}