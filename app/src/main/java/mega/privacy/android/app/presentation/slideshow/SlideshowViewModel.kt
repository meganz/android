package mega.privacy.android.app.presentation.slideshow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.imageviewer.data.ImageItem
import mega.privacy.android.app.presentation.slideshow.model.SlideshowViewState
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.usecase.GetPhotosByIds
import javax.inject.Inject

/**
 * ViewModel for slideshow
 *
 */
@HiltViewModel
class SlideshowViewModel @Inject constructor(
    private val getPhotosByIds: GetPhotosByIds,
) : ViewModel() {

    private val _state = MutableStateFlow(SlideshowViewState())
    val state = _state.asStateFlow()

    /**
     * Monitor ImageViewer source images
     */
    fun setData(
        items: List<ImageItem>,
    ) {
        playSlideshowItems(items = items)
    }

    /**
     * Play slideshow from ImageViewer source images
     */
    fun playSlideshowItems(items: List<ImageItem>) {
        viewModelScope.launch {
            val chunkedSourceItems = items.chunked(slideshowPlayingUnit)
            val nextPlayingChunkedIndex = chunkedItems(chunkedSourceItems = chunkedSourceItems)
            val ids = chunkedSourceItems[_state.value.currentPlayingChunkedIndex].map { it.id }
            _state.update {
                it.copy(
                    items = getPhotosByIds(ids = ids.map { id -> NodeId(id) })
                        .filterIsInstance<Photo.Image>(),
                    currentPlayingChunkedIndex = nextPlayingChunkedIndex
                )
            }
        }
    }

    private fun chunkedItems(chunkedSourceItems: List<List<ImageItem>>) =
        _state.value.currentPlayingChunkedIndex.also { currentPlayingChunkedIndex ->
            if (currentPlayingChunkedIndex > chunkedSourceItems.size.minus(1)) {
                0
            } else {
                currentPlayingChunkedIndex.inc()
            }
        }

    companion object {
        private const val slideshowPlayingUnit = 200
    }
}
