package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.entity.chat.ChatScheduledMeeting

/**
 * Use case for monitoring updates on scheduled meeting
 */
fun interface MonitorScheduledMeetingUpdates {

    /**
     * Invoke.
     *
     * @return          Flow of [ChatRoom].
     */
    suspend operator fun invoke(): Flow<ChatScheduledMeeting>
}