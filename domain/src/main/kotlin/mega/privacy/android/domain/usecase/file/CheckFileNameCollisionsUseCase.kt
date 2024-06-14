package mega.privacy.android.domain.usecase.file

import mega.privacy.android.domain.entity.document.DocumentEntity
import mega.privacy.android.domain.entity.node.FileNameCollision
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.exception.node.NodeDoesNotExistsException
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.node.GetChildNodeUseCase
import mega.privacy.android.domain.usecase.node.GetNodeByHandleUseCase
import javax.inject.Inject

/**
 * Check files name collision use case
 *
 */
class CheckFileNameCollisionsUseCase @Inject constructor(
    private val getNodeByHandleUseCase: GetNodeByHandleUseCase,
    private val getRootNodeUseCase: GetRootNodeUseCase,
    private val getChildNodeUseCase: GetChildNodeUseCase,
    private val nodeRepository: NodeRepository,
) {
    /**
     * Invoke
     *
     * @param files
     * @param parentNodeId
     */
    suspend operator fun invoke(
        files: List<DocumentEntity>,
        parentNodeId: NodeId,
    ): List<FileNameCollision> {
        val parentNode = getParentOrRootNode(parentNodeId.longValue)
            ?: throw NodeDoesNotExistsException()
        return files.mapNotNull { entity ->
            getChildNodeUseCase(
                parentNode.id,
                entity.name
            )?.let {
                FileNameCollision(
                    collisionHandle = it.id.longValue,
                    name = entity.name,
                    isFile = !entity.isFolder,
                    size = entity.size,
                    lastModified = entity.lastModified,
                    childFileCount = entity.numFiles,
                    childFolderCount = entity.numFolders,
                    parentHandle = parentNodeId.longValue,
                    path = entity.uri
                )
            }
        }
    }

    private suspend fun getParentOrRootNode(parentHandle: Long) =
        if (parentHandle == nodeRepository.getInvalidHandle()) {
            getRootNodeUseCase()
        } else {
            getNodeByHandleUseCase(parentHandle)
        }
}