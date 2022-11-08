package mega.privacy.android.domain.usecase

import javax.inject.Inject

/**
 * Default get favourite sort order
 *
 * @property getCloudSortOrder
 * @property mapFavouriteSortOrder
 */
class DefaultGetFavouriteSortOrder @Inject constructor(
    private val getCloudSortOrder: GetCloudSortOrder,
    private val mapFavouriteSortOrder: MapFavouriteSortOrder,
) : GetFavouriteSortOrder {
    /**
     * Invoke
     */
    override suspend fun invoke() = mapFavouriteSortOrder(getCloudSortOrder())
}