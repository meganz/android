package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.repository.SortOrderRepository
import javax.inject.Inject

/**
 * GetLinksSortOrderUseCase
 *
 * @property sortOrderRepository
 */
class GetLinksSortOrderUseCase @Inject constructor(private val sortOrderRepository: SortOrderRepository) {
    suspend operator fun invoke(isSingleActivityEnabled: Boolean): SortOrder =
        sortOrderRepository.getLinksSortOrder(isSingleActivityEnabled)
            ?: SortOrder.ORDER_DEFAULT_ASC
}