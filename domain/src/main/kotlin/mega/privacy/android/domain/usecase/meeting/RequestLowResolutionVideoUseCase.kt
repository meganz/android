package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.entity.ChatRequest
import mega.privacy.android.domain.repository.CallRepository
import javax.inject.Inject

/**
 * Use case to request low resolution video
 *
 * @property callRepository     [CallRepository]
 */
class RequestLowResolutionVideoUseCase @Inject constructor(
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
    ): ChatRequest = callRepository.requestLowResVideo(
        chatId,
        listOf(clientId)
    )
}