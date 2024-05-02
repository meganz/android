package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.offline.OfflineNodeInformation
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Get sorted offline nodes by parent search query
 */
class GetSortedOfflineNodeInformationByQuery @Inject constructor(
    private val nodeRepository: NodeRepository,
) {
    /**
     * invoke
     * @param query [String]
     */
    suspend operator fun invoke(query: String): List<OfflineNodeInformation>? {
        return nodeRepository.getOfflineByQuery(query)
    }
}