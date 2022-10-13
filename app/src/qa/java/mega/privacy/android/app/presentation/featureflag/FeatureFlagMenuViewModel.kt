package mega.privacy.android.app.presentation.featureflag


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mega.privacy.android.app.domain.usecase.GetAllFeatureFlags
import mega.privacy.android.app.domain.usecase.SetFeatureFlag
import mega.privacy.android.app.presentation.featureflag.model.FeatureFlag
import mega.privacy.android.app.presentation.featureflag.model.FeatureFlagMapper
import mega.privacy.android.domain.entity.Feature
import javax.inject.Inject

/**
 * View model for feature flag menu
 */
@HiltViewModel
class FeatureFlagMenuViewModel @Inject constructor(
    private val setFeatureFlag: SetFeatureFlag,
    private val getAllFeatureFlags: GetAllFeatureFlags,
    private val featureFlagMapper: FeatureFlagMapper,
) : ViewModel() {

    private val _state = MutableStateFlow(emptyList<FeatureFlag>())

    /**
     * UI State for feature flag list
     */
    val state: StateFlow<List<FeatureFlag>> = _state

    init {
        viewModelScope.launch {
            getAllFeatureFlags().map { map: Map<Feature, Boolean> ->
                map.map { (key, value) ->
                    featureFlagMapper(key, value)
                }
            }.collect {
                _state.value = it
            }
        }
    }


    /**
     * Sets feature flag value
     * @param featureName : Name of the feature
     * @param isEnabled: Boolean value
     */
    fun setFeatureEnabled(featureName: String, isEnabled: Boolean) {
        viewModelScope.launch {
            setFeatureFlag(featureName, isEnabled)
        }
    }
}