package mega.privacy.android.domain.usecase.call

import mega.privacy.android.domain.entity.call.ChatCall
import mega.privacy.android.domain.exception.chat.StartCallException
import mega.privacy.android.domain.repository.CallRepository
import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Open call if already exists, start it if not.
 */
class StartCallUseCase @Inject constructor(
    private val callRepository: CallRepository,
    private val chatRepository: ChatRepository,
) {

    /**
     * Invoke
     *
     * @param chatId Chat id.
     * @param audio True if should enable audio, false otherwise.
     * @param video True if should enable video, false otherwise.
     * @return [ChatCall] if any, null otherwise.
     */
    suspend operator fun invoke(chatId: Long, audio: Boolean, video: Boolean): ChatCall? =
        if (chatId == chatRepository.getChatInvalidHandle()) {
            throw StartCallException(chatId)
        } else {
            val result = runCatching {
                callRepository.startCallRinging(
                    chatId = chatId,
                    enabledVideo = video,
                    enabledAudio = audio
                )
            }

            if (result.isSuccess) {
                callRepository.getChatCall(result.getOrNull()?.chatHandle)
            } else {
                throw result.exceptionOrNull() ?: StartCallException(chatId)
            }
        }
}