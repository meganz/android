package mega.privacy.android.domain.usecase.favourites

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.FavouritesRepository
import javax.inject.Inject

/**
 * Use case interface for adding favourites
 */
class AddFavouritesUseCase @Inject constructor(
    private val favouritesRepository: FavouritesRepository,
) {

    /**
     * Adding favourites
     * @param nodeIds the handle of items that are added.
     */
    suspend operator fun invoke(nodeIds: List<NodeId>) {
        favouritesRepository.addFavourites(nodeIds)
    }
}