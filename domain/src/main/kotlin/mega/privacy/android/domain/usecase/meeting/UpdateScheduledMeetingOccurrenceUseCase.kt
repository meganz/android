package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.entity.ChatRequest
import mega.privacy.android.domain.repository.CallRepository
import javax.inject.Inject

/**
 * Modify an existing scheduled meeting occurrence
 */
class UpdateScheduledMeetingOccurrenceUseCase @Inject constructor(
    private val callRepository: CallRepository,
) {
    /**
     * Invoke
     *
     * @param chatId MegaChatHandle that identifies a chat room
     * @param schedId MegaChatHandle that identifies the scheduled meeting
     * @param overrides start date time that along with schedId identifies the occurrence with the format (unix timestamp UTC)
     * @param newStartDate new start date time of the occurrence with the format (unix timestamp UTC)
     * @param newEndDate new end date time of the occurrence with the format (unix timestamp UTC)
     * @param cancelled True if scheduled meeting occurrence is going to be cancelled
     */
    suspend operator fun invoke(
        chatId: Long,
        schedId: Long,
        overrides: Long,
        newStartDate: Long,
        newEndDate: Long,
        cancelled: Boolean,
    ): ChatRequest = callRepository.updateScheduledMeetingOccurrence(
        chatId,
        schedId,
        overrides,
        newStartDate,
        newEndDate,
        cancelled,
    )
}