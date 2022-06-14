package mega.privacy.android.app.fragments.homepage.photos

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import mega.privacy.android.app.di.IoDispatcher
import mega.privacy.android.app.domain.usecase.GetCameraSortOrder
import mega.privacy.android.app.gallery.data.GalleryItem
import mega.privacy.android.app.gallery.data.MediaCardType
import mega.privacy.android.app.gallery.repository.ImagesItemRepository
import mega.privacy.android.app.gallery.ui.GalleryViewModel
import mega.privacy.android.app.globalmanagement.SortOrderManagement
import mega.privacy.android.app.utils.ZoomUtil
import javax.inject.Inject

@HiltViewModel
class ImagesViewModel @Inject constructor(
    repository: ImagesItemRepository,
    getCameraSortOrder: GetCameraSortOrder,
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
) : GalleryViewModel(
    galleryItemRepository = repository,
    ioDispatcher = ioDispatcher,
    getCameraSortOrder = getCameraSortOrder,
) {

    override var mZoom = ZoomUtil.IMAGES_ZOOM_LEVEL

    override fun getFilterRealPhotoCountCondition(item: GalleryItem) = item.type == MediaCardType.Image

    override fun initMediaIndex(item: GalleryItem, mediaIndex: Int): Int {
        var tempIndex = mediaIndex
        if (item.type == MediaCardType.Image) item.indexForViewer = tempIndex++
        return tempIndex
    }
}