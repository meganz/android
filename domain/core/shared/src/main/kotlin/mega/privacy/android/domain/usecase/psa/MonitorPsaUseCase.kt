package mega.privacy.android.domain.usecase.psa

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.take
import mega.privacy.android.domain.entity.psa.Psa
import mega.privacy.android.domain.logging.Log
import mega.privacy.android.domain.repository.psa.PsaRepository
import mega.privacy.android.domain.usecase.setting.MonitorMiscLoadedUseCase
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Monitor psa use case
 *
 * @property psaRepository
 * @property monitorMiscLoadedUseCase
 * @property psaCheckFrequency
 *
 */
@Singleton
class MonitorPsaUseCase(
    private val psaRepository: PsaRepository,
    private val monitorMiscLoadedUseCase: MonitorMiscLoadedUseCase,
    private val psaCheckFrequency: Duration,
) {
    @Inject
    constructor(
        psaRepository: PsaRepository,
        monitorMiscLoadedUseCase: MonitorMiscLoadedUseCase,
    ) : this(
        psaRepository = psaRepository,
        monitorMiscLoadedUseCase = monitorMiscLoadedUseCase,
        psaCheckFrequency = 5.minutes,
    )

    private var hasRun: Boolean = false
    private fun shouldRefresh(): Boolean {
        if (!hasRun) {
            hasRun = true
            return true
        }
        return false
    }

    /**
     * Invoke
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<Psa?> =
        monitorMiscLoadedUseCase()
            .onEach { Log.d("Monitor PSA use case - monitorMiscLoadedUseCase: $it") }
            .take(1).flatMapLatest {
                combine(
                    psaRepository.monitorPsa()
                        .onStart {
                            if (shouldRefresh()) psaRepository.refreshPsa()
                        },
                    flow {
                        emit(Unit)
                        while (true) {
                            delay(psaCheckFrequency)
                            psaRepository.refreshPsa()
                            emit(Unit)
                        }
                    }
                ) { psa, _ -> psa }
            }.distinctUntilChanged { old, new -> old?.id == new?.id }
            .onEach { Log.d("Monitor PSA use case - psa: $it") }

}