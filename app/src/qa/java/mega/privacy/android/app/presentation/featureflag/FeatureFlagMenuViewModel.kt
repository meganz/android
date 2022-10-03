package mega.privacy.android.app.presentation.featureflag


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.domain.usecase.GetAllFeatureFlags
import mega.privacy.android.app.domain.usecase.SetFeatureFlag
import mega.privacy.android.app.presentation.featureflag.model.FeatureFlagState
import mega.privacy.android.domain.qualifier.IoDispatcher
import javax.inject.Inject

/**
 * View model for feature flag menu
 */
@HiltViewModel
class FeatureFlagMenuViewModel @Inject constructor(
    private val setFeatureFlag: SetFeatureFlag,
    private val getAllFeatureFlags: GetAllFeatureFlags,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _state = MutableStateFlow(FeatureFlagState())

    /**
     * UI State for feature flag list
     */
    val state: StateFlow<FeatureFlagState> = _state

    init {
        viewModelScope.launch(ioDispatcher) {
            getAllFeatureFlags().map { list ->
                { state: FeatureFlagState -> state.copy(featureFlagList = list) }
            }.collect {
                _state.update(it)
            }
        }
    }

    /**
     * Sets feature flag value
     * @param featureName : Name of the feature
     * @param isEnabled: Boolean value
     */
    fun setFeatureEnabled(featureName: String, isEnabled: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            setFeatureFlag(featureName, isEnabled)
        }
    }
}