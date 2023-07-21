package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.entity.chat.ChatScheduledMeetingOccurr
import javax.inject.Inject

/**
 * Cancel a scheduled meeting occurrence
 */
class CancelScheduledMeetingOccurrenceUseCase @Inject constructor(
    private val updateScheduledMeetingOccurrenceUseCase: UpdateScheduledMeetingOccurrenceUseCase,
) {
    /**
     * Invoke
     *
     * @param chatId MegaChatHandle that identifies a chat room
     * @param occurrence [ChatScheduledMeetingOccurr] to cancel
     * */
    suspend operator fun invoke(chatId: Long, occurrence: ChatScheduledMeetingOccurr) {
        updateScheduledMeetingOccurrenceUseCase(
            chatId,
            occurrence.schedId,
            occurrence.startDateTime ?: return,
            occurrence.startDateTime,
            occurrence.endDateTime ?: return,
            true
        )
    }
}