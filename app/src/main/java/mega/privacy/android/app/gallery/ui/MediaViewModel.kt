package mega.privacy.android.app.gallery.ui

import androidx.hilt.lifecycle.ViewModelInject
import mega.privacy.android.app.gallery.data.GalleryItem
import mega.privacy.android.app.gallery.data.GalleryItem.Companion.TYPE_HEADER
import mega.privacy.android.app.gallery.repository.MediaItemRepository
import mega.privacy.android.app.utils.ZoomUtil
import nz.mega.sdk.MegaApiJava

class MediaViewModel @ViewModelInject constructor(
        private val repository: MediaItemRepository,
) : GalleryViewModel(repository) {

    override var mZoom = ZoomUtil.MEDIA_ZOOM_LEVEL

    override var mOrder = MegaApiJava.ORDER_MODIFICATION_DESC

    private var isAuto = false

    override fun isAutoGetItem() = isAuto

    override fun getFilterRealPhotoCountCondition(item: GalleryItem): Boolean {
        return item.type != TYPE_HEADER
    }

    override fun initMediaIndex(item: GalleryItem, mediaIndex: Int): Int {
        var tempIndex = mediaIndex
        if (item.type != TYPE_HEADER) {
            item.indexForViewer = tempIndex++
        }
        return tempIndex
    }

    fun setHandle(handle: Long) {
        repository.setCurrentHandle(handle)
    }

    fun getAndFilterFilesByHandle() {
        isAuto = true
        items = getAndFilterFiles()
    }
}