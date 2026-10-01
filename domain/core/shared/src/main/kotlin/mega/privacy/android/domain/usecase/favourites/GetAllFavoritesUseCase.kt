package mega.privacy.android.domain.usecase.favourites

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.repository.FavouritesRepository
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.AddNodeType
import javax.inject.Inject


/**

 * Default get all favorites
 *
 * @property favouritesRepository
 * @property nodeRepository
 * @property addNodeType
 */
class GetAllFavoritesUseCase @Inject constructor(
    private val favouritesRepository: FavouritesRepository,
    private val nodeRepository: NodeRepository,
    private val addNodeType: AddNodeType,
    private val sortFavouritesUseCase: SortFavouritesUseCase,
) {
    /**
     * get favourites
     *
     * @param excludeSensitives When true, the SDK search filter excludes sensitive (hidden)
     *   favourites so the UI never sees them. Defaults to false.
     * @return Flow<List<FavouriteInfo>>
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(excludeSensitives: Boolean = false): Flow<List<TypedNode>> =
        nodeRepository.monitorNodeUpdates()
            .onStart { emit(NodeUpdate(emptyMap())) }
            .mapLatest {
                val favorites = favouritesRepository.getAllFavorites(excludeSensitives)
                sortFavouritesUseCase(favorites).map { addNodeType(it) }
            }
}
