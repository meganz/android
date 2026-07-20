package mega.privacy.android.data.test.gateway

import com.google.common.truth.Truth.assertThat
import java.util.stream.Stream
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.test.stub.StubMegaChatError
import mega.privacy.android.data.test.stub.StubMegaChatPeerList
import mega.privacy.android.data.test.stub.StubMegaChatRequest
import mega.privacy.android.data.test.stub.StubMegaHandleList
import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaChatError
import nz.mega.sdk.MegaChatRequest
import nz.mega.sdk.MegaChatRequestListenerInterface
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

/**
 * Documents the listener-completion behaviour of every listener-based [FakeMegaChatApiGateway]
 * method: recorded invocation, default success completion carrying the method's request type, and
 * outcome stubbing via [FakeMegaChatApiGateway.stubChatRequest].
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FakeMegaChatApiGatewayRequestListenerTest {

    private class RecordingChatRequestListener : MegaChatRequestListenerInterface {
        val startedRequests = mutableListOf<MegaChatRequest>()
        val finishedRequests = mutableListOf<Pair<MegaChatRequest, MegaChatError>>()

        override fun onRequestStart(api: MegaChatApiJava?, request: MegaChatRequest?) {
            startedRequests.add(requireNotNull(request))
        }

        override fun onRequestUpdate(api: MegaChatApiJava?, request: MegaChatRequest?) = Unit

        override fun onRequestFinish(
            api: MegaChatApiJava?,
            request: MegaChatRequest?,
            e: MegaChatError?,
        ) {
            finishedRequests.add(requireNotNull(request) to requireNotNull(e))
        }

        override fun onRequestTemporaryError(
            api: MegaChatApiJava?,
            request: MegaChatRequest?,
            e: MegaChatError?,
        ) = Unit
    }

    private fun listenerCase(
        name: String,
        expectedType: Int,
        block: (FakeMegaChatApiGateway, MegaChatRequestListenerInterface) -> Unit,
    ): Arguments = Arguments.of(name, expectedType, block)

    fun listenerMethods(): Stream<Arguments> = Stream.of(
        listenerCase("logout", MegaChatRequest.TYPE_LOGOUT) { g, l -> g.logout(l) },
        listenerCase("pushReceived", MegaChatRequest.TYPE_PUSH_RECEIVED) { g, l ->
            g.pushReceived(true, l)
        },
        listenerCase(
            "retryPendingConnections",
            MegaChatRequest.TYPE_RETRY_PENDING_CONNECTIONS,
        ) { g, l -> g.retryPendingConnections(false, l) },
        listenerCase("createChat", MegaChatRequest.TYPE_CREATE_CHATROOM) { g, l ->
            g.createChat(true, StubMegaChatPeerList(), l)
        },
        listenerCase("createGroupChat", MegaChatRequest.TYPE_CREATE_CHATROOM) { g, l ->
            g.createGroupChat(StubMegaChatPeerList(), "title", false, false, false, l)
        },
        listenerCase("createPublicChat", MegaChatRequest.TYPE_CREATE_CHATROOM) { g, l ->
            g.createPublicChat(StubMegaChatPeerList(), "title", false, false, false, l)
        },
        listenerCase("leaveChat", MegaChatRequest.TYPE_REMOVE_FROM_CHATROOM) { g, l ->
            g.leaveChat(1L, l)
        },
        listenerCase("setChatTitle", MegaChatRequest.TYPE_EDIT_CHATROOM_NAME) { g, l ->
            g.setChatTitle(1L, "title", l)
        },
        listenerCase("setOpenInvite", MegaChatRequest.TYPE_SET_CHATROOM_OPTIONS) { g, l ->
            g.setOpenInvite(1L, true, l)
        },
        listenerCase("setWaitingRoom", MegaChatRequest.TYPE_SET_CHATROOM_OPTIONS) { g, l ->
            g.setWaitingRoom(1L, true, l)
        },
        listenerCase("loadUserAttributes", MegaChatRequest.TYPE_GET_PEER_ATTRIBUTES) { g, l ->
            g.loadUserAttributes(1L, StubMegaHandleList(), l)
        },
        listenerCase("startChatCall", MegaChatRequest.TYPE_START_CHAT_CALL) { g, l ->
            g.startChatCall(1L, true, true, l)
        },
        listenerCase("startChatCallNoRinging", MegaChatRequest.TYPE_START_CHAT_CALL) { g, l ->
            g.startChatCallNoRinging(1L, 2L, true, true, l)
        },
        listenerCase(
            "startMeetingInWaitingRoomChat",
            MegaChatRequest.TYPE_START_CHAT_CALL,
        ) { g, l -> g.startMeetingInWaitingRoomChat(1L, 2L, true, true, l) },
        listenerCase(
            "ringIndividualInACall",
            MegaChatRequest.TYPE_RING_INDIVIDUAL_IN_CALL,
        ) { g, l -> g.ringIndividualInACall(1L, 2L, 30, l) },
        listenerCase("answerChatCall", MegaChatRequest.TYPE_ANSWER_CHAT_CALL) { g, l ->
            g.answerChatCall(1L, true, true, l)
        },
        listenerCase("hangChatCall", MegaChatRequest.TYPE_HANG_CHAT_CALL) { g, l ->
            g.hangChatCall(1L, l)
        },
        listenerCase("holdChatCall", MegaChatRequest.TYPE_SET_CALL_ON_HOLD) { g, l ->
            g.holdChatCall(1L, true, l)
        },
        listenerCase("setChatVideoInDevice", MegaChatRequest.TYPE_CHANGE_VIDEO_STREAM) { g, l ->
            g.setChatVideoInDevice("device", l)
        },
        listenerCase(
            "fetchScheduledMeetingOccurrencesByChat",
            MegaChatRequest.TYPE_FETCH_SCHEDULED_MEETING_OCCURRENCES,
        ) { g, l -> g.fetchScheduledMeetingOccurrencesByChat(1L, 0L, l) },
        listenerCase("inviteToChat", MegaChatRequest.TYPE_INVITE_TO_CHATROOM) { g, l ->
            g.inviteToChat(1L, 2L, l)
        },
        listenerCase("openChatPreview", MegaChatRequest.TYPE_LOAD_PREVIEW) { g, l ->
            g.openChatPreview("link", l)
        },
        listenerCase("checkChatLink", MegaChatRequest.TYPE_LOAD_PREVIEW) { g, l ->
            g.checkChatLink("link", l)
        },
        listenerCase("setPublicChatToPrivate", MegaChatRequest.TYPE_SET_PRIVATE_MODE) { g, l ->
            g.setPublicChatToPrivate(1L, l)
        },
        listenerCase("queryChatLink", MegaChatRequest.TYPE_CHAT_LINK_HANDLE) { g, l ->
            g.queryChatLink(1L, l)
        },
        listenerCase("removeChatLink", MegaChatRequest.TYPE_CHAT_LINK_HANDLE) { g, l ->
            g.removeChatLink(1L, l)
        },
        listenerCase("createChatLink", MegaChatRequest.TYPE_CHAT_LINK_HANDLE) { g, l ->
            g.createChatLink(1L, l)
        },
        listenerCase("autojoinPublicChat", MegaChatRequest.TYPE_AUTOJOIN_PUBLIC_CHAT) { g, l ->
            g.autojoinPublicChat(1L, l)
        },
        listenerCase("autorejoinPublicChat", MegaChatRequest.TYPE_AUTOJOIN_PUBLIC_CHAT) { g, l ->
            g.autorejoinPublicChat(1L, 2L, l)
        },
        listenerCase("removeFromChat", MegaChatRequest.TYPE_REMOVE_FROM_CHATROOM) { g, l ->
            g.removeFromChat(1L, 2L, l)
        },
        listenerCase(
            "updateChatPermissions",
            MegaChatRequest.TYPE_UPDATE_PEER_PERMISSIONS,
        ) { g, l -> g.updateChatPermissions(1L, 2L, 3, l) },
        listenerCase("signalPresenceActivity", MegaChatRequest.TYPE_SIGNAL_ACTIVITY) { g, l ->
            g.signalPresenceActivity(l)
        },
        listenerCase("clearChatHistory", MegaChatRequest.TYPE_TRUNCATE_HISTORY) { g, l ->
            g.clearChatHistory(1L, l)
        },
        listenerCase("archiveChat", MegaChatRequest.TYPE_ARCHIVE_CHATROOM) { g, l ->
            g.archiveChat(1L, true, l)
        },
        listenerCase(
            "createChatroomAndSchedMeeting",
            MegaChatRequest.TYPE_CREATE_SCHEDULED_MEETING,
        ) { g, l ->
            g.createChatroomAndSchedMeeting(
                StubMegaChatPeerList(), true, true, "title", false, false, false,
                "UTC", 0L, 1L, "description", null, null, null, l,
            )
        },
        listenerCase(
            "updateScheduledMeeting",
            MegaChatRequest.TYPE_UPDATE_SCHEDULED_MEETING,
        ) { g, l ->
            g.updateScheduledMeeting(
                1L, 2L, "UTC", 0L, 1L, "title", "description", false, null, null, true, l,
            )
        },
        listenerCase(
            "updateScheduledMeetingOccurrence",
            MegaChatRequest.TYPE_UPDATE_SCHEDULED_MEETING_OCCURRENCE,
        ) { g, l -> g.updateScheduledMeetingOccurrence(1L, 2L, 0L, 1L, 2L, false, l) },
        listenerCase("setOnlineStatus", MegaChatRequest.TYPE_SET_ONLINE_STATUS) { g, l ->
            g.setOnlineStatus(3, l)
        },
        listenerCase("openVideoDevice", MegaChatRequest.TYPE_OPEN_VIDEO_DEVICE) { g, l ->
            g.openVideoDevice(l)
        },
        listenerCase("releaseVideoDevice", MegaChatRequest.TYPE_OPEN_VIDEO_DEVICE) { g, l ->
            g.releaseVideoDevice(l)
        },
        listenerCase("enableVideo", MegaChatRequest.TYPE_DISABLE_AUDIO_VIDEO_CALL) { g, l ->
            g.enableVideo(1L, l)
        },
        listenerCase("disableVideo", MegaChatRequest.TYPE_DISABLE_AUDIO_VIDEO_CALL) { g, l ->
            g.disableVideo(1L, l)
        },
        listenerCase("enableAudio", MegaChatRequest.TYPE_DISABLE_AUDIO_VIDEO_CALL) { g, l ->
            g.enableAudio(1L, l)
        },
        listenerCase("disableAudio", MegaChatRequest.TYPE_DISABLE_AUDIO_VIDEO_CALL) { g, l ->
            g.disableAudio(1L, l)
        },
        listenerCase("pushUsersIntoWaitingRoom", MegaChatRequest.TYPE_WR_PUSH) { g, l ->
            g.pushUsersIntoWaitingRoom(1L, StubMegaHandleList(), false, l)
        },
        listenerCase("kickUsersFromCall", MegaChatRequest.TYPE_WR_KICK) { g, l ->
            g.kickUsersFromCall(1L, StubMegaHandleList(), l)
        },
        listenerCase("allowUsersJoinCall", MegaChatRequest.TYPE_WR_ALLOW) { g, l ->
            g.allowUsersJoinCall(1L, StubMegaHandleList(), false, l)
        },
        listenerCase("attachNode", MegaChatRequest.TYPE_ATTACH_NODE_MESSAGE) { g, l ->
            g.attachNode(1L, 2L, l)
        },
        listenerCase("attachVoiceMessage", MegaChatRequest.TYPE_ATTACH_NODE_MESSAGE) { g, l ->
            g.attachVoiceMessage(1L, 2L, l)
        },
        listenerCase("raiseHandToSpeak", MegaChatRequest.TYPE_RAISE_HAND_TO_SPEAK) { g, l ->
            g.raiseHandToSpeak(1L, l)
        },
        listenerCase("lowerHandToStopSpeak", MegaChatRequest.TYPE_RAISE_HAND_TO_SPEAK) { g, l ->
            g.lowerHandToStopSpeak(1L, l)
        },
        listenerCase("requestHiResVideo", MegaChatRequest.TYPE_REQUEST_HIGH_RES_VIDEO) { g, l ->
            g.requestHiResVideo(1L, 2L, l)
        },
        listenerCase("stopHiResVideo", MegaChatRequest.TYPE_REQUEST_HIGH_RES_VIDEO) { g, l ->
            g.stopHiResVideo(1L, StubMegaHandleList(), l)
        },
        listenerCase("requestLowResVideo", MegaChatRequest.TYPE_REQUEST_LOW_RES_VIDEO) { g, l ->
            g.requestLowResVideo(1L, StubMegaHandleList(), l)
        },
        listenerCase("stopLowResVideo", MegaChatRequest.TYPE_REQUEST_LOW_RES_VIDEO) { g, l ->
            g.stopLowResVideo(1L, StubMegaHandleList(), l)
        },
        listenerCase("endChatCall", MegaChatRequest.TYPE_HANG_CHAT_CALL) { g, l ->
            g.endChatCall(1L, l)
        },
        listenerCase("mutePeers", MegaChatRequest.TYPE_MUTE) { g, l -> g.mutePeers(1L, 2L, l) },
        listenerCase("addReaction", MegaChatRequest.TYPE_MANAGE_REACTION) { g, l ->
            g.addReaction(1L, 2L, "+1", l)
        },
        listenerCase("delReaction", MegaChatRequest.TYPE_MANAGE_REACTION) { g, l ->
            g.delReaction(1L, 2L, "+1", l)
        },
        listenerCase("setLimitsInCall", MegaChatRequest.TYPE_SET_LIMIT_CALL) { g, l ->
            g.setLimitsInCall(1L, null, null, null, null, null, l)
        },
        listenerCase("setChatRetentionTime", MegaChatRequest.TYPE_SET_RETENTION_TIME) { g, l ->
            g.setChatRetentionTime(1L, 60L, l)
        },
        listenerCase("createMeeting", MegaChatRequest.TYPE_CREATE_CHATROOM) { g, l ->
            g.createMeeting("title", false, false, false, l)
        },
    )

    @ParameterizedTest(name = "{0}")
    @MethodSource("listenerMethods")
    fun `test that listener method records and completes with success and request type when unstubbed`(
        name: String,
        expectedType: Int,
        block: (FakeMegaChatApiGateway, MegaChatRequestListenerInterface) -> Unit,
    ) {
        val underTest = FakeMegaChatApiGateway()
        val listener = RecordingChatRequestListener()

        block(underTest, listener)

        assertThat(underTest.invocations.single().methodName).isEqualTo(name)
        assertThat(listener.startedRequests.single().type).isEqualTo(expectedType)
        val (request, error) = listener.finishedRequests.single()
        assertThat(request.type).isEqualTo(expectedType)
        assertThat(error.errorCode).isEqualTo(MegaChatError.ERROR_OK)
    }

    @Test
    fun `test that listener receives stubbed error when stubChatRequest stubs a failure`() {
        val underTest = FakeMegaChatApiGateway()
        val listener = RecordingChatRequestListener()
        underTest.stubChatRequest(
            MegaChatApiGateway::archiveChat,
            error = StubMegaChatError(MegaChatError.ERROR_ACCESS),
        )

        underTest.archiveChat(1L, true, listener)

        val (request, error) = listener.finishedRequests.single()
        assertThat(request.type).isEqualTo(MegaChatRequest.TYPE_ARCHIVE_CHATROOM)
        assertThat(error.errorCode).isEqualTo(MegaChatError.ERROR_ACCESS)
    }

    @Test
    fun `test that listener receives stubbed request when stubChatRequest provides one`() {
        val underTest = FakeMegaChatApiGateway()
        val listener = RecordingChatRequestListener()
        val stubbedRequest = StubMegaChatRequest(type = MegaChatRequest.TYPE_DELETE)
        underTest.stubChatRequest(MegaChatApiGateway::logout, request = stubbedRequest)

        underTest.logout(listener)

        assertThat(listener.startedRequests.single()).isSameInstanceAs(stubbedRequest)
        assertThat(listener.finishedRequests.single().first).isSameInstanceAs(stubbedRequest)
    }

    @Test
    fun `test that only matching call fails when stubChatRequest uses an argument matcher`() {
        val underTest = FakeMegaChatApiGateway()
        val failingListener = RecordingChatRequestListener()
        val succeedingListener = RecordingChatRequestListener()
        underTest.stubChatRequest(
            MegaChatApiGateway::archiveChat,
            error = StubMegaChatError(MegaChatError.ERROR_NOENT),
            matcher = { it[0] == 7L },
        )

        underTest.archiveChat(7L, true, failingListener)
        underTest.archiveChat(8L, true, succeedingListener)

        assertThat(failingListener.finishedRequests.single().second.errorCode)
            .isEqualTo(MegaChatError.ERROR_NOENT)
        assertThat(succeedingListener.finishedRequests.single().second.errorCode)
            .isEqualTo(MegaChatError.ERROR_OK)
    }

    @Test
    fun `test that invocation is recorded without crash when listener is null`() {
        val underTest = FakeMegaChatApiGateway()

        underTest.logout(null)
        underTest.pushReceived(true, null)
        underTest.inviteToChat(1L, 2L, null)

        assertThat(underTest.invocations.map { it.methodName })
            .containsExactly("logout", "pushReceived", "inviteToChat")
            .inOrder()
    }

    @Test
    fun `test that recorded arguments include the listener when a listener method is called`() {
        val underTest = FakeMegaChatApiGateway()
        val listener = RecordingChatRequestListener()

        underTest.archiveChat(7L, true, listener)

        val invocation = underTest.invocationsOf(MegaChatApiGateway::archiveChat).single()
        assertThat(invocation.arguments).containsExactly(7L, true, listener).inOrder()
    }

    @Test
    fun `test that chat request listeners collection tracks add and remove when called`() {
        val underTest = FakeMegaChatApiGateway()
        val listener = RecordingChatRequestListener()

        underTest.addChatRequestListener(listener)

        assertThat(underTest.chatRequestListeners).containsExactly(listener)

        underTest.removeChatRequestListener(listener)

        assertThat(underTest.chatRequestListeners).isEmpty()
    }
}
