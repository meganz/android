package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.entity.ChatRequest
import mega.privacy.android.domain.repository.CallRepository
import javax.inject.Inject

/**
 * Use Case to Enable/Disable audio
 *
 * @property callRepository     [CallRepository]
 */
class EnableOrDisableAudioUseCase @Inject constructor(
    private val callRepository: CallRepository,
) {

    /**
     * Enable or disable audio
     *
     * @param enable  True to start the audio device, false to stop.
     * @param chatId    Chat Id.
     */
    suspend operator fun invoke(chatId: Long, enable: Boolean): ChatRequest =
        when {
            enable -> callRepository.enableAudio(chatId)
            else -> callRepository.disableAudio(chatId)
        }
}