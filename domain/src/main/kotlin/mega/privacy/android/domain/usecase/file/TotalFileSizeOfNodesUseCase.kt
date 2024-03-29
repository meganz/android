package mega.privacy.android.domain.usecase.file

import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.usecase.GetFolderTreeInfo
import javax.inject.Inject

/**
 * Get the total size of the files represented with the provided list of nodes
 */
class TotalFileSizeOfNodesUseCase @Inject constructor(
    private val getFolderTreeInfo: GetFolderTreeInfo,
) {
    /**
     * Invoke
     * @param nodes the list of nodes that will be checked
     * @return total size of the files represented with the provided list of nodes
     */
    suspend operator fun invoke(nodes: List<TypedNode>) = nodes.sumOf {
        when (it) {
            is FileNode -> it.size
            is TypedFolderNode -> getFolderTreeInfo(it).totalCurrentSizeInBytes
            else -> 0L
        }
    }
}