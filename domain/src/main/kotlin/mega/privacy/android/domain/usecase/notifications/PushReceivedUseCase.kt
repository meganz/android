package mega.privacy.android.domain.usecase.notifications

import mega.privacy.android.domain.entity.ChatRequest
import mega.privacy.android.domain.repository.PushesRepository
import javax.inject.Inject

/**
 * Push received use case.
 */
class PushReceivedUseCase @Inject constructor(
    private val pushesRepository: PushesRepository,
) {

    /**
     * Invoke
     *
     * @param beep   True if should beep, false otherwise.
     * @param chatId Chat identifier.
     * @return Result of the request. Required for creating the notification.
     */
    suspend operator fun invoke(beep: Boolean, chatId: Long) =
        pushesRepository.pushReceived(beep, chatId)
}