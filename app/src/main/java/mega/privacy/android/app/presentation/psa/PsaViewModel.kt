package mega.privacy.android.app.presentation.psa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.presentation.psa.legacy.LegacyPsaGlobalState
import mega.privacy.android.app.presentation.psa.mapper.PsaStateMapper
import mega.privacy.android.app.presentation.psa.model.PsaState
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.psa.DismissPsaUseCase
import mega.privacy.android.domain.usecase.psa.FetchPsaUseCase
import mega.privacy.android.domain.usecase.psa.MonitorPsaUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Psa view model
 *
 * legacyState - We are using this instead of the actual implementation to unify the experience while legacy screens still exist
 */
@HiltViewModel
class PsaViewModel(
    private val monitorPsaUseCase: MonitorPsaUseCase,
    private val fetchPsaUseCase: FetchPsaUseCase,
    private val dismissPsaUseCase: DismissPsaUseCase,
    private val psaStateMapper: PsaStateMapper,
    private val currentTimeProvider: () -> Long,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val legacyState: LegacyPsaGlobalState,
) : ViewModel() {

    @Inject
    constructor(
        monitorPsaUseCase: MonitorPsaUseCase,
        fetchPsaUseCase: FetchPsaUseCase,
        dismissPsaUseCase: DismissPsaUseCase,
        psaStateMapper: PsaStateMapper,
        getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
        legacyState: LegacyPsaGlobalState,
    ) : this(
        monitorPsaUseCase = monitorPsaUseCase,
        fetchPsaUseCase = fetchPsaUseCase,
        dismissPsaUseCase = dismissPsaUseCase,
        psaStateMapper = psaStateMapper,
        currentTimeProvider = System::currentTimeMillis,
        getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
        legacyState = legacyState,
    )

    private val _state = MutableStateFlow<PsaState>(PsaState.NoPsa)

    /**
     * State
     */
    val state: StateFlow<PsaState> = _state

    init {
        viewModelScope.launch {
            runCatching {
                if (getFeatureFlagValueUseCase(AppFeatures.NewPsaState)) {
                    monitorPsaUseCase(currentTimeProvider)
                        .map { psaStateMapper(it) }
                        .catch { Timber.e(it, "Error in monitoring psa") }
                        .collect { _state.value = it }
                } else{
                    legacyState.state
                        .collect { _state.value = it }
                }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    /**
     * Mark as seen
     *
     * @param psaId
     */
    fun markAsSeen(psaId: Int) = viewModelScope.launch {
        if (getFeatureFlagValueUseCase(AppFeatures.NewPsaState)) {
            dismissPsaUseCase(psaId)
            _state.value = psaStateMapper(fetchPsaUseCase(currentTimeProvider()))
        } else {
            dismissPsaUseCase(psaId)
            legacyState.clearPsa()
        }
    }
}
