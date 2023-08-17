package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.entity.chat.ChatScheduledMeetingOccurr
import mega.privacy.android.domain.repository.CallRepository
import java.time.Instant
import java.time.temporal.ChronoUnit
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
    suspend operator fun invoke(chatId: Long): ChatScheduledMeetingOccurr? {
        val now = Instant.now()
        return callRepository.fetchScheduledMeetingOccurrencesByChat(
            chatId,
            now.minus(1L, ChronoUnit.HALF_DAYS).epochSecond
        ).sortedBy(ChatScheduledMeetingOccurr::startDateTime)
            .firstOrNull { occurrence ->
                !occurrence.isCancelled
                        && (occurrence.startDateTime?.toInstant()?.isAfter(now) == true
                        || occurrence.endDateTime?.toInstant()?.isAfter(now) == true)
            }
    }

    private fun Long.toInstant(): Instant =
        Instant.ofEpochSecond(this)
}
