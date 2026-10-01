package mega.privacy.android.domain.usecase.node.sort

import mega.privacy.android.domain.repository.SortOrderRepository
import javax.inject.Inject

/**
 * Monitor the others sort order (incoming shares / other browsing surfaces).
 */
class MonitorOthersSortOrderUseCase @Inject constructor(
    private val sortOrderRepository: SortOrderRepository,
) {
    operator fun invoke() = sortOrderRepository.monitorOthersSortOrder()
}
