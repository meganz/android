package mega.privacy.android.domain.usecase.folderlink

import mega.privacy.android.domain.entity.folderlink.FetchFolderNodesResult
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.exception.FetchFolderNodesException
import mega.privacy.android.domain.repository.FolderLinkRepository
import mega.privacy.android.domain.usecase.AddNodeType
import javax.inject.Inject

/**
 * Use case implementation for fetching folder nodes
 */
class FetchFolderNodesUseCase @Inject constructor(
    private val repository: FolderLinkRepository,
    private val addNodeType: AddNodeType,
    private val getChildrenNodes: GetFolderLinkChildrenNodes,
) {

    /**
     * Invoke
     *
     * @param folderSubHandle   Base 64 handle of the folder node
     * @return Folder nodes result
     */
    suspend operator fun invoke(folderSubHandle: String?): FetchFolderNodesResult {
        val folderNodesResult = FetchFolderNodesResult()
        runCatching { repository.fetchNodes() }
            .onSuccess { result ->
                repository.updateLastPublicHandle(result.nodeHandle)
                val rootNode = repository.getRootNode()
                if (rootNode != null) {
                    if (result.flag) {
                        throw FetchFolderNodesException.InvalidDecryptionKey()
                    } else {
                        runCatching { addNodeType(rootNode) as TypedFolderNode }
                            .onSuccess { typedFolderNode ->
                                folderNodesResult.rootNode = typedFolderNode
                                var parentHandle = typedFolderNode.id.longValue

                                if (folderSubHandle != null) {
                                    runCatching { repository.getFolderLinkNode(folderSubHandle) }
                                        .onSuccess {
                                            runCatching { addNodeType(it) as TypedFolderNode }
                                                .onSuccess {
                                                    folderNodesResult.parentNode = it
                                                    parentHandle = it.id.longValue
                                                }
                                        }
                                        .onFailure { throw FetchFolderNodesException.GenericError() }
                                }

                                runCatching { getChildrenNodes(parentHandle, null) }
                                    .onSuccess { folderNodesResult.childrenNodes = it }
                                    .onFailure { throw FetchFolderNodesException.GenericError() }
                            }
                            .onFailure { throw FetchFolderNodesException.GenericError() }
                    }
                } else {
                    throw FetchFolderNodesException.GenericError()
                }
            }.onFailure { throw FetchFolderNodesException.GenericError() }

        return folderNodesResult
    }
}