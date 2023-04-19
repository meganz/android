package mega.privacy.android.app.presentation.photos

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import mega.privacy.android.app.presentation.photos.model.PhotosTab
import mega.privacy.android.app.presentation.photos.model.PhotosViewState
import javax.inject.Inject

@HiltViewModel
class PhotosViewModel @Inject constructor() : ViewModel() {
    private val _state = MutableStateFlow(PhotosViewState())
    val state = _state.asStateFlow()

    fun onTabSelected(selectedTab: PhotosTab) {
        _state.update {
            it.copy(selectedTab = selectedTab)
        }
    }

    fun setMenuShowing(isShow: Boolean) {
        _state.update {
            it.copy(isMenuShowing = isShow)
        }
    }
}
