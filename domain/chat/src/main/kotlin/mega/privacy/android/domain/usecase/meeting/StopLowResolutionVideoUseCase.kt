package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.entity.ChatRequest
import mega.privacy.android.domain.repository.CallRepository
import javax.inject.Inject

/**
 * Use case to stop low resolution video
 *
 * @property callRepository     [CallRepository]
 */
class StopLowResolutionVideoUseCase @Inject constructor(
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
    ): ChatRequest = callRepository.stopLowResVideo(
        chatId,
        listOf(clientId)
    )
}