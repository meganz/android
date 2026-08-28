package mega.privacy.android.data.test.gateway

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.listener.OptionalMegaChatRequestListenerInterface
import mega.privacy.android.data.test.state.FakeChatState
import mega.privacy.android.data.test.stub.StubMegaChatRoom
import nz.mega.sdk.MegaChatApi
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Documents the [FakeChatState]-backed defaults of [FakeMegaChatApiGateway]: mutating the state
 * object changes what the unstubbed gateway returns.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FakeMegaChatApiGatewayStateTest {

    private lateinit var underTest: FakeMegaChatApiGateway

    @BeforeEach
    fun setUp() {
        underTest = FakeMegaChatApiGateway()
    }

    @Test
    fun `test that initState property reflects chat state when mutated`() {
        underTest.chatState.initState = MegaChatApi.INIT_OFFLINE_SESSION

        assertThat(underTest.initState).isEqualTo(MegaChatApi.INIT_OFFLINE_SESSION)
    }

    @Test
    fun `test that init returns chat state init state when unstubbed`() {
        underTest.chatState.initState = MegaChatApi.INIT_WAITING_NEW_SESSION

        assertThat(underTest.init("session")).isEqualTo(MegaChatApi.INIT_WAITING_NEW_SESSION)
    }

    @Test
    fun `test that getMyUserHandle returns chat state handle when mutated`() {
        underTest.chatState.myUserHandle = 4242L

        assertThat(underTest.getMyUserHandle()).isEqualTo(4242L)
    }

    @Test
    fun `test that getMyFullname returns chat state fullname when mutated`() {
        underTest.chatState.myFullname = "Jane Doe"

        assertThat(underTest.getMyFullname()).isEqualTo("Jane Doe")
    }

    @Test
    fun `test that getMyEmail returns chat state email when mutated`() {
        underTest.chatState.myEmail = "jane@mega.nz"

        assertThat(underTest.getMyEmail()).isEqualTo("jane@mega.nz")
    }

    @Test
    fun `test that getOnlineStatus returns chat state status when mutated`() {
        underTest.chatState.onlineStatus = MegaChatApi.STATUS_BUSY

        assertThat(underTest.getOnlineStatus()).isEqualTo(MegaChatApi.STATUS_BUSY)
    }

    @Test
    fun `test that getUserOnlineStatus returns chat state status when queried for own handle`() {
        underTest.chatState.myUserHandle = 4242L
        underTest.chatState.onlineStatus = MegaChatApi.STATUS_AWAY

        assertThat(underTest.getUserOnlineStatus(4242L)).isEqualTo(MegaChatApi.STATUS_AWAY)
    }

    @Test
    fun `test that getUserEmailFromCache returns chat state email when queried for own handle`() {
        underTest.chatState.myUserHandle = 4242L
        underTest.chatState.myEmail = "jane@mega.nz"

        assertThat(underTest.getUserEmailFromCache(4242L)).isEqualTo("jane@mega.nz")
    }

    @Test
    fun `test that getUserFullNameFromCache returns chat state fullname when queried for own handle`() {
        underTest.chatState.myUserHandle = 4242L
        underTest.chatState.myFullname = "Jane Doe"

        assertThat(underTest.getUserFullNameFromCache(4242L)).isEqualTo("Jane Doe")
    }

    @Test
    fun `test that getChatPresenceConfig returns a config backed by chat state when mutated`() = runTest {
        underTest.chatState.onlineStatus = MegaChatApi.STATUS_AWAY
        underTest.chatState.presenceAutoAwayEnabled = true
        underTest.chatState.presenceAutoAwayTimeout = 300L
        underTest.chatState.presencePersist = true
        underTest.chatState.presencePending = true
        underTest.chatState.lastGreenVisible = true

        val config = requireNotNull(underTest.getChatPresenceConfig())

        assertThat(config.onlineStatus).isEqualTo(MegaChatApi.STATUS_AWAY)
        assertThat(config.isAutoawayEnabled).isTrue()
        assertThat(config.autoawayTimeout).isEqualTo(300L)
        assertThat(config.isPersist).isTrue()
        assertThat(config.isPending).isTrue()
        assertThat(config.isLastGreenVisible).isTrue()
    }

    @Test
    fun `test that setPresenceAutoaway updates chat state when invoked`() = runTest {
        underTest.setPresenceAutoaway(true, 300, OptionalMegaChatRequestListenerInterface())

        assertThat(underTest.chatState.presenceAutoAwayEnabled).isTrue()
        assertThat(underTest.chatState.presenceAutoAwayTimeout).isEqualTo(300L)
        assertThat(requireNotNull(underTest.getChatPresenceConfig()).isAutoawayEnabled).isTrue()
    }

    @Test
    fun `test that setPresencePersist updates chat state when invoked`() = runTest {
        underTest.setPresencePersist(true, OptionalMegaChatRequestListenerInterface())

        assertThat(underTest.chatState.presencePersist).isTrue()
        assertThat(requireNotNull(underTest.getChatPresenceConfig()).isPersist).isTrue()
    }

    @Test
    fun `test that setLastGreenVisible updates chat state when invoked`() = runTest {
        underTest.setLastGreenVisible(true, OptionalMegaChatRequestListenerInterface())

        assertThat(underTest.chatState.lastGreenVisible).isTrue()
        assertThat(requireNotNull(underTest.getChatPresenceConfig()).isLastGreenVisible).isTrue()
    }

    @Test
    fun `test that getChatRoom returns seeded room when present in chat state`() {
        val room = StubMegaChatRoom()
        underTest.chatState.chatRooms[42L] = room

        assertThat(underTest.getChatRoom(42L)).isSameInstanceAs(room)
    }

    @Test
    fun `test that getChatRooms returns all seeded rooms when chat state is populated`() {
        val roomA = StubMegaChatRoom()
        val roomB = StubMegaChatRoom()
        underTest.chatState.chatRooms[1L] = roomA
        underTest.chatState.chatRooms[2L] = roomB

        assertThat(underTest.getChatRooms()).containsExactly(roomA, roomB)
    }

    @Test
    fun `test that room list getters return seeded rooms without type filtering when chat state is populated`() {
        val room = StubMegaChatRoom()
        underTest.chatState.chatRooms[1L] = room

        assertThat(underTest.getMeetingChatRooms()).containsExactly(room)
        assertThat(underTest.getGroupChatRooms()).containsExactly(room)
        assertThat(underTest.getIndividualChatRooms()).containsExactly(room)
    }

    @Test
    fun `test that state backed reads return defaults when resetToDefaults is called`() {
        underTest.chatState.myUserHandle = 4242L
        underTest.chatState.myEmail = "jane@mega.nz"
        underTest.chatState.chatRooms[42L] = StubMegaChatRoom()

        underTest.resetToDefaults()

        val defaults = FakeChatState()
        assertThat(underTest.getMyUserHandle()).isEqualTo(defaults.myUserHandle)
        assertThat(underTest.getMyEmail()).isEqualTo(defaults.myEmail)
        assertThat(underTest.getChatRoom(42L)).isNull()
    }
}
