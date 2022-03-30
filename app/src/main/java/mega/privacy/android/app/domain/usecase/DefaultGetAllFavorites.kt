package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest
import mega.privacy.android.app.domain.entity.FavouriteInfo
import mega.privacy.android.app.domain.repository.FavouritesRepository
import javax.inject.Inject

/**
 * The use case implementation class to get favourites
 * @param repository FavouritesRepository
 */
class DefaultGetAllFavorites @Inject constructor(private val repository: FavouritesRepository) :
    GetAllFavorites {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun invoke(): Flow<List<FavouriteInfo>> =
        flow {
            emit(repository.getAllFavorites())
            emitAll(repository.monitorNodeChange().mapLatest { repository.getAllFavorites() })
        }

}