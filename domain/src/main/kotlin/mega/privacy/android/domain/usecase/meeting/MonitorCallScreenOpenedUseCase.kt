package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.repository.CallRepository
import javax.inject.Inject

/**
 * Use case for monitoring when a specific call is opened.
 */
class MonitorCallScreenOpenedUseCase @Inject constructor(
    private val callRepository: CallRepository
) {
    /**
     * Invoke
     *
     * @return Flow of Boolean. True, if it's opened. False if not.
     */
    operator fun invoke() = callRepository.monitorCallScreenOpened()
}