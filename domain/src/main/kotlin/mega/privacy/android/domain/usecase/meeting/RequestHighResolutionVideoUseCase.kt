package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.entity.ChatRequest
import mega.privacy.android.domain.repository.CallRepository
import javax.inject.Inject

/**
 * Use case to request high resolution video
 *
 * @property callRepository     [CallRepository]
 */
class RequestHighResolutionVideoUseCase @Inject constructor(
    private val callRepository: CallRepository,
) {
    /**
     * Invoke
     *
     * @param chatId
     * @param clientId
     */
    suspend operator fun invoke(
        chatId: Long,
        clientId: Long,
    ): ChatRequest = callRepository.requestHiResVideo(
        chatId,
        clientId
    )
}