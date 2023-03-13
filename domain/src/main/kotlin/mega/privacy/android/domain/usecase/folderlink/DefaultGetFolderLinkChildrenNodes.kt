package mega.privacy.android.domain.usecase.folderlink

import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.exception.FetchFolderNodesException
import mega.privacy.android.domain.repository.FolderLinkRepository
import mega.privacy.android.domain.usecase.AddNodeType
import javax.inject.Inject

/**
 * Use case implementation for GetChildrenNodes
 */
class DefaultGetFolderLinkChildrenNodes @Inject constructor(
    private val repository: FolderLinkRepository,
    private val addNodeType: AddNodeType,
) : GetFolderLinkChildrenNodes {
    override suspend fun invoke(parentHandle: Long, order: Int?): List<TypedNode> =
        runCatching { repository.getNodeChildren(parentHandle, order) }
            .getOrElse { throw FetchFolderNodesException.GenericError() }
            .map { addNodeType(it) }
}