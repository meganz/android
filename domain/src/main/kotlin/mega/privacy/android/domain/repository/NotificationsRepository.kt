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
     * Enable or disable notifications for a list of chats
     *
     * @param chatIdList list of chat IDs
     * @param enabled tru to enable, false to disable
     */
    suspend fun setChatEnabled(chatIdList: List<Long>, enabled: Boolean)

    /**
     * Set the DND mode for a list of chats for a period of time
     *
     * @param chatIdList list of chat IDs
     * @param timestamp Timestamp until DND mode is enabled (in seconds since the Epoch)
     */
    suspend fun setChatDoNotDisturb(chatIdList: List<Long>, timestamp: Long)

    /**
     * Returns whether Do-Not-Disturb mode for a chat is enabled or not
     *
     * @param chatId - handle of the node that identifies the chat room
     * @return true if enabled, false otherwise
     */
    suspend fun isChatDoNotDisturbEnabled(chatId: Long): Boolean

    /**
     * Set Do-Not-Disturb mode for a chat
     *
     * @param chatId handle of the node that identifies the chat room
     * @param timestamp timestamp until DND mode is enabled (in seconds since the Epoch)
     */
    suspend fun setChatDoNotDisturb(chatId: Long, timestamp: Long)

    /**
     * Enable or disable notifications for all chats
     *
     * @param enabled   true to enable, false to disable
     */
    suspend fun setChatsEnabled(enabled: Boolean)

    /**
     * Set Do-Not-Disturb mode for all chats
     *
     * @param timestamp timestamp until DND mode is enabled (in seconds since the Epoch)
     */
    suspend fun setChatsDoNotDisturb(timestamp: Long)

    /**
     *  Retrieve the push notification settings from the server and update the data in memory
     */
    suspend fun updatePushNotificationSettings()

    /**
     * Set launcher badge count
     *
     * @param count
     */
    suspend fun setLauncherBadgeCount(count: Int)
}
