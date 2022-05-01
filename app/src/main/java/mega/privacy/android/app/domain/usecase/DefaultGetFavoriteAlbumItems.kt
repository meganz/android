package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest
import mega.privacy.android.app.domain.entity.AlbumItemInfo
import mega.privacy.android.app.domain.repository.AlbumsRepository
import javax.inject.Inject

/**
 * The use case implementation class to get favourites for album
 * @param repository AlbumsRepository
 */
class DefaultGetFavoriteAlbumItems @Inject constructor(private val repository: AlbumsRepository) :
    GetFavouriteAlbumItems {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun invoke(): Flow<List<AlbumItemInfo>> =
        flow {
            emit(repository.getFavouriteAlbumItems())
            emitAll(repository.monitorNodeChange().mapLatest { repository.getFavouriteAlbumItems() })
        }

}