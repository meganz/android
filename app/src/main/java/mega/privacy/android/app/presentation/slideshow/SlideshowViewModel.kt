package mega.privacy.android.app.presentation.slideshow

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import mega.privacy.android.app.imageviewer.data.ImageItem
import mega.privacy.android.app.presentation.slideshow.model.SlideshowViewState
import javax.inject.Inject

/**
 * ViewModel for slideshow
 *
 */
@HiltViewModel
class SlideshowViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(SlideshowViewState())
    val state = _state.asStateFlow()

    fun setData(
        items: List<ImageItem>,
    ) {
        //TODO
    }
}
