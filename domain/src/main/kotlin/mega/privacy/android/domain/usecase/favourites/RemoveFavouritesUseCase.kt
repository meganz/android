package mega.privacy.android.domain.usecase.favourites

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.FavouritesRepository
import javax.inject.Inject

/**
 * Use case interface for removing favourites
 */
class RemoveFavouritesUseCase @Inject constructor(
    private val favouritesRepository: FavouritesRepository,
) {

    /**
     * Removing favourites
     * @param nodeIds the nodeId of items that are removed.
     */
    suspend operator fun invoke(nodeIds: List<NodeId>) {
        favouritesRepository.removeFavourites(nodeIds)
    }
}