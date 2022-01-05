package mega.privacy.android.app.fragments.homepage.photos

import dagger.hilt.android.lifecycle.HiltViewModel
import mega.privacy.android.app.gallery.data.GalleryItem
import mega.privacy.android.app.gallery.repository.ImagesItemRepository
import mega.privacy.android.app.gallery.ui.GalleryViewModel
import mega.privacy.android.app.globalmanagement.SortOrderManagement
import mega.privacy.android.app.utils.ZoomUtil
import javax.inject.Inject

@HiltViewModel
class ImagesViewModel @Inject constructor(
    repository: ImagesItemRepository,
    sortOrderManagement: SortOrderManagement
) : GalleryViewModel(repository, sortOrderManagement) {

    override var mZoom = ZoomUtil.IMAGES_ZOOM_LEVEL

    override fun getFilterRealPhotoCountCondition(item: GalleryItem): Boolean {
       return item.type == GalleryItem.TYPE_IMAGE
    }

    override fun initMediaIndex(item: GalleryItem, mediaIndex: Int): Int {
        var tempIndex = mediaIndex
        if (item.type == GalleryItem.TYPE_IMAGE) item.indexForViewer = tempIndex++
        return tempIndex
    }
}