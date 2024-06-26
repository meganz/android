package mega.privacy.android.domain.usecase.offline

import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Get OfflineNodeInformation by database row id
 *
 */
class GetOfflineNodeInformationByIdUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
) {
    /**
     * Invoke
     * @param id [Int] of database row
     */
    suspend operator fun invoke(id: Int) = nodeRepository.getOfflineNodeById(id)
}