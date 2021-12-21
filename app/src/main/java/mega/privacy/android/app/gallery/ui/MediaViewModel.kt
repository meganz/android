package mega.privacy.android.app.gallery.ui

import androidx.hilt.lifecycle.ViewModelInject
import mega.privacy.android.app.gallery.data.GalleryItem
import mega.privacy.android.app.gallery.data.GalleryItem.Companion.TYPE_HEADER
import mega.privacy.android.app.gallery.repository.MediaItemRepository
import mega.privacy.android.app.globalmanagement.SortOrderManagement
import mega.privacy.android.app.utils.ZoomUtil

/**
 * MediaDiscovery viewModel
 */
class MediaViewModel @ViewModelInject constructor(
        private val repository: MediaItemRepository,
        val sortOrderManagement: SortOrderManagement
) : GalleryViewModel(repository, sortOrderManagement) {

    override var mZoom = ZoomUtil.MEDIA_ZOOM_LEVEL

    private var currentHandle: Long = 0L

    fun getOrder() = sortOrderManagement.getOrderCamera()

    private var isFetchItemsDirectly = false

    override fun isFetchItemsDirectly() = isFetchItemsDirectly

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
        currentHandle = handle
    }

    /**
     * manually getAndFilterFilesByHandle, should put the right flag [isFetchItemsDirectly] and [shouldMapCards]
     */
    fun getAndFilterFilesByHandle() {
        repository.setCurrentHandle(currentHandle)
        isFetchItemsDirectly = true
        shouldMapCards = true
        triggerDataLoad()
    }
}