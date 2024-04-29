package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.entity.chat.ChatScheduledMeetingOccurr
import mega.privacy.android.domain.repository.CallRepository
import javax.inject.Inject

/**
 * Use case for getting occurrences of a recurring meeting from a chat
 */
class FetchScheduledMeetingOccurrencesByChatUseCase @Inject constructor(
    private val callRepository: CallRepository,
) {

    /**
     * Invocation method.
     *
     * @param chatId                            Chat id.
     * @param since                             Timestamp from which API will generate more occurrences
     * @return [ChatScheduledMeetingOccurr]     List of occurrences.
     */
    suspend operator fun invoke(chatId: Long, since: Long): List<ChatScheduledMeetingOccurr> =
        callRepository.fetchScheduledMeetingOccurrencesByChat(chatId = chatId, since = since)
}
