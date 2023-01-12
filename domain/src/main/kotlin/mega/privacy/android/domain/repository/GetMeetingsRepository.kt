package mega.privacy.android.domain.repository

import mega.privacy.android.domain.entity.chat.MeetingRoomItem

/**
 * Get Meetings repository
 */
interface GetMeetingsRepository {

    /**
     * Update missing meeting item fields
     *
     * @param items Mutable list of MeetingRoomItem
     */
    suspend fun updateMeetingFields(items: MutableList<MeetingRoomItem>)
}
