package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.repository.SortOrderRepository
import javax.inject.Inject

/**
 * Default implementation of [GetOfflineSortOrder]
 *
 * @property sortOrderRepository
 */
class DefaultGetOfflineSortOrder @Inject constructor(private val sortOrderRepository: SortOrderRepository) :
    GetOfflineSortOrder {
    override suspend fun invoke(): SortOrder =
        sortOrderRepository.getOfflineSortOrder() ?: SortOrder.ORDER_DEFAULT_ASC
}