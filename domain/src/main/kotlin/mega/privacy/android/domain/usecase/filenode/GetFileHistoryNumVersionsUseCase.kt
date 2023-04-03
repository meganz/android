package mega.privacy.android.domain.usecase.filenode

import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Gets the number of versions the file has, excluding the current one
 */
class GetFileHistoryNumVersionsUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
) {
    /**
     * Gets the number of versions the file represented by nodeHandle has, excluding the current version
     * @param fileNode the node to check
     * @return the number of history versions or 0 if the file is not found or has no versions
     */
    suspend operator fun invoke(fileNode: FileNode): Int = if (fileNode.hasVersion) {
        (nodeRepository.getNumVersions(fileNode.id.longValue) - 1).coerceAtLeast(0)
    } else {
        0
    }
}