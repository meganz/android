package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.entity.chat.ChatScheduledMeetingOccurr

/**
 * Use case for getting occurrences of a recurring meeting from a chat
 */
fun interface FetchScheduledMeetingOccurrencesByChat {

    /**
     * Invoke.
     *
     * @param chatId                            Chat id.
     * @param since                             Timestamp from which API will generate more occurrences
     * @return [ChatScheduledMeetingOccurr]     List of occurrences.
     */
    suspend operator fun invoke(chatId: Long, since: Long): List<ChatScheduledMeetingOccurr>?
}