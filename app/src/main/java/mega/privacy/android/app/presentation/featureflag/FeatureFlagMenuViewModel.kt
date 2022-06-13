package mega.privacy.android.app.presentation.featureflag


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import mega.privacy.android.app.di.IoDispatcher
import mega.privacy.android.app.domain.entity.FeatureFlag
import mega.privacy.android.app.domain.usecase.GetAllFeatureFlags
import mega.privacy.android.app.domain.usecase.SetFeatureFlag
import mega.privacy.android.app.presentation.featureflag.model.FeatureFlagState
import javax.inject.Inject

/**
 * View model for feature flag menu
 */
@HiltViewModel
class FeatureFlagMenuViewModel @Inject constructor(
    private val setFeatureFlag: SetFeatureFlag,
    private val getAllFeatureFlags: GetAllFeatureFlags,
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _state = MutableStateFlow(FeatureFlagState())
    val state: StateFlow<FeatureFlagState> = _state

    init {
        viewModelScope.launch(ioDispatcher) {
            getAllFeatures().map { list ->
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
    suspend fun setFeatureEnabled(featureName: String, isEnabled: Boolean) {
        setFeatureFlag(featureName, isEnabled)
    }

    /**
     * Returns flow of list of all feature flags
     *
     * @return flow of list of @FeatureFlag
     */
    suspend fun getAllFeatures(): Flow<MutableList<FeatureFlag>> {
        return getAllFeatureFlags()
    }
}