package mega.privacy.android.app.fragments.homepage.photos

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import mega.privacy.android.app.di.IoDispatcher
import mega.privacy.android.app.gallery.data.GalleryItem
import mega.privacy.android.app.gallery.repository.ImagesItemRepository
import mega.privacy.android.app.gallery.ui.GalleryViewModel
import mega.privacy.android.app.globalmanagement.SortOrderManagement
import mega.privacy.android.app.utils.ZoomUtil
import javax.inject.Inject

@HiltViewModel
class ImagesViewModel @Inject constructor(
        repository: ImagesItemRepository,
        sortOrderManagement: SortOrderManagement,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
) : GalleryViewModel(
        galleryItemRepository = repository,
        sortOrderManagement = sortOrderManagement,
        ioDispatcher = ioDispatcher,
) {

    override var mZoom = ZoomUtil.IMAGES_ZOOM_LEVEL

    override fun getFilterRealPhotoCountCondition(item: GalleryItem) = item.type == GalleryItem.TYPE_IMAGE

    override fun initMediaIndex(item: GalleryItem, mediaIndex: Int): Int {
        var tempIndex = mediaIndex
        if (item.type == GalleryItem.TYPE_IMAGE) item.indexForViewer = tempIndex++
        return tempIndex
    }
}