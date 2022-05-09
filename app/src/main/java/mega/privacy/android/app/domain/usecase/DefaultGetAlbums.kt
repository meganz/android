package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.flow.*
import mega.privacy.android.app.domain.entity.Album
import mega.privacy.android.app.domain.entity.Album.*
import mega.privacy.android.app.domain.entity.FavouriteInfo
import mega.privacy.android.app.domain.repository.AlbumsRepository
import javax.inject.Inject

class DefaultGetAlbums @Inject constructor(
    private val getAllFavorites: GetAllFavorites,
    private val getThumbnail: GetThumbnail,
    private val albumsRepository: AlbumsRepository,
) : GetAlbums {

    override fun invoke(): Flow<List<Album>> {
        return getAllFavorites().map { favouriteInfoList ->
            val albumList = ArrayList<Album>()
            if (favouriteInfoList.isNotEmpty()) {
                val filterList = favouriteInfoList.filter { favouriteInfo ->
                    favouriteInfo.isImage() ||
                            (favouriteInfo.isVideo() &&
                                    isInSyncFolder(
                                        parentId = favouriteInfo.parentId,
                                        cameraUploadFolderId = albumsRepository.getCameraUploadFolderId(),
                                        mediaUploadFolderId = albumsRepository.getMediaUploadFolderId()
                                    ))
                }

                if (filterList.isEmpty()) {
                    addEmptyFavouriteAlbum(albumList)
                } else {
                    // Get newestFavouriteInfo
                    val newestFavouriteInfo = filterList.maxByOrNull {
                        it.modificationTime
                    }!!
                    // Get thumbnail
                    val thumbnail = getThumbnail(newestFavouriteInfo.id)
                    albumList.add(
                        FavouriteAlbum(
                            thumbnail = thumbnail,
                            itemCount = filterList.size
                        )
                    )
                }
            } else {
                addEmptyFavouriteAlbum(albumList)
            }
            albumList
        }
    }

    private fun addEmptyFavouriteAlbum(albumList: ArrayList<Album>) {
        albumList.add(
            FavouriteAlbum(
                thumbnail = null,
                itemCount = 0
            )
        )
    }

    /**
     * Check the file is in Camera Uploads(CU) or Media Uploads(MU) folder, if it is in, the parent handle will be camSyncHandle or secondaryMediaFolderEnabled
     *
     * @return True, the file is in CU or MU folder. False, it is not in.
     */
    private fun isInSyncFolder(
        parentId: Long,
        cameraUploadFolderId: Long?,
        mediaUploadFolderId: Long?
    ): Boolean {
        // check node in Camera Uploads Folder if camSyncHandle existed
        cameraUploadFolderId?.let { it ->
            if (parentId == it)
                return true
        }
        //  check node in  Media Uploads Folder if megaHandleSecondaryFolder handle existed
        mediaUploadFolderId?.let { it ->
            if (parentId == it)
                return true
        }
        return false
    }
}