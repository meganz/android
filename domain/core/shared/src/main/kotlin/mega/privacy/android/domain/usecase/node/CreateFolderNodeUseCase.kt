package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Create a folder node
 */
class CreateFolderNodeUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
) {

    /**
     * invoke
     * @param name Name of the folder to create
     * @param parentNodeId Parent node id under which the folder should be created
     *                   If null, the folder will be created in the root folder
     * @return handle [Long]
     */
    suspend operator fun invoke(name: String, parentNodeId: NodeId? = null): NodeId =
        nodeRepository.createFolder(name, parentNodeId)
}
