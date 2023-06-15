package mega.privacy.android.domain.usecase.photos

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.AlbumRepository
import javax.inject.Inject

/**
 * Save album to folder use case
 */
class SaveAlbumToFolderUseCase @Inject constructor(
    private val albumRepository: AlbumRepository,
) {
    /**
     * @param folderName name of new folder to be created
     * @param photoIds to be added into target folder
     * @param targetParentFolderNodeId target parent folder where [photoIds] to be copied into
     *
     * @return list of copied node ids
     */
    suspend operator fun invoke(
        folderName: String,
        photoIds: List<NodeId>,
        targetParentFolderNodeId: NodeId,
    ): List<NodeId> = albumRepository.saveAlbumToFolder(
        folderName,
        photoIds,
        targetParentFolderNodeId,
    )
}
