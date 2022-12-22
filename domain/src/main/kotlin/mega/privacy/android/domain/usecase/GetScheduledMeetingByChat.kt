package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.chat.ChatScheduledMeeting

/**
 * Use case for getting the scheduled meeting from a chat
 */
fun interface GetScheduledMeetingByChat {

    /**
     * Invoke.
     *
     * @param chatId                    Chat id.
     * @return [ChatScheduledMeeting]   containing the updated data.
     */
    suspend operator fun invoke(chatId: Long): List<ChatScheduledMeeting>?
}