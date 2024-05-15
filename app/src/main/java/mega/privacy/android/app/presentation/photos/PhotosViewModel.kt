package mega.privacy.android.app.presentation.photos

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import mega.privacy.android.app.presentation.photos.model.PhotosTab
import mega.privacy.android.app.presentation.photos.model.PhotosViewState
import javax.inject.Inject

/**
 * The ViewModel for the Photos screen
 */
@HiltViewModel
class PhotosViewModel @Inject constructor() : ViewModel() {
    private val _state = MutableStateFlow(PhotosViewState())

    /**
     * The Photos State
     */
    val state = _state.asStateFlow()

    /**
     * Updates the selected Tab in the UI State
     *
     * @param selectedTab The selected [PhotosTab]
     */
    fun onTabSelected(selectedTab: PhotosTab) {
        _state.update {
            it.copy(selectedTab = selectedTab)
        }
    }

    /**
     * Updates the UI State as to whether the Menu is showing or not
     *
     * @param isShow true if the Menu is showing
     */
    fun setMenuShowing(isShow: Boolean) {
        _state.update {
            it.copy(isMenuShowing = isShow)
        }
    }
}
