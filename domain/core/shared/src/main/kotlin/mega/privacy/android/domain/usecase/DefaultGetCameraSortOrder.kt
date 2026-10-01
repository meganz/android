package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.repository.SortOrderRepository
import javax.inject.Inject

/**
 * Default implementation of [GetCameraSortOrder]
 *
 * @property sortOrderRepository
 */
class DefaultGetCameraSortOrder @Inject constructor(private val sortOrderRepository: SortOrderRepository) :
    GetCameraSortOrder {
    override suspend fun invoke(): SortOrder =
        sortOrderRepository.getCameraSortOrder() ?: SortOrder.ORDER_MODIFICATION_DESC
}