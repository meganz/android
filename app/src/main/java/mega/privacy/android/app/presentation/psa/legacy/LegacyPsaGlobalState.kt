package mega.privacy.android.app.presentation.psa.legacy

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.receiveAsFlow
import mega.privacy.android.app.presentation.psa.mapper.PsaStateMapper
import mega.privacy.android.app.presentation.psa.model.PsaState
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.psa.MonitorPsaUseCase
import mega.privacy.android.navigation.contract.viewmodel.asUiStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

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
    private val override = Channel<PsaState>()

    /**
     * State
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<PsaState> by lazy {
        flow {
            delay(50.seconds)
            emit(true)
        }.flatMapLatest {
            merge(
                monitorPsaUseCase(System::currentTimeMillis)
                    .map { psaStateMapper(it) },
                override.receiveAsFlow()
            ).catch { Timber.e(it, "Error in monitoring psa") }
        }.asUiStateFlow(coroutineScope, PsaState.NoPsa)
    }


    fun clearPsa() {
        override.trySend(PsaState.NoPsa)
    }
}