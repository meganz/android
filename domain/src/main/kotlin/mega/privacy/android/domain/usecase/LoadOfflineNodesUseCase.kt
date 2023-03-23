package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.offline.OfflineNodeInformation
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Use case for loading offline nodes
 *
 */
class LoadOfflineNodesUseCase @Inject constructor(private val nodeRepository: NodeRepository) {

    /**
     * Invoke
     *
     * @param path Node path
     * @param searchQuery search query for database
     */
    suspend operator fun invoke(
        path: String,
        searchQuery: String?,
    ): List<OfflineNodeInformation> = nodeRepository.loadOfflineNodes(path, searchQuery)
}