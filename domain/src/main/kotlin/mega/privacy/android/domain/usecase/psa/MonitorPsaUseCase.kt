package mega.privacy.android.domain.usecase.psa

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import mega.privacy.android.domain.entity.psa.Psa
import mega.privacy.android.domain.logging.Log
import mega.privacy.android.domain.usecase.setting.MonitorMiscLoadedUseCase
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Monitor psa use case
 *
 * @property fetchPsaUseCase
 * @property psaCheckFrequency
 */
class MonitorPsaUseCase(
    private val fetchPsaUseCase: FetchPsaUseCase,
    private val monitorMiscLoadedUseCase: MonitorMiscLoadedUseCase,
    private val psaCheckFrequency: Duration,
) {
    @Inject
    constructor(
        fetchPsaUseCase: FetchPsaUseCase,
        monitorMiscLoadedUseCase: MonitorMiscLoadedUseCase,
    ) : this(
        fetchPsaUseCase = fetchPsaUseCase,
        monitorMiscLoadedUseCase = monitorMiscLoadedUseCase,
        psaCheckFrequency = 5.minutes,
    )

    /**
     * Invoke
     *
     * @param currentMilliSecondTimeProvider
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(currentMilliSecondTimeProvider: () -> Long): Flow<Psa> =
        monitorMiscLoadedUseCase()
            .onEach { Log.d("Monitor PSA use case - monitorMiscLoadedUseCase: $it") }
            .take(1).flatMapLatest {
                flow {
                    fetchPsaUseCase(
                        currentTime = currentMilliSecondTimeProvider(),
                        forceRefresh = true
                    )?.let { emit(it) }
                    while (true) {
                        delay(psaCheckFrequency)
                        fetchPsaUseCase(
                            currentTime = currentMilliSecondTimeProvider(),
                            forceRefresh = false
                        )?.let { emit(it) }
                    }
                }
            }
}