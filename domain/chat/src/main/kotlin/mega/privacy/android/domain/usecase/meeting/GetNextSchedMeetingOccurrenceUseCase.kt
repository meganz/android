package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.entity.chat.ChatScheduledMeetingOccurr
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * Get next scheduled meeting occurrence
 */
class GetNextSchedMeetingOccurrenceUseCase @Inject constructor(
    private val fetchScheduledMeetingOccurrencesByChatUseCase: FetchScheduledMeetingOccurrencesByChatUseCase,
) {

    /**
     * Invoke
     *
     * @param chatId    Chat id
     * @return          [ChatScheduledMeetingOccurr]
     */
    suspend operator fun invoke(chatId: Long): ChatScheduledMeetingOccurr? {
        val now = Instant.now()
        return fetchScheduledMeetingOccurrencesByChatUseCase(
            chatId = chatId,
            since = now.minus(1L, ChronoUnit.HALF_DAYS).epochSecond
        ).sortedBy(ChatScheduledMeetingOccurr::startDateTime)
            .firstOrNull { occurrence ->
                !occurrence.isCancelled
                        && (occurrence.startDateTime?.toInstant()?.isAfter(now) == true
                        || occurrence.endDateTime?.toInstant()?.isAfter(now) == true)
            }
    }

    private fun Long.toInstant(): Instant = Instant.ofEpochSecond(this)
}
