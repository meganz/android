package mega.privacy.android.app.presentation.photos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.presentation.photos.model.PhotosTab
import mega.privacy.android.app.presentation.photos.model.PhotosViewState
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * The ViewModel for the Photos screen
 *
 * @property getFeatureFlagValueUseCase
 */
@HiltViewModel
class PhotosViewModel @Inject constructor(
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(PhotosViewState())

    init {
        checkSettingsCameraUploadsComposeState()
    }

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

    /**
     * Checks the state of [AppFeatures.SettingsCameraUploadsCompose] and decide if the Compose
     * version of Settings Camera Uploads should be shown or not
     */
    private fun checkSettingsCameraUploadsComposeState() {
        viewModelScope.launch {
            runCatching {
                getFeatureFlagValueUseCase(AppFeatures.SettingsCameraUploadsCompose)
            }.onSuccess { isFeatureFlagEnabled ->
                Timber.d("Successfully retrieved the Settings Camera Uploads Compose Feature Flag")
                _state.update { it.copy(enableSettingsCameraUploadsCompose = isFeatureFlagEnabled) }
            }.onFailure {
                Timber.e("Unable to Retrieve the Settings Camera Uploads Compose Feature Flag\n${it.message}")
            }
        }
    }
}
