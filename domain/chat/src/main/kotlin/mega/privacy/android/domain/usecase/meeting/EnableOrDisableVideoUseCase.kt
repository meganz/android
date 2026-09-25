package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.entity.ChatRequest
import mega.privacy.android.domain.repository.CallRepository
import javax.inject.Inject

/**
 * Use Case to Enable/Disable video
 *
 * @property callRepository     [CallRepository]
 */
class EnableOrDisableVideoUseCase @Inject constructor(
    private val callRepository: CallRepository,
) {

    /**
     * Enable or disable video
     *
     * @param enable  True to start the video device, false to stop.
     * @param chatId    Chat Id.
     */
    suspend operator fun invoke(chatId: Long, enable: Boolean): ChatRequest =
        when {
            enable -> callRepository.enableVideo(chatId)
            else -> callRepository.disableVideo(chatId)
        }
}
