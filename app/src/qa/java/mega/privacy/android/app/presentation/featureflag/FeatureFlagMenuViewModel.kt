package mega.privacy.android.app.presentation.featureflag


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.domain.usecase.GetAllFeatureFlags
import mega.privacy.android.app.domain.usecase.SetFeatureFlag
import mega.privacy.android.app.featuretoggle.QAFeatures
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

    private val _state = MutableStateFlow(FeatureFlagState())

    /**
     * UI State for feature flag list
     */
    internal val state: StateFlow<FeatureFlagState> = _state

    init {
        viewModelScope.launch {
            getAllFeatureFlags().map { map: Map<Feature, Boolean> ->
                map.mapValues { (key, value) ->
                    featureFlagMapper(key, value)
                }
            }.collect { featureFlags ->
                _state.value =
                    FeatureFlagState(
                        featureFlags = featureFlags.values.toList(),
                        filter = _state.value.filter,
                        showDescription = featureFlags[QAFeatures.QATest]?.isEnabled == true
                    )
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

    /**
     * Sets the filter for features flags
     */
    fun onFilterChanged(filter: String) {
        _state.update {
            it.copy(filter = filter)
        }
    }
}

internal data class FeatureFlagState(
    val featureFlags: List<FeatureFlag> = emptyList(),
    val filter: String? = null,
    val showDescription: Boolean = true,
) {
    val filteredFeatureFlags = if (filter.isNullOrBlank()) {
        featureFlags
    } else {
        featureFlags.filter {
            it.featureName.contains(filter, true) || it.description.contains(filter, true)
        }
    }
}