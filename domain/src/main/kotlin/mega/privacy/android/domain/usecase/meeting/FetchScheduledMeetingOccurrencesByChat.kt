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
     * @return [ChatScheduledMeetingOccurr]     List of occurrences.
     */
    suspend operator fun invoke(chatId: Long): List<ChatScheduledMeetingOccurr>?
}