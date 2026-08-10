package mega.privacy.android.data.test.gateway

import com.google.common.truth.Truth.assertThat
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
