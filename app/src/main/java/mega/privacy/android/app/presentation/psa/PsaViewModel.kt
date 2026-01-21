package mega.privacy.android.app.presentation.psa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.psa.mapper.PsaStateMapper
import mega.privacy.android.app.presentation.psa.model.PsaState
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.psa.DismissPsaUseCase
import mega.privacy.android.domain.usecase.psa.FetchPsaUseCase
import mega.privacy.android.domain.usecase.psa.MonitorPsaUseCase
import mega.privacy.android.navigation.contract.viewmodel.asUiStateFlow
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
) : ViewModel() {

    @Inject
    constructor(
        monitorPsaUseCase: MonitorPsaUseCase,
        fetchPsaUseCase: FetchPsaUseCase,
        dismissPsaUseCase: DismissPsaUseCase,
        psaStateMapper: PsaStateMapper,
        getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    ) : this(
        monitorPsaUseCase = monitorPsaUseCase,
        fetchPsaUseCase = fetchPsaUseCase,
        dismissPsaUseCase = dismissPsaUseCase,
        psaStateMapper = psaStateMapper,
        currentTimeProvider = System::currentTimeMillis,
        getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
    )

    private val seenChannel = Channel<PsaState>()

    /**
     * State
     */
    val state: StateFlow<PsaState> by lazy {
        flow {
            runCatching {
                emitAll(
                    merge(
                        monitorPsaUseCase(currentTimeProvider)
                            .map { psaStateMapper(it) }
                            .onEach { Timber.d("PSA State: $it") }
                            .catch { Timber.e(it, "Error in monitoring psa") },
                        seenChannel.receiveAsFlow()
                    )
                )
            }.onFailure {
                Timber.e(it)
            }
        }.asUiStateFlow(scope = viewModelScope, initialValue = PsaState.NoPsa)
    }

    /**
     * Mark as seen
     *
     * @param psaId
     */
    fun markAsSeen(psaId: Int) = viewModelScope.launch {
        dismissPsaUseCase(psaId)
        seenChannel.trySend(
            psaStateMapper(
                fetchPsaUseCase(
                    currentTime = currentTimeProvider(),
                    forceRefresh = false
                )
            )
        )
    }
}
