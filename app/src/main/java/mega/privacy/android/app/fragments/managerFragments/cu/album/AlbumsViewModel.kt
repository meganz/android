package mega.privacy.android.app.fragments.managerFragments.cu.album

import dagger.hilt.android.lifecycle.HiltViewModel
import mega.privacy.android.app.gallery.data.GalleryItem
import mega.privacy.android.app.gallery.repository.FavouriteAlbumRepository
import mega.privacy.android.app.gallery.ui.GalleryViewModel
import mega.privacy.android.app.globalmanagement.SortOrderManagement
import mega.privacy.android.app.utils.ZoomUtil
import javax.inject.Inject

/**
 * AlbumsViewModel works with AlbumsFragment
 */
@HiltViewModel
class AlbumsViewModel @Inject constructor(
    repository: FavouriteAlbumRepository,
    val sortOrderManagement: SortOrderManagement
) : GalleryViewModel(repository, sortOrderManagement) {

    override var mZoom = ZoomUtil.ALBUM_ZOOM_LEVEL

    /**
     * Get current sort rule from SortOrderManagement
     */
    fun getOrder() = sortOrderManagement.getOrderCamera()

    /**
     * A list for storing albums
     */
    val albumList = ArrayList<AlbumCover>()

    /**
     * Create a default albums when init
     */
    fun createDefaultAlbums(title: String) {
        albumList.add(
            AlbumCover(
                title = title
            )
        )
    }

    /**
     * Update Album Covers
     *
     * @return new Album covers list
     */
    fun updateAlbumCovers(favorites: List<GalleryItem>, title: String): List<AlbumCover> {
        val newAlbumList = ArrayList<AlbumCover>()
        // update favorite album
        albumList.map { album ->
            if (favorites.isNotEmpty()) {
                // filter headers
                val pureFavorites = favorites.filter { favorite ->
                    favorite.type != GalleryItem.TYPE_HEADER
                }
                newAlbumList.add(
                    AlbumCover(
                        count = pureFavorites.size,
                        thumbnail = pureFavorites[0].thumbnail,
                        title = title
                    )
                )
            } else {
                newAlbumList.add(album)
            }
        }

        return newAlbumList
    }
}