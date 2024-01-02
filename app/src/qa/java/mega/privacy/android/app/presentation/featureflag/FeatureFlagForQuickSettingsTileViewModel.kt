package mega.privacy.android.app.presentation.featureflag


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.domain.usecase.GetAllFeatureFlags
import mega.privacy.android.app.domain.usecase.MonitorFeatureFlagForQuickSettingsTileUseCase
import mega.privacy.android.app.domain.usecase.SetFeatureFlagForQuickSettingsTileUseCase
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.presentation.featureflag.model.FeatureFlagMapper
import javax.inject.Inject

/**
 * View model for feature flag menu
 */
@HiltViewModel
class FeatureFlagForQuickSettingsTileViewModel @Inject constructor(
    private val monitorFeatureFlagForQuickSettingsTileUseCase: MonitorFeatureFlagForQuickSettingsTileUseCase,
    private val setFeatureFlagForQuickSettingsTileUseCase: SetFeatureFlagForQuickSettingsTileUseCase,
    private val getAllFeatureFlags: GetAllFeatureFlags,
    private val featureFlagMapper: FeatureFlagMapper,
) : ViewModel() {

    private val _state = MutableStateFlow(FeatureFlagForQuickSettingsTileState())

    /**
     * UI State for feature flag list
     */
    internal val state: StateFlow<FeatureFlagState> = _state.map { originalState ->
        FeatureFlagState(
            featureFlags = originalState.features.map {
                featureFlagMapper(it, originalState.selectedFeature == it)
            },
            filter = originalState.filter,
            showDescription = originalState.showDescription,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        FeatureFlagState(),
    )

    init {
        viewModelScope.launch {
            monitorFeatureFlagForQuickSettingsTileUseCase().collect { pair ->
                _state.update {
                    it.copy(selectedFeature = pair?.first)
                }
            }
        }
        viewModelScope.launch {
            getAllFeatureFlags()
                .map {
                    it.keys.filterIsInstance<AppFeatures>()
                }
                .collect { features ->
                    _state.update {
                        it.copy(features = features)
                    }
                }
        }
    }


    /**
     * Sets feature flag value
     * @param feature
     */
    fun setFeatureEnabled(feature: AppFeatures) {
        viewModelScope.launch {
            setFeatureFlagForQuickSettingsTileUseCase(feature)
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

internal data class FeatureFlagForQuickSettingsTileState(
    val features: List<AppFeatures> = emptyList(),
    val selectedFeature: AppFeatures? = null,
    val filter: String? = null,
    val showDescription: Boolean = true,
)