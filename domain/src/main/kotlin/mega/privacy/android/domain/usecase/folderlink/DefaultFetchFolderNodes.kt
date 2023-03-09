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
class DefaultFetchFolderNodes @Inject constructor(
    private val repository: FolderLinkRepository,
    private val addNodeType: AddNodeType
) : FetchFolderNodes {

    override suspend fun invoke(): FetchFolderNodesResult {
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

                                runCatching { repository.getNodeChildren(typedFolderNode.id.longValue) }
                                    .onSuccess { untypedNodeList ->
                                        folderNodesResult.childrenNodes =
                                            untypedNodeList.map { addNodeType(it) }
                                    }
                                    .onFailure { throw FetchFolderNodesException.GenericError() }
                            }
                            .onFailure { throw FetchFolderNodesException.GenericError() }
                    }
                } else {
                    throw FetchFolderNodesException.GenericError()
                }
            }.onFailure { throw it }

        return folderNodesResult
    }
}