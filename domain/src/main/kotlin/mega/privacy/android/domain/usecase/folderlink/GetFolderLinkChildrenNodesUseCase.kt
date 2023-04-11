package mega.privacy.android.domain.usecase.folderlink

import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.exception.FetchFolderNodesException
import mega.privacy.android.domain.repository.FolderLinkRepository
import mega.privacy.android.domain.usecase.AddNodeType
import javax.inject.Inject

/**
 * Get children nodes of a parent node
 */
class GetFolderLinkChildrenNodesUseCase @Inject constructor(
    private val folderLinkRepository: FolderLinkRepository,
    private val addNodeType: AddNodeType,
) {
    /**
     * Get children nodes of a parent node
     *
     * @param parentHandle  Parent handle
     * @param order         Order for the returned list
     * @return Children nodes of the parent node
     */
    suspend operator fun invoke(parentHandle: Long, order: Int?): List<TypedNode> =
        runCatching { folderLinkRepository.getNodeChildren(parentHandle, order) }
            .getOrElse { throw FetchFolderNodesException.GenericError() }
            .map { addNodeType(it) }
}