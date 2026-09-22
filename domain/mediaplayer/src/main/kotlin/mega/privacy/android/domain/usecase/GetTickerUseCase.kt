package mega.privacy.android.domain.usecase

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * The use case for getting ticker
 */
class GetTickerUseCase @Inject constructor() {
    /**
     * Get ticker
     *
     * @param intervalMilliSeconds the interval time
     */
    suspend operator fun invoke(intervalMilliSeconds: Long) = flow {
        while (true) {
            emit(Unit)
            delay(intervalMilliSeconds)
        }
    }
}