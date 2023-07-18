package mega.privacy.android.domain.usecase.passcode

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Monitor passcode attempts use case
 */
class MonitorPasscodeAttemptsUseCase @Inject constructor() {
    /**
     * Invoke
     *
     * @return flow of failed attempts count
     */
    operator fun invoke(): Flow<Int> {
        TODO()
    }
}
