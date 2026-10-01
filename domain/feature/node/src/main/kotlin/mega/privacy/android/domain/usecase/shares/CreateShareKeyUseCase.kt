package mega.privacy.android.domain.usecase.shares

import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Creates a new share key for the folder if there is no share key already created.
 */
class CreateShareKeyUseCase @Inject constructor(private val nodeRepository: NodeRepository) {
    /**
     * Creates a new share key for the folder if there is no share key already created.
     * @param node : [FolderNode]
     */
    suspend operator fun invoke(node: FolderNode) {
        nodeRepository.createShareKey(node)
    }
}