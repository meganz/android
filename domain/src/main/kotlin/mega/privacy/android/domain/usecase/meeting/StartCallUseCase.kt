package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.entity.chat.ChatCall
import mega.privacy.android.domain.repository.CallRepository
import javax.inject.Inject

/**
 * Open call if already exists, start it if not.
 */
class StartCallUseCase @Inject constructor(
    private val callRepository: CallRepository,
) {

    /**
     * Invoke
     *
     * @param chatId Chat id.
     * @param video True if should enable video, false otherwise.
     * @return [ChatCall] if any, null otherwise.
     */
    suspend operator fun invoke(chatId: Long, video: Boolean) =
        if (chatId == -1L) {
            null
        } else {
            runCatching {
                callRepository.startCallRinging(chatId, video, true)
            }.fold(
                onSuccess = { request -> callRepository.getChatCall(request.chatHandle) },
                onFailure = { null }
            )
        }
}