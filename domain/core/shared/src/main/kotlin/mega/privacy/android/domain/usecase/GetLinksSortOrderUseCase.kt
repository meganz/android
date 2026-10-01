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

    /**
     * Returns the persisted Links sort order, or [SortOrder.ORDER_DEFAULT_ASC] when none is stored.
     */
    @Suppress("UNUSED_PARAMETER")
    suspend operator fun invoke(isSingleActivityEnabled: Boolean = true): SortOrder =
        sortOrderRepository.getLinksSortOrder() ?: SortOrder.ORDER_DEFAULT_ASC
}
