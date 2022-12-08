package mega.privacy.android.domain.repository

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
     * Get my user handle
     *
     * @return My user handle
     */
    suspend fun getMyUserHandle(): Long

    /**
     * Get my full name
     *
     * @return My full name
     */
    suspend fun getMyFullName(): String

    /**
     * Get my email
     *
     * @return My email
     */
    suspend fun getMyEmail(): String

    /**
     * Get my default Avatar Color
     *
     * @return My avatar color
     */
    suspend fun getMyDefaultAvatarColor(): String

    /**
     * Get my status
     *
     * @return My status
     */
    suspend fun getMyStatus(): UserStatus
}