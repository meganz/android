package mega.privacy.android.app.fragments.managerFragments.cu.album

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.di.IoDispatcher
import mega.privacy.android.app.gallery.repository.FavouriteAlbumRepository
import mega.privacy.android.app.gallery.ui.GalleryViewModel
import mega.privacy.android.app.utils.ZoomUtil
import mega.privacy.android.domain.usecase.GetCameraSortOrder
import javax.inject.Inject

/**
 * AlbumContentViewModel work with AlbumContentFragment
 */
@HiltViewModel
class AlbumContentViewModel @Inject constructor(
    val repository: FavouriteAlbumRepository,
    getCameraSortOrder: GetCameraSortOrder,
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
) : GalleryViewModel(
    galleryItemRepository = repository,
    ioDispatcher = ioDispatcher,
    getCameraSortOrder = getCameraSortOrder,
) {

    override var mZoom = ZoomUtil.ALBUM_ZOOM_LEVEL

    /**
     * Get current sort rule from SortOrderManagement
     */
    fun getOrder() = runBlocking { getCameraSortOrder() }
}