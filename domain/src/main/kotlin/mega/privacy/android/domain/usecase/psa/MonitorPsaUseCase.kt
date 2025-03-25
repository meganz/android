package mega.privacy.android.domain.usecase.psa

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import mega.privacy.android.domain.entity.psa.Psa
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
    private val psaCheckFrequency: Duration,
) {
    @Inject
    constructor(
        fetchPsaUseCase: FetchPsaUseCase,
    ) : this(
        fetchPsaUseCase = fetchPsaUseCase,
        psaCheckFrequency = 5.minutes,
    )

    /**
     * Invoke
     *
     * @param currentMilliSecondTimeProvider
     */
    operator fun invoke(currentMilliSecondTimeProvider: () -> Long): Flow<Psa> = flow {
        while (true) {
            fetchPsaUseCase(currentMilliSecondTimeProvider())
                ?.let { emit(it) }
            delay(psaCheckFrequency)
        }
    }
}