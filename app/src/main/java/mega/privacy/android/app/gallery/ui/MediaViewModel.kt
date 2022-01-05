package mega.privacy.android.app.gallery.ui

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import mega.privacy.android.app.gallery.constant.MEDIA_HANDLE
import mega.privacy.android.app.gallery.data.GalleryCard
import mega.privacy.android.app.gallery.data.GalleryItem
import mega.privacy.android.app.gallery.data.GalleryItem.Companion.TYPE_HEADER
import mega.privacy.android.app.gallery.repository.MediaItemRepository
import mega.privacy.android.app.globalmanagement.SortOrderManagement
import mega.privacy.android.app.utils.ZoomUtil
import javax.inject.Inject

/**
 * MediaDiscovery viewModel
 */
@HiltViewModel
class MediaViewModel @Inject constructor(
    repository: MediaItemRepository,
    val sortOrderManagement: SortOrderManagement,
    savedStateHandle:SavedStateHandle
) : GalleryViewModel(repository, sortOrderManagement,savedStateHandle) {

    override var mZoom = ZoomUtil.MEDIA_ZOOM_LEVEL

    fun getOrder() = sortOrderManagement.getOrderCamera()

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
}