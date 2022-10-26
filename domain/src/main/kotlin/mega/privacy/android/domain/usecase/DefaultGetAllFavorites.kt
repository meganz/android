package mega.privacy.android.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.repository.FavouritesRepository
import mega.privacy.android.domain.repository.FileRepository
import javax.inject.Inject

/**
 * The use case implementation class to get favourites
 * @param repository FavouritesRepository
 * @param fileRepository FileRepository
 */
class DefaultGetAllFavorites @Inject constructor(
    private val repository: FavouritesRepository,
    private val fileRepository: FileRepository,
) : GetAllFavorites {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun invoke(): Flow<List<Node>> =
        flow {
            emit(repository.getAllFavorites())
            emitAll(fileRepository.monitorNodeUpdates().mapLatest { repository.getAllFavorites() })
        }

}
