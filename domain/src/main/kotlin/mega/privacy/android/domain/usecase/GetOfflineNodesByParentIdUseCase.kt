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
     *
     * If parentId is -1 and searchQuery is set, the search in entire table is performed
     *
     * @param parentId Int
     * @param searchQuery String
     */
    suspend operator fun invoke(
        parentId: Int,
        searchQuery: String? = null,
    ) = coroutineScope {
        val semaphore = Semaphore(8)
        if (searchQuery.isNullOrBlank()) {
            nodeRepository.getOfflineNodesByParentId(parentId)
        } else {
            nodeRepository.getOfflineNodesByQuery(searchQuery, parentId)
        }.let {
            sortOfflineInfoUseCase(it)
        }.map {
            async {
                semaphore.withPermit {
                    getOfflineFileInformationUseCase(it, false)
                }
            }
        }.awaitAll()
    }
}