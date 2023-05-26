package mega.privacy.android.app.presentation.photos.mediadiscovery

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import mega.privacy.android.app.presentation.photos.model.FilterMediaType
import mega.privacy.android.app.presentation.photos.model.ZoomLevel
import javax.inject.Inject

@HiltViewModel
class MediaDiscoveryGlobalStateViewModel @Inject constructor() : ViewModel() {
    private val _state = MutableStateFlow(ZoomLevel.Grid_3)

    val state: StateFlow<ZoomLevel> = _state

    val currentZoomLevel: ZoomLevel get() = state.value

    fun zoomIn() {
        if (currentZoomLevel == ZoomLevel.values().first()) return
        _state.update {
            ZoomLevel.values()[currentZoomLevel.ordinal - 1]
        }
    }

    fun zoomOut() {
        if (currentZoomLevel == ZoomLevel.values().last()) return
        _state.update {
            ZoomLevel.values()[currentZoomLevel.ordinal + 1]
        }
    }

    private val _filterState = MutableStateFlow(FilterMediaType.ALL_MEDIA)

    val filterState: StateFlow<FilterMediaType> = _filterState

    fun storeCurrentMediaType(mediaType: FilterMediaType) {
        _filterState.update {
            mediaType
        }
    }
}
