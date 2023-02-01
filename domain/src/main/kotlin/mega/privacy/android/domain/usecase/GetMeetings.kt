package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import mega.privacy.android.domain.entity.chat.MeetingRoomItem

/**
 * Get chat meetings.
 */
fun interface GetMeetings {
    /**
     * Get chat meetings.
     * @return a flow list of chat meetings.
     */
    operator fun invoke(mutex: Mutex): Flow<List<MeetingRoomItem>>
}
