package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.CompleteFolderInfo
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFolderNode
import javax.inject.Inject

/**
 * Get complete folder information combining node properties and tree statistics
 *
 * This use case combines getting a node by ID and its folder tree information
 * to provide a complete picture of folder statistics including file count,
 * folder count, total size, and creation time.
 */
class GetCompleteFolderInfoUseCase @Inject constructor(
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val getFolderTreeInfo: GetFolderTreeInfo,
) {
    /**
     * Get complete folder information for a given node ID
     *
     * @param nodeId The ID of the folder node
     * @return CompleteFolderInfo containing all folder statistics, or null if node not found
     */
    suspend operator fun invoke(nodeId: NodeId): CompleteFolderInfo? {
        return runCatching {
            getNodeByIdUseCase(nodeId)
        }.getOrNull()
            ?.let { node ->
                val folder = node as? TypedFolderNode ?: return null
                val folderTreeInfo = runCatching {
                    getFolderTreeInfo(folder)
                }.getOrNull()

                CompleteFolderInfo(
                    numOfFiles = folderTreeInfo?.numberOfFiles ?: 0,
                    numOfFolders = folderTreeInfo?.numberOfFolders ?: 0,
                    totalSizeInBytes = folderTreeInfo?.totalCurrentSizeInBytes ?: 0L,
                    creationTime = folder.creationTime,
                )
            }
    }
}
