package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.entity.FavouriteFile
import mega.privacy.android.domain.entity.FavouriteInfo
import mega.privacy.android.domain.entity.ImageFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.repository.AlbumsRepository
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
    override fun invoke() = getFavouritesFlow()

    /**
     * Get Favourites Flow
     */
    private fun getFavouritesFlow() = flow {
        emit(listOf(
            getFavouriteAlbum(null)
        ))
        emitAll(
            getAllFavorites().map {
                listOf(
                    getFavouriteAlbum(it)
                )
            }
        )
    }

    /**
     * Filter favourites albums
     */
    private suspend fun getFavouriteAlbum(favInfoList: List<FavouriteInfo>?): Album.FavouriteAlbum {
        val favouriteList = favInfoList?.filterIsInstance<FavouriteFile>()?.filter { favItem ->
            favItem.type is ImageFileTypeInfo || (favItem.type is VideoFileTypeInfo && inSyncFolder(
                favItem.parentId))
        }
        return Album.FavouriteAlbum(
            thumbnail = getThumbnailOrNull(favouriteList),
            itemCount = favouriteList?.size ?: 0
        )
    }

    /**
     * Get thumbnail,return null when there is an exception
     */
    private suspend fun getThumbnailOrNull(favouriteList: List<FavouriteFile>?): File? {
        return favouriteList?.maxByOrNull { favItem ->
            favItem.modificationTime
        }?.let {
            try {
                getThumbnail(it.id)
            } catch (e: Exception) {
                null
            }
        }
    }

    private suspend fun inSyncFolder(parentId: Long): Boolean =
        parentId == albumsRepository.getCameraUploadFolderId() || parentId == albumsRepository.getMediaUploadFolderId()
}