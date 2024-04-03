package mega.privacy.android.domain.usecase.foldernode

import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Returns the folder empty
 */
class IsFolderEmptyUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
) {

    /**
     * invoke
     *
     * @param node [TypedNode]
     * @return true if folder is empty false otherwise
     */
    suspend operator fun invoke(node: TypedNode): Boolean = nodeRepository.isEmptyFolder(node)
}