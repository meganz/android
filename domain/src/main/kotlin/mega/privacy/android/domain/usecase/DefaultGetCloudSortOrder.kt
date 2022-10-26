package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.repository.SortOrderRepository
import javax.inject.Inject

/**
 * Default implementation of [GetCloudSortOrder]
 *
 * @property sortOrderRepository
 */
class DefaultGetCloudSortOrder @Inject constructor(private val sortOrderRepository: SortOrderRepository) :
    GetCloudSortOrder {
    override suspend fun invoke(): SortOrder =
        sortOrderRepository.getCloudSortOrder() ?: SortOrder.ORDER_DEFAULT_ASC
}