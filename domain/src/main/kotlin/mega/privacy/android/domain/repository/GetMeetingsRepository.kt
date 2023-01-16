package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import mega.privacy.android.domain.entity.chat.MeetingRoomItem

/**
 * Get Meetings repository
 */
interface GetMeetingsRepository {

    /**
     * Update missing meeting item fields
     *
     * @param items
     * @param mutex
     * @return Flow of updating meeting room items
     */
    suspend fun getUpdatedMeetingItems(
        items: MutableList<MeetingRoomItem>,
        mutex: Mutex
    ): Flow<MutableList<MeetingRoomItem>>

    /**
     * Get updated meeting item
     *
     * @param item  Item to be updated
     * @return      Item with updated fields
     */
    suspend fun getUpdatedMeetingItem(item: MeetingRoomItem): MeetingRoomItem
}
