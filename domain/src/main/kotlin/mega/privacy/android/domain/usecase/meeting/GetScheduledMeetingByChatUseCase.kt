package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.entity.chat.ChatScheduledMeeting
import mega.privacy.android.domain.repository.CallRepository
import javax.inject.Inject

/**
 * Use case for getting the scheduled meeting from a chat
 */
class GetScheduledMeetingByChatUseCase @Inject constructor(
    private val callRepository: CallRepository,
) {

    /**
     * Invocation method.
     *
     * @param chatId                    Chat id.
     * @return [ChatScheduledMeeting]   containing the updated data.
     */
    suspend operator fun invoke(chatId: Long): List<ChatScheduledMeeting>? =
        callRepository.getScheduledMeetingsByChat(chatId = chatId)
}
