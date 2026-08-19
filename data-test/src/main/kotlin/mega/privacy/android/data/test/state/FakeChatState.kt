package mega.privacy.android.data.test.state

import nz.mega.sdk.MegaChatApi
import nz.mega.sdk.MegaChatMessage
import nz.mega.sdk.MegaChatRoom

/**
 * Mutable chat-side defaults backing a fake [mega.privacy.android.data.gateway.api.MegaChatApiGateway].
 *
 * Tests mutate the fields directly, register chat rooms by chat id in [chatRooms], and seed a
 * room's message history in [chatMessages]. Defaults describe an online chat session for the same
 * user as [FakeAccountState].
 */
class FakeChatState {

    /** Chat initialisation state, as returned by MegaChatApi.init. */
    var initState: Int = MegaChatApi.INIT_ONLINE_SESSION

    /** Own online status. */
    var onlineStatus: Int = MegaChatApi.STATUS_ONLINE

    /** Handle of the logged-in user. */
    var myUserHandle: Long = 111L

    /** Full name of the logged-in user. */
    var myFullname: String = "Test User"

    /** Email of the logged-in user. */
    var myEmail: String = "test@mega.nz"

    /** Chat rooms by chat id; room lookups on the fake gateway resolve through this map. */
    val chatRooms: MutableMap<Long, MegaChatRoom> = mutableMapOf()

    /**
     * Message history by chat id, ordered oldest first. `loadMessages` on the fake gateway
     * delivers these newest to oldest, as the SDK does; give each message a unique `msgId` and
     * distinct timestamps so the app's message store keeps them in order.
     */
    val chatMessages: MutableMap<Long, MutableList<MegaChatMessage>> = mutableMapOf()

    /** Append [message] to the history of [chatId], creating the history if needed. */
    fun addChatMessage(chatId: Long, message: MegaChatMessage) {
        chatMessages.getOrPut(chatId) { mutableListOf() }.add(message)
    }

    /** Restore every field to its default value and remove all chat rooms and messages. */
    fun reset() {
        initState = MegaChatApi.INIT_ONLINE_SESSION
        onlineStatus = MegaChatApi.STATUS_ONLINE
        myUserHandle = 111L
        myFullname = "Test User"
        myEmail = "test@mega.nz"
        chatRooms.clear()
        chatMessages.clear()
    }
}
