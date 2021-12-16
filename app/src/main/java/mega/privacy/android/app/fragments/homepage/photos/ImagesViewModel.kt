package mega.privacy.android.app.fragments.homepage.photos

import androidx.hilt.lifecycle.ViewModelInject
import mega.privacy.android.app.gallery.data.GalleryItem
import mega.privacy.android.app.gallery.repository.ImagesItemRepository
import mega.privacy.android.app.gallery.ui.GalleryViewModel
import mega.privacy.android.app.utils.ZoomUtil
import nz.mega.sdk.MegaApiJava

class ImagesViewModel @ViewModelInject constructor(
    repository: ImagesItemRepository
) : GalleryViewModel(repository) {

    override var mZoom = ZoomUtil.IMAGES_ZOOM_LEVEL

    override var mOrder = MegaApiJava.ORDER_MODIFICATION_DESC

    override fun isAutoGetItem() = true

    override fun getFilterRealPhotoCountCondition(item: GalleryItem): Boolean {
       return item.type == GalleryItem.TYPE_IMAGE
    }

    override fun initMediaIndex(item: GalleryItem, mediaIndex: Int): Int {
        var tempIndex = mediaIndex
        if (item.type == GalleryItem.TYPE_IMAGE) item.indexForViewer = tempIndex++
        return tempIndex
    }

//    fun getHandlesOfPhotos(): LongArray? {
//        val list = items.value?.filter {
//            it.type == GalleryItem.TYPE_IMAGE
//        }?.map { node -> node.node?.handle ?: MegaApiJava.INVALID_HANDLE }
//        return list?.toLongArray()
//    }
}