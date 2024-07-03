package mega.privacy.android.domain.usecase.offline

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Use case to sync offline local files with database entries
 */
class SyncOfflineFilesUseCase @Inject constructor(
    private val clearOfflineUseCase: ClearOfflineUseCase,
    private val removeOfflineNodesUseCase: RemoveOfflineNodesUseCase,
    private val getOfflineFilesUseCase: GetOfflineFilesUseCase,
    private val nodeRepository: NodeRepository,
    private val fileSystemRepository: FileSystemRepository,
) {
    /**
     * Invoke
     */
    suspend operator fun invoke() {
        val offlineNodes = nodeRepository.getAllOfflineNodes()
        if (fileSystemRepository.getOfflineFolder().exists()) {
            // Delete offline info from database if file doesn't exist
            getOfflineFilesUseCase(offlineNodes)
                .filter { !it.value.exists() }
                .map { it.key }
                .let { nodeRepository.removeOfflineNodeByIds(it) }

            // Delete empty folders
            offlineNodes
                .filter { it.isFolder && nodeRepository.getOfflineNodesByParentId(it.id).isEmpty() }
                .map { NodeId(it.handle.toLong()) }
                .let { removeOfflineNodesUseCase(it) }
        } else if (offlineNodes.isNotEmpty()) {
            clearOfflineUseCase()
        }
    }
}