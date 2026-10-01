package mega.privacy.android.domain.usecase.node.sort

import mega.privacy.android.domain.repository.SortOrderRepository
import javax.inject.Inject

/**
 * Monitor the links sort order.
 */
class MonitorLinksSortOrderUseCase @Inject constructor(
    private val sortOrderRepository: SortOrderRepository,
) {
    operator fun invoke() = sortOrderRepository.monitorLinksSortOrder()
}
