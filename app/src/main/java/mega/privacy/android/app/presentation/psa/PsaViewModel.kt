package mega.privacy.android.app.presentation.psa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.psa.mapper.PsaStateMapper
import mega.privacy.android.app.presentation.psa.model.PsaState
import mega.privacy.android.domain.usecase.psa.DismissPsaUseCase
import mega.privacy.android.domain.usecase.psa.MonitorPsaUseCase
import mega.privacy.android.domain.usecase.psa.SetDisplayedPsaUseCase
import mega.privacy.android.navigation.contract.viewmodel.asUiStateFlow
import timber.log.Timber
import javax.inject.Inject

/**
 * Psa view model
 *
 * @property monitorPsaUseCase
 * @property dismissPsaUseCase
 * @property psaStateMapper
 * @property currentTimeProvider
 * @property setDisplayedPsaUseCase
 */
@HiltViewModel
class PsaViewModel(
    private val monitorPsaUseCase: MonitorPsaUseCase,
    private val dismissPsaUseCase: DismissPsaUseCase,
    private val psaStateMapper: PsaStateMapper,
    private val currentTimeProvider: () -> Long,
    private val setDisplayedPsaUseCase: SetDisplayedPsaUseCase,
) : ViewModel() {

    @Inject
    constructor(
        monitorPsaUseCase: MonitorPsaUseCase,
        dismissPsaUseCase: DismissPsaUseCase,
        psaStateMapper: PsaStateMapper,
        setDisplayedPsaUseCase: SetDisplayedPsaUseCase,
    ) : this(
        monitorPsaUseCase = monitorPsaUseCase,
        dismissPsaUseCase = dismissPsaUseCase,
        psaStateMapper = psaStateMapper,
        currentTimeProvider = System::currentTimeMillis,
        setDisplayedPsaUseCase = setDisplayedPsaUseCase,
    )


    /**
     * State
     */
    val state: StateFlow<PsaState> by lazy {
        flow {
            runCatching {
                emitAll(
                    monitorPsaUseCase(currentTimeProvider)
                        .map { psaStateMapper(it) }
                        .onEach { Timber.d("PSA State: $it") }
                        .catch { Timber.e(it, "Error in monitoring psa") },
                )
            }.onFailure {
                Timber.e(it)
            }
        }.asUiStateFlow(scope = viewModelScope, initialValue = PsaState.NoPsa)
    }

    suspend fun setDisplayed(psaId: Int) {
        setDisplayedPsaUseCase(psaId)
    }

    /**
     * Mark as seen
     *
     * @param psaId
     */
    fun markAsSeen(psaId: Int) = viewModelScope.launch {
        dismissPsaUseCase(psaId)
    }
}
