package mega.privacy.android.app.fragments.managerFragments.cu.album

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import mega.privacy.android.app.di.IoDispatcher
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
    val repository: FavouriteAlbumRepository,
    val sortOrderManagement: SortOrderManagement,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : GalleryViewModel(
    galleryItemRepository = repository,
    sortOrderManagement = sortOrderManagement,
    ioDispatcher = ioDispatcher,
) {

    override var mZoom = ZoomUtil.ALBUM_ZOOM_LEVEL

    /**
     * Get current sort rule from SortOrderManagement
     */
    fun getOrder() = sortOrderManagement.getOrderCamera()
}