package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.repository.SortOrderRepository
import javax.inject.Inject

/**
 * Default implementation of [GetLinksSortOrder]
 *
 * @property sortOrderRepository
 */
class DefaultGetLinksSortOrder @Inject constructor(private val sortOrderRepository: SortOrderRepository) :
    GetLinksSortOrder {
    override suspend fun invoke(): SortOrder =
        sortOrderRepository.getLinksSortOrder() ?: SortOrder.ORDER_DEFAULT_ASC
}