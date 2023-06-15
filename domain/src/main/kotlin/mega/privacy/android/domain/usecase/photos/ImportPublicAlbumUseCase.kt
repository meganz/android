package mega.privacy.android.domain.usecase.photos

import mega.privacy.android.domain.entity.node.NodeId
import javax.inject.Inject

/**
 * Import album use case
 */
class ImportPublicAlbumUseCase @Inject constructor(
    private val saveAlbumToFolderUseCase: SaveAlbumToFolderUseCase,
    private val createAlbumUseCase: CreateAlbumUseCase,
    private val addPhotosToAlbumUseCase: AddPhotosToAlbumUseCase,
) {
    /**
     * @param albumName name of new album to be created
     * @param photoIds to be added into new album
     * @param targetParentFolderNodeId target parent folder where [photoIds] to be copied into
     */
    suspend operator fun invoke(
        albumName: String,
        photoIds: List<NodeId>,
        targetParentFolderNodeId: NodeId,
    ) {
        val albumId = createAlbumUseCase(albumName).id
        val nodeIds = saveAlbumToFolderUseCase(albumName, photoIds, targetParentFolderNodeId)

        addPhotosToAlbumUseCase(albumId, nodeIds)
    }
}
