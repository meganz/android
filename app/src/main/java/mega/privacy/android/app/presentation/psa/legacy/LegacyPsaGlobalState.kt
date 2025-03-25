package mega.privacy.android.app.presentation.psa.legacy

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.psa.mapper.PsaStateMapper
import mega.privacy.android.app.presentation.psa.model.PsaState
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.psa.MonitorPsaUseCase
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Legacy psa global state
 *
 * @property monitorPsaUseCase
 * @property psaStateMapper
 * @property coroutineScope
 */
@Singleton
class LegacyPsaGlobalState @Inject constructor(
    private val monitorPsaUseCase: MonitorPsaUseCase,
    private val psaStateMapper: PsaStateMapper,
    @ApplicationScope private val coroutineScope: CoroutineScope,
) {
    private val _state = MutableStateFlow<PsaState>(PsaState.NoPsa)

    /**
     * State
     */
    val state: StateFlow<PsaState> = _state

    init {
        coroutineScope.launch {
            monitorPsaUseCase(System::currentTimeMillis)
                .map { psaStateMapper(it) }
                .catch { Timber.e(it, "Error in monitoring psa") }
                .collect { _state.value = it }
        }
    }

    fun clearPsa() {
        _state.value = PsaState.NoPsa
    }
}