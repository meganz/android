package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.entity.chat.ChatScheduledMeetingOccurr
import javax.inject.Inject

/**
 * Update a scheduled meeting occurrence
 */
class UpdateOccurrenceUseCase @Inject constructor(
    private val updateScheduledMeetingOccurrenceUseCase: UpdateScheduledMeetingOccurrenceUseCase,
) {
    /**
     * Invoke
     *
     * @param chatId        MegaChatHandle that identifies a chat room
     * @param overrides     Start date time that along with schedId identifies the occurrence with the format (unix timestamp UTC)
     * @param occurrence    [ChatScheduledMeetingOccurr] to cancel
     * */
    suspend operator fun invoke(
        chatId: Long,
        overrides: Long,
        occurrence: ChatScheduledMeetingOccurr,
    ) {
        updateScheduledMeetingOccurrenceUseCase(
            chatId = chatId,
            schedId = occurrence.schedId,
            overrides = overrides,
            newStartDate = occurrence.startDateTime ?: return,
            newEndDate = occurrence.endDateTime ?: return,
            cancelled = false
        )
    }
}