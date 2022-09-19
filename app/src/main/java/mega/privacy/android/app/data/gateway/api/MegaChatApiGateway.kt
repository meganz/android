package mega.privacy.android.app.data.gateway.api

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.app.data.model.ChatUpdate
import nz.mega.sdk.MegaChatLoggerInterface
import nz.mega.sdk.MegaChatPeerList
import nz.mega.sdk.MegaChatRequestListenerInterface
import nz.mega.sdk.MegaChatRoom

/**
 * Mega chat api gateway
 *
 * @constructor Create empty Mega chat api gateway
 */
interface MegaChatApiGateway {

    /**
     * Init state
     */
    val initState: Int

    /**
     * Initializes API.
     *
     * @param session   Account session.
     * @return Init state.
     */
    fun init(session: String): Int

    /**
     * Logouts API.
     */
    fun logout()

    /**
     * Set logger
     *
     * @param logger
     */
    fun setLogger(logger: MegaChatLoggerInterface)

    /**
     * Set log level
     *
     * @param logLevel
     */
    fun setLogLevel(logLevel: Int)

    /**
     * Add chat request listener
     *
     * @param listener
     */
    fun addChatRequestListener(listener: MegaChatRequestListenerInterface)

    /**
     * Remove chat request listener
     *
     * @param listener
     */
    fun removeChatRequestListener(listener: MegaChatRequestListenerInterface)

    /**
     * Notifies a push has been received.
     *
     * @param beep      True if should beep, false otherwise.
     * @param listener  Listener.
     */
    fun pushReceived(beep: Boolean, listener: MegaChatRequestListenerInterface?)

    /**
     * Refreshes DNS servers and retries pending connections.
     *
     * @param disconnect True if should disconnect, false otherwise.
     */
    fun retryPendingConnections(disconnect: Boolean)

    /**
     * Chat updates.
     */
    val chatUpdates: Flow<ChatUpdate>

    /**
     * Request the number of minutes since the user was seen as green by last time.
     *
     * @param userHandle User handle from who the last green has been requested.
     */
    suspend fun requestLastGreen(userHandle: Long)

    /**
     * Creates a chat for one or more participants, allowing you to specify their
     * permissions and if the chat should be a group chat or not.
     *
     * @param isGroup  True if is should create a group chat, false otherwise.
     * @param peers    MegaChatPeerList] including contacts and their privilege level.
     * @param listener Listener.
     */
    fun createChat(
        isGroup: Boolean,
        peers: MegaChatPeerList,
        listener: MegaChatRequestListenerInterface,
    )

    /**
     * Gets a 1to1 chat conversation if exists.
     *
     * @param userHandle The user handle.
     * @return The chat conversation.
     */
    fun getChatRoomByUser(userHandle: Long): MegaChatRoom?

    /**
     * Returns the known alias given to the user.
     * Returns NULL if data is not cached yet or it's not possible to get.
     *
     * @param userHandle Handle of the user whose alias is requested.
     * @return The user alias.
     */
    fun getUserAliasFromCache(userHandle: Long): String?

    /**
     *  Returns the current full name of the user.
     *  Returns NULL if data is not cached yet or it's not possible to get.
     *
     * @param userHandle Handle of the user whose full name is requested.
     * @return The user full name.
     */
    fun getUserFullNameFromCache(userHandle: Long): String?

    /**
     * Gets the online status of a user.
     *
     * @param userHandle Handle of user whose status is requested.
     * @return Online status of the user.
     */
    fun getUserOnlineStatus(userHandle: Long): Int
}