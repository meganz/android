package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.entity.chat.ChatScheduledMeeting

/**
 * Use case for getting the scheduled meeting from a chat
 */
fun interface GetScheduledMeeting {

    /**
     * Invoke.
     *
     * @param chatId                    Chat id.
     * @param schedId                   Sched id.
     * @return [ChatScheduledMeeting]   containing the updated data.
     */
    suspend operator fun invoke(chatId: Long, schedId: Long): ChatScheduledMeeting?
}
