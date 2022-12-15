package mega.privacy.android.domain.repository

import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.ChatParticipant
import mega.privacy.android.domain.entity.contacts.UserStatus

/**
 * The repository interface regarding Chat participants.
 */
interface ChatParticipantsRepository {

    /**
     * Get chat participants
     */
    suspend fun getAllChatParticipants(chatId: Long): List<ChatParticipant>

    /**
     * Update list
     *
     * @param chatId        Chat Id
     * @param currentList          [ChatParticipant]
     * @return List of participant updated permissions
     */
    suspend fun updateList(chatId: Long, currentList: List<ChatParticipant>): MutableList<ChatParticipant>

    /**
     * Get status
     *
     * @param participant [ChatParticipant]
     * @return [UserStatus]
     */
    suspend fun getStatus(participant: ChatParticipant): UserStatus

    /**
     * Get alias
     *
     * @param participant [ChatParticipant]
     * @return Participant alias
     */
    suspend fun getAlias(participant: ChatParticipant): String?

    /**
     * Check credentials
     *
     * @param participant [ChatParticipant]
     * @return  True, if credentials are verified. False, if not.
     */
    suspend fun areCredentialsVerified(participant: ChatParticipant): Boolean

    /**
     * Get avatar
     *
     * @param participant [ChatParticipant]
     * @return Participant avatar
     */
    suspend fun getAvatarUri(participant: ChatParticipant): String?

    /**
     * Get permission
     *
     * @param chatId        Chat Id
     * @param participant   [ChatParticipant]
     * @return Participant permissions
     */
    suspend fun getPermissions(chatId: Long, participant: ChatParticipant): ChatRoomPermission
}