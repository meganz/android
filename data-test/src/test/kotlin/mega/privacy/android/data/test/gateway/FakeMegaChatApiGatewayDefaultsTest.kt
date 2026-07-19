package mega.privacy.android.data.test.gateway

import com.google.common.truth.Truth.assertThat
import java.util.stream.Stream
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.test.stub.StubMegaHandleList
import nz.mega.sdk.MegaChatApi
import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaChatLoggerInterface
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

/**
 * Documents the out-of-the-box defaults of every unstubbed [FakeMegaChatApiGateway] method that
 * is not backed by [mega.privacy.android.data.test.state.FakeChatState].
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FakeMegaChatApiGatewayDefaultsTest {

    private val unknownHandle = 999_999L

    private fun case(
        name: String,
        block: suspend (FakeMegaChatApiGateway) -> Any?,
    ): Arguments = Arguments.of(name, block)

    private fun valueCase(
        name: String,
        expected: Any?,
        block: suspend (FakeMegaChatApiGateway) -> Any?,
    ): Arguments = Arguments.of(name, expected, block)

    fun nullReturningMethods(): Stream<Arguments> = Stream.of(
        case("getNoteToSelfChat") { it.getNoteToSelfChat() },
        case("getChatRoom") { it.getChatRoom(unknownHandle) },
        case("getChatRoomByUser") { it.getChatRoomByUser(unknownHandle) },
        case("getUserAliasFromCache") { it.getUserAliasFromCache(unknownHandle) },
        case("getUserFirstnameFromCache") { it.getUserFirstnameFromCache(unknownHandle) },
        case("getUserLastnameFromCache") { it.getUserLastnameFromCache(unknownHandle) },
        case("getUserEmailFromCache") { it.getUserEmailFromCache(unknownHandle) },
        case("getUserFullNameFromCache") { it.getUserFullNameFromCache(unknownHandle) },
        case("getChatListItem") { it.getChatListItem(1L) },
        case("getMessage") { it.getMessage(1L, 2L) },
        case("getMessageFromNodeHistory") { it.getMessageFromNodeHistory(1L, 2L) },
        case("getChatCall") { it.getChatCall(1L) },
        case("getChatCallByCallId") { it.getChatCallByCallId(1L) },
        case("getScheduledMeeting") { it.getScheduledMeeting(1L, 2L) },
        case("getChatPresenceConfig") { it.getChatPresenceConfig() },
    )

    @ParameterizedTest(name = "{0}")
    @MethodSource("nullReturningMethods")
    fun `test that method returns null when unstubbed`(
        name: String,
        block: suspend (FakeMegaChatApiGateway) -> Any?,
    ) = runTest {
        assertThat(block(FakeMegaChatApiGateway())).isNull()
    }

    fun valueDefaultMethods(): Stream<Arguments> = Stream.of(
        valueCase("initAnonymous", MegaChatApi.INIT_ANONYMOUS) { it.initAnonymous() },
        valueCase("getChatInvalidHandle", MegaChatApiJava.MEGACHAT_INVALID_HANDLE) {
            it.getChatInvalidHandle()
        },
        valueCase("getConnectedState", MegaChatApi.CONNECTED) { it.getConnectedState() },
        valueCase("getChatConnectionState", MegaChatApi.CHAT_CONNECTION_ONLINE) {
            it.getChatConnectionState(1L)
        },
        valueCase("getNumUnreadChats", 0) { it.getNumUnreadChats() },
        valueCase("loadMessages", MegaChatApi.SOURCE_NONE) { it.loadMessages(1L, 32) },
        valueCase("hasUrl", false) { it.hasUrl("no url here") },
        valueCase("hasCallInChatRoom", false) { it.hasCallInChatRoom(1L) },
        valueCase("isAudioLevelMonitorEnabled", false) { it.isAudioLevelMonitorEnabled(1L) },
        valueCase("setMessageSeen", true) { it.setMessageSeen(1L, 2L) },
        valueCase("setIgnoredCall", true) { it.setIgnoredCall(1L) },
        valueCase("getLastMessageSeenId", MegaChatApiJava.MEGACHAT_INVALID_HANDLE) {
            it.getLastMessageSeenId(1L)
        },
        valueCase("getMessageReactionCount", 0) { it.getMessageReactionCount(1L, 2L, "+1") },
        valueCase("getUserOnlineStatus", MegaChatApi.STATUS_OFFLINE) {
            it.getUserOnlineStatus(unknownHandle)
        },
    )

    @ParameterizedTest(name = "{0}")
    @MethodSource("valueDefaultMethods")
    fun `test that method returns documented default value when unstubbed`(
        name: String,
        expected: Any?,
        block: suspend (FakeMegaChatApiGateway) -> Any?,
    ) = runTest {
        assertThat(block(FakeMegaChatApiGateway())).isEqualTo(expected)
    }

    fun emptyCollectionMethods(): Stream<Arguments> = Stream.of(
        case("getChatRooms") { it.getChatRooms().size },
        case("getMeetingChatRooms") { it.getMeetingChatRooms()?.size },
        case("getGroupChatRooms") { it.getGroupChatRooms()?.size },
        case("getIndividualChatRooms") { it.getIndividualChatRooms()?.size },
        case("getChatListItems") { it.getChatListItems(0, 0)?.size },
        case("getAllScheduledMeetings") { it.getAllScheduledMeetings()?.size },
        case("getScheduledMeetingsByChat") { it.getScheduledMeetingsByChat(1L)?.size },
        case("getMessageReactions") { it.getMessageReactions(1L, 2L).size() },
        case("getReactionUsers") { it.getReactionUsers(1L, 2L, "+1").size().toInt() },
        case("getChatCalls") { it.getChatCalls(-1)?.size()?.toInt() },
        case("getChatCallIds") { it.getChatCallIds()?.size()?.toInt() },
    )

    @ParameterizedTest(name = "{0}")
    @MethodSource("emptyCollectionMethods")
    fun `test that method returns empty collection when unstubbed`(
        name: String,
        block: suspend (FakeMegaChatApiGateway) -> Any?,
    ) = runTest {
        assertThat(block(FakeMegaChatApiGateway())).isEqualTo(0)
    }

    fun unitCommandMethods(): Stream<Arguments> = Stream.of(
        case("requestLastGreen") { it.requestLastGreen(1L) },
        case("refreshUrl") { it.refreshUrl() },
        case("closeChatPreview") { it.closeChatPreview(1L) },
        case("setSFUid") { it.setSFUid(1) },
        case("removeFailedMessage") { it.removeFailedMessage(1L, 2L) },
        case("setUserTyping") { it.setUserTyping(1L) },
        case("setUserStoppedTyping") { it.setUserStoppedTyping(1L) },
        case("enableAudioLevelMonitor") { it.enableAudioLevelMonitor(true, 1L) },
        case("setLogLevel") { it.setLogLevel(5) },
        case("setLogger") {
            it.setLogger(object : MegaChatLoggerInterface {
                override fun log(loglevel: Int, message: String?) = Unit
            })
        },
    )

    @ParameterizedTest(name = "{0}")
    @MethodSource("unitCommandMethods")
    fun `test that unit command records its invocation when unstubbed`(
        name: String,
        block: suspend (FakeMegaChatApiGateway) -> Any?,
    ) = runTest {
        val underTest = FakeMegaChatApiGateway()

        block(underTest)

        assertThat(underTest.invocations.single().methodName).isEqualTo(name)
    }

    fun messageFactoryMethods(): Stream<Arguments> = Stream.of(
        case("sendMessage") { it.sendMessage(1L, "hello") },
        case("sendGeolocation") { it.sendGeolocation(1L, 1f, 2f, "image") },
        case("sendGiphy") { it.sendGiphy(1L, null, null, 0L, 0L, 0, 0, null) },
        case("attachContacts") { it.attachContacts(1L, StubMegaHandleList()) },
        case("forwardContact") { it.forwardContact(1L, 2L, 3L) },
        case("deleteMessage") { it.deleteMessage(1L, 2L) },
        case("revokeAttachmentMessage") { it.revokeAttachmentMessage(1L, 2L) },
        case("editMessage") { it.editMessage(1L, 2L, "edited") },
        case("editGeolocation") { it.editGeolocation(1L, 2L, 1f, 2f, "image") },
    )

    @ParameterizedTest(name = "{0}")
    @MethodSource("messageFactoryMethods")
    fun `test that message factory returns stub message when unstubbed`(
        name: String,
        block: suspend (FakeMegaChatApiGateway) -> Any?,
    ) = runTest {
        assertThat(block(FakeMegaChatApiGateway())).isNotNull()
    }
}
