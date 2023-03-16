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

    fun setData(
        items: List<ImageItem>,
    ) {
        viewModelScope.launch {
            val ids = items
                .filter { it.imageResult?.isVideo != true }
                .map { it.id }

            _state.update {
                it.copy(
                    items = getPhotosByIds(ids = ids.map { id -> NodeId(id) })
                )
            }
        }

    }

}
