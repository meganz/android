package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.entity.chat.ChatScheduledMeetingOccurr

/**
 * Use case for monitoring updates on scheduled meetings occurrences
 */
fun interface FetchNumberOfScheduledMeetingOccurrencesByChat {

    /**
     * Invoke.
     *
     * @param chatId                            Chat id.
     * @param count                             Number of occurrences.
     * @return [ChatScheduledMeetingOccurr]     List of occurrences.
     */
    suspend operator fun invoke(chatId: Long, count: Int): List<ChatScheduledMeetingOccurr>?
}