package mega.privacy.android.domain.repository

import mega.privacy.android.domain.entity.GroupChatPeer

/**
 * Get Meetings repository
 */
interface GetMeetingsRepository {

    /**
     * Get peers from a chat group
     *
     * @param chatId    Chat Id
     * @return          List of chat peers within that group
     */
    suspend fun getGroupChatPeers(chatId: Long): List<GroupChatPeer>
}
