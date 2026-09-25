package mega.privacy.android.domain.usecase.meeting.raisehandtospeak

import mega.privacy.android.domain.entity.ChatRequest
import mega.privacy.android.domain.repository.CallRepository
import javax.inject.Inject

/**
 * Raise hand to speak Use case.
 */
class RaiseHandToSpeakUseCase @Inject constructor(
    private val callRepository: CallRepository,
) {
    /**
     * Invoke
     *
     * @param chatId Chat id
     * @return                  [ChatRequest]
     */
    suspend operator fun invoke(
        chatId: Long,
    ): ChatRequest = callRepository.raiseHandToSpeak(
        chatId,
    )
}