package mega.privacy.android.data.test.state

import nz.mega.sdk.MegaChatApi
import nz.mega.sdk.MegaChatListItem
import nz.mega.sdk.MegaChatMessage
import nz.mega.sdk.MegaChatRoom
import nz.mega.sdk.MegaChatScheduledMeeting

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

    /** Whether the presence auto-away option is enabled. */
    var presenceAutoAwayEnabled: Boolean = false

    /** Auto-away timeout in seconds. */
    var presenceAutoAwayTimeout: Long = 0L

    /** Whether the online status persists after going offline. */
    var presencePersist: Boolean = false

    /** Whether the presence configuration is pending server confirmation. */
    var presencePending: Boolean = false

    /** Whether the last time online is visible to other users. */
    var lastGreenVisible: Boolean = false

    /** Handle of the logged-in user. */
    var myUserHandle: Long = 111L

    /** Full name of the logged-in user. */
    var myFullname: String = "Test User"

    /** Email of the logged-in user. */
    var myEmail: String = "test@mega.nz"

    /** Chat rooms by chat id; room lookups on the fake gateway resolve through this map. */
    val chatRooms: MutableMap<Long, MegaChatRoom> = mutableMapOf()

    /**
     * Chat list items by chat id; the chat-list feed on the fake gateway
     * (`getChatListItem` / `getChatListItems`) resolves through this map. A room must have a
     * matching list item here to appear in the chat list, because the repository combines the
     * [MegaChatRoom] with its [MegaChatListItem] to build each list row.
     */
    val chatListItems: MutableMap<Long, MegaChatListItem> = mutableMapOf()

    /**
     * Scheduled meetings by chat id; the scheduled-meeting reads on the fake gateway
     * (`getScheduledMeetingsByChat` / `getScheduledMeeting` / `getAllScheduledMeetings`) resolve
     * through this map. Seed a room's meeting here for the chat list Meetings tab to decorate it
     * (headers, upcoming / recurring labels). Occurrences are not modelled — the domain falls back
     * to the scheduled meeting's own start / end times.
     */
    val scheduledMeetings: MutableMap<Long, MutableList<MegaChatScheduledMeeting>> = mutableMapOf()

    /** Register [meeting] as a scheduled meeting of [chatId], creating the list if needed. */
    fun addScheduledMeeting(chatId: Long, meeting: MegaChatScheduledMeeting) {
        scheduledMeetings.getOrPut(chatId) { mutableListOf() }.add(meeting)
    }

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
        presenceAutoAwayEnabled = false
        presenceAutoAwayTimeout = 0L
        presencePersist = false
        presencePending = false
        lastGreenVisible = false
        myUserHandle = 111L
        myFullname = "Test User"
        myEmail = "test@mega.nz"
        chatRooms.clear()
        chatListItems.clear()
        scheduledMeetings.clear()
        chatMessages.clear()
    }
}
