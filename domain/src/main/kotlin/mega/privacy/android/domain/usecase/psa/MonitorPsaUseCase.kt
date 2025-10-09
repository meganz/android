package mega.privacy.android.domain.usecase.psa

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import mega.privacy.android.domain.entity.psa.Psa
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
        monitorMiscLoadedUseCase().take(1).flatMapLatest {
            flow {
                while (true) {
                    fetchPsaUseCase(currentMilliSecondTimeProvider())
                        ?.let { emit(it) }
                    delay(psaCheckFrequency)
                }
            }
        }
}