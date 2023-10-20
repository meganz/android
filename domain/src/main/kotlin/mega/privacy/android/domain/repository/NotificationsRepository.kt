package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.Event
import mega.privacy.android.domain.entity.UserAlert

/**
 * Notification repository.
 */
interface NotificationsRepository {

    /**
     * Monitor user alerts
     *
     * @return a flow of all global user alerts
     */
    fun monitorUserAlerts(): Flow<List<UserAlert>>

    /**
     * Monitor events
     *
     * @return a flow of global [Event]
     */
    fun monitorEvent(): Flow<Event>

    /**
     * Get user alerts
     *
     * @return list of current user alerts
     */
    suspend fun getUserAlerts(): List<UserAlert>

    /**
     * Acknowledge user alerts have been seen
     */
    suspend fun acknowledgeUserAlerts()

    /**
     * Monitor home badge count.
     *
     * @return Flow of the number of pending actions the current logged in account has.
     */
    fun monitorHomeBadgeCount(): Flow<Int>

    /**
     * Broadcast home badge count.
     *
     * @param badgeCount Number of pending actions the current logged in account has.
     */
    suspend fun broadcastHomeBadgeCount(badgeCount: Int)

    /**
     * Check if notifications are enabled for a chat
     *
     * @param chatId    handle of the node that identifies the chat room
     * @return          true if it is enabled, false otherwise
     */
    suspend fun isChatEnabled(chatId: Long): Boolean

    /**
     * Enable or disable notifications for a chat
     *
     * If notifications for this chat are disabled, the DND settings for this chat,
     * if any, will be cleared.
     *
     * @note Settings per chat override any global notification setting.
     *
     * @param chatId    handle of the node that identifies the chat room
     * @param enabled   true to enable, false to disable
     */
    suspend fun setChatEnabled(chatId: Long, enabled: Boolean)

    /**
     * Returns whether Do-Not-Disturb mode for a chat is enabled or not
     *
     * @param chatId - handle of the node that identifies the chat room
     * @return true if enabled, false otherwise
     */
    suspend fun isChatDoNotDisturbEnabled(chatId: Long): Boolean

}
