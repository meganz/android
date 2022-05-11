package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import mega.privacy.android.app.domain.entity.Album
import mega.privacy.android.app.domain.entity.FavouriteInfo
import mega.privacy.android.app.domain.repository.AlbumsRepository
import java.io.File
import javax.inject.Inject

/**
 * Default get albums
 *
 * @property getAllFavorites
 * @property getThumbnail
 * @property albumsRepository
 */
class DefaultGetAlbums @Inject constructor(
        private val getAllFavorites: GetAllFavorites,
        private val getThumbnail: GetThumbnail,
        private val albumsRepository: AlbumsRepository,
) : GetAlbums {

    override fun invoke(): Flow<List<Album>> {
        return flow {
            emit(
                    listOf(
                            getFavouriteAlbum()
                    )
            )
        }
    }

    private suspend fun getFavouriteAlbum(): Album {
        val favouriteList = getAllFavorites().firstOrNull()?.filter { favItem ->
            favItem.isImage || (favItem.isVideo && inSyncFolder(favItem.parentId))
        }
        return Album.FavouriteAlbum(
                thumbnail = getThumbnailOrNull(favouriteList),
                itemCount = favouriteList?.size ?: 0
        )
    }

    private suspend fun getThumbnailOrNull(favouriteList: List<FavouriteInfo>?): File? {
        return favouriteList?.maxByOrNull { favItem ->
            favItem.modificationTime
        }?.let {
            getThumbnail(it.id)
        }
    }

    private suspend fun inSyncFolder(parentId: Long): Boolean =
            parentId == albumsRepository.getCameraUploadFolderId() || parentId == albumsRepository.getMediaUploadFolderId()
    
}