package mega.privacy.android.app.fragments.managerFragments.cu.album

import dagger.hilt.android.lifecycle.HiltViewModel
import mega.privacy.android.app.gallery.data.GalleryItem
import mega.privacy.android.app.gallery.repository.FavouriteAlbumRepository
import mega.privacy.android.app.gallery.ui.GalleryViewModel
import mega.privacy.android.app.globalmanagement.SortOrderManagement
import mega.privacy.android.app.utils.ZoomUtil
import javax.inject.Inject

/**
 * AlbumContentViewModel work with AlbumContentFragment
 */
@HiltViewModel
class AlbumContentViewModel @Inject constructor(
    repository: FavouriteAlbumRepository,
    val sortOrderManagement: SortOrderManagement
) : GalleryViewModel(repository, sortOrderManagement) {

    override var mZoom = ZoomUtil.ALBUM_ZOOM_LEVEL

    /**
     * Get current sort rule from SortOrderManagement
     */
    fun getOrder() = sortOrderManagement.getOrderCamera()
}