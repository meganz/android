package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.repository.FavouritesRepository
import javax.inject.Inject

/**
 * Use case implementation class for removing favourites
 * @param repository DefaultFavouritesRepository
 */
class DefaultRemoveFavourites @Inject constructor(private val repository: FavouritesRepository) :
    RemoveFavourites {
    override suspend fun invoke(handles: List<Long>) {
        repository.removeFavourites(handles)
    }
}