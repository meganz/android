package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.entity.chat.ChatScheduledMeetingOccurr
import mega.privacy.android.domain.repository.CallRepository
import javax.inject.Inject

/**
 * Get next scheduled meeting occurrence
 */
class GetNextSchedMeetingOccurrenceUseCase @Inject constructor(
    private val callRepository: CallRepository,
) {

    /**
     * Invoke
     *
     * @param chatId    Chat id
     * @return          [ChatScheduledMeetingOccurr]
     */
    suspend operator fun invoke(chatId: Long): ChatScheduledMeetingOccurr? =
        callRepository.getNextScheduledMeetingOccurrence(chatId)
}
