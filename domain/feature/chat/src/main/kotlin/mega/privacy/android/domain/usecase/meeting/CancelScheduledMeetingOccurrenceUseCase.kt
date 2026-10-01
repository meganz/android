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
        val startDateTime = occurrence.startDateTime ?: return
        updateScheduledMeetingOccurrenceUseCase(
            chatId,
            occurrence.schedId,
            startDateTime,
            startDateTime,
            occurrence.endDateTime ?: return,
            true
        )
    }
}