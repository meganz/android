package mega.privacy.android.domain.usecase.favourites

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
     * @param handles the handle of items that are removed.
     */
    suspend operator fun invoke(handles: List<Long>) {
        favouritesRepository.removeFavourites(handles)
    }
}