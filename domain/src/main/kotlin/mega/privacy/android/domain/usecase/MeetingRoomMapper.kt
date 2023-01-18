package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.chat.CombinedChatRoom
import mega.privacy.android.domain.entity.chat.MeetingRoomItem
import kotlin.reflect.KSuspendFunction1

/**
 * Get meeting room item mapper
 */
fun interface MeetingRoomMapper {
    /**
     * Invoke
     *
     * @param chatRoom
     * @param isChatNotifiable
     * @param isChatLastMessageGeolocation
     * @return  MeetingRoomItem
     */
    suspend fun invoke(
        chatRoom: CombinedChatRoom,
        isChatNotifiable: KSuspendFunction1<Long, Boolean>,
        isChatLastMessageGeolocation: KSuspendFunction1<Long, Boolean>,
    ): MeetingRoomItem
}
