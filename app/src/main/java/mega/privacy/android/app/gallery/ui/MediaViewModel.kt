package mega.privacy.android.app.gallery.ui

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import mega.privacy.android.app.gallery.data.GalleryItem
import mega.privacy.android.app.gallery.data.GalleryItem.Companion.TYPE_HEADER
import mega.privacy.android.app.gallery.repository.MediaItemRepository
import mega.privacy.android.app.utils.ZoomUtil

class MediaViewModel @ViewModelInject constructor(
     private val repository: MediaItemRepository,
) : GalleryViewModel(repository) {

    override var _mZoom: Int = ZoomUtil.PHOTO_ZOOM_LEVEL
    var isAuto:Boolean = false

    override fun isAutoGetItem(): Boolean = isAuto

    private fun setIsAutoGetItem(isAuto:Boolean) {
        this.isAuto = isAuto
    }

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

    fun setHandle(handle:Long){
        repository.setCurrentHandle(handle)
    }

    fun getAndFilterFilesByHandle(){
        setIsAutoGetItem(true)
        items = getAndFilterFiles()
    }
}