package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.chat.MeetingRoomItem

/**
 * Get chat meetings.
 */
fun interface GetMeetings {
    /**
     * Get chat meetings.
     * @return a flow list of chat meetings.
     */
    operator fun invoke(): Flow<List<MeetingRoomItem>>
}
