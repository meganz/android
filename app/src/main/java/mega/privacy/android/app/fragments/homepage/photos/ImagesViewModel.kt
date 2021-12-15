package mega.privacy.android.app.fragments.homepage.photos

import androidx.hilt.lifecycle.ViewModelInject
import mega.privacy.android.app.gallery.data.GalleryItem
import mega.privacy.android.app.gallery.repository.ImagesItemRepository
import mega.privacy.android.app.gallery.ui.MediaViewModel
import mega.privacy.android.app.utils.ZoomUtil
import nz.mega.sdk.MegaApiJava

class ImagesViewModel @ViewModelInject constructor(
    repository: ImagesItemRepository
) : MediaViewModel(repository) {

    override var mZoom: Int = ZoomUtil.IMAGES_ZOOM_LEVEL

    override fun initMediaIndex(item: GalleryItem, mediaIndex: Int): Int {
        var tempIndex = mediaIndex
        if (item.type == GalleryItem.TYPE_IMAGE) item.indexForViewer = tempIndex++
        return tempIndex
    }

    fun getHandlesOfPhotos(): LongArray? {
        val list = items.value?.filter {
            it.type == GalleryItem.TYPE_IMAGE
        }?.map {
            it.node?.handle ?: MegaApiJava.INVALID_HANDLE
        }

        return list?.toLongArray()
    }

    fun getRealPhotoCount(): Int {
        items.value?.filter { it.type == GalleryItem.TYPE_IMAGE }?.let {
            return it.size
        }
        return 0
    }
}