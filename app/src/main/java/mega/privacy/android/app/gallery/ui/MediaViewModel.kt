package mega.privacy.android.app.gallery.ui

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
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
    savedStateHandle: SavedStateHandle
) : GalleryViewModel(repository, sortOrderManagement, savedStateHandle) {

    override var mZoom = ZoomUtil.MEDIA_ZOOM_LEVEL

    fun getOrder() = sortOrderManagement.getOrderCamera()
}