package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Use case to get Offline nodes
 */
class GetOfflineNodesByParentIdUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
    private val sortOfflineInfoUseCase: SortOfflineInfoUseCase,
) {
    /**
     * invoke
     * @param parentId Int
     */
    suspend operator fun invoke(parentId: Int) =
        nodeRepository.getOfflineNodeByParentId(parentId = parentId)?.let {
            sortOfflineInfoUseCase(it)
        }
}