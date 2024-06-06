package mega.privacy.android.domain.usecase

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.offline.GetOfflineFileInformationUseCase
import javax.inject.Inject

/**
 * Use case to get Offline nodes
 */
class GetOfflineNodesByParentIdUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
    private val getOfflineFileInformationUseCase: GetOfflineFileInformationUseCase,
    private val sortOfflineInfoUseCase: SortOfflineInfoUseCase,
) {
    /**
     * Invoke
     * @param parentId Int
     */
    suspend operator fun invoke(parentId: Int) = coroutineScope {
        val semaphore = Semaphore(8)
        nodeRepository.getOfflineNodeByParentId(parentId = parentId)
            ?.let {
                sortOfflineInfoUseCase(it)
            }?.map {
                async {
                    semaphore.withPermit {
                        getOfflineFileInformationUseCase(it, false)
                    }
                }
            }?.awaitAll() ?: emptyList()
    }
}