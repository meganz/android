package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.repository.CallRepository
import javax.inject.Inject


/**
 * Use case for broadcasting when a specific call has opened.
 */
class BroadcastCallScreenOpenedUseCase @Inject constructor(
    private val callRepository: CallRepository,
) {
    /**
     * Invoke
     *
     * @param isOpened  True, the call is opened. False, if the call is not opened.
     */
    suspend operator fun invoke(isOpened: Boolean) =
        callRepository.broadcastCallScreenOpened(isOpened)
}