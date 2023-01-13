package mega.privacy.android.domain.usecase

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * The implementation for getting ticker
 */
class DefaultGetTicker @Inject constructor() : GetTicker {
    override suspend fun invoke(intervalMilliSeconds: Long) = flow {
        while (true) {
            emit(Unit)
            delay(intervalMilliSeconds)
        }
    }
}