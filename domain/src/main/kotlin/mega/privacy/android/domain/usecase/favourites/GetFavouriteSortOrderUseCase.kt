package mega.privacy.android.domain.usecase.favourites

import mega.privacy.android.domain.usecase.GetCloudSortOrder
import javax.inject.Inject

/**
 * Default get favourite sort order
 *
 * @property getCloudSortOrder
 * @property mapFavouriteSortOrderUseCase
 */
class GetFavouriteSortOrderUseCase @Inject constructor(
    private val getCloudSortOrder: GetCloudSortOrder,
    private val mapFavouriteSortOrderUseCase: MapFavouriteSortOrderUseCase,
) {
    /**
     * Invoke
     */
    suspend operator fun invoke() = mapFavouriteSortOrderUseCase(getCloudSortOrder())
}