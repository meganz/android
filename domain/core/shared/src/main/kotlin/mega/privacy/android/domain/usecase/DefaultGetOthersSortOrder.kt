package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.repository.SortOrderRepository
import javax.inject.Inject

/**
 * Default implementation of [GetOthersSortOrder]
 *
 * @property sortOrderRepository
 */
class DefaultGetOthersSortOrder @Inject constructor(private val sortOrderRepository: SortOrderRepository) :
    GetOthersSortOrder {
    override suspend fun invoke(): SortOrder =
        sortOrderRepository.getOthersSortOrder() ?: SortOrder.ORDER_DEFAULT_ASC
}