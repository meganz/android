package mega.privacy.android.domain.usecase.notifications

import mega.privacy.android.domain.repository.PushesRepository
import javax.inject.Inject

/**
 * Push received use case.
 */
class LegacyPushReceivedUseCase @Inject constructor(
    private val pushesRepository: PushesRepository,
) {

    /**
     * Invoke
     *
     * @param beep   True if should beep, false otherwise.
     * @return Result of the request. Required for creating the notification.
     */
    suspend operator fun invoke(beep: Boolean) = pushesRepository.pushReceived(beep)
}