package mega.privacy.android.domain.usecase.call

import mega.privacy.android.domain.repository.CallRepository
import javax.inject.Inject

/**
 * Open call if already exists, start it if not.
 */
class OpenOrStartCallUseCase @Inject constructor(
    private val callRepository: CallRepository,
    private val startCallUseCase: StartCallUseCase,
) {

    /**
     * Invoke
     *
     * @param chatId Chat id.
     * @param audio True if should enable audio, false otherwise.
     * @param video True if should enable video, false otherwise.
     * @return [ChatCall] if any, null otherwise.
     */
    suspend operator fun invoke(chatId: Long, audio: Boolean, video: Boolean) =
        if (chatId == -1L) {
            null
        } else {
            callRepository.getChatCall(chatId) ?: startCallUseCase(
                chatId = chatId,
                audio = audio,
                video = video
            )
        }
}