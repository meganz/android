package mega.privacy.android.data.test.gateway

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import nz.mega.sdk.MegaChatApi
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Documents the WireMock-style stubbing and verification surface of [FakeMegaChatApiGateway].
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FakeMegaChatApiGatewayStubbingTest {

    private lateinit var underTest: FakeMegaChatApiGateway

    @BeforeEach
    fun setUp() {
        underTest = FakeMegaChatApiGateway()
    }

    @Test
    fun `test that stubResult overrides the default when method is stubbed`() = runTest {
        underTest.stubResult(MegaChatApiGateway::getNumUnreadChats, 5)

        assertThat(underTest.getNumUnreadChats()).isEqualTo(5)
    }

    @Test
    fun `test that stub answer receives the call arguments when method is stubbed`() {
        underTest.stub(MegaChatApiGateway::getUserEmailFromCache) { arguments ->
            "user${arguments[0]}@mega.nz"
        }

        assertThat(underTest.getUserEmailFromCache(42L)).isEqualTo("user42@mega.nz")
    }

    @Test
    fun `test that matcher stub only answers matching arguments when others use the default`() {
        underTest.stub(
            MegaChatApiGateway::getChatConnectionState,
            matcher = { it[0] == 7L },
        ) { MegaChatApi.CHAT_CONNECTION_OFFLINE }

        assertThat(underTest.getChatConnectionState(7L))
            .isEqualTo(MegaChatApi.CHAT_CONNECTION_OFFLINE)
        assertThat(underTest.getChatConnectionState(8L))
            .isEqualTo(MegaChatApi.CHAT_CONNECTION_ONLINE)
    }

    @Test
    fun `test that the later stub wins when the same method is stubbed twice`() = runTest {
        underTest.stubResult(MegaChatApiGateway::getNumUnreadChats, 1)
        underTest.stubResult(MegaChatApiGateway::getNumUnreadChats, 2)

        assertThat(underTest.getNumUnreadChats()).isEqualTo(2)
    }

    @Test
    fun `test that suspend method throws when stubError is applied`() = runTest {
        val error = IllegalStateException("boom")
        underTest.stubError(MegaChatApiGateway::loadMessages, error)

        val result = runCatching { underTest.loadMessages(1L, 32) }

        assertThat(result.exceptionOrNull()).isSameInstanceAs(error)
    }

    @Test
    fun `test that non suspend method throws when stubError is applied`() {
        val error = IllegalStateException("boom")
        underTest.stubError(MegaChatApiGateway::hasUrl, error)

        val result = runCatching { underTest.hasUrl("content") }

        assertThat(result.exceptionOrNull()).isSameInstanceAs(error)
    }

    @Test
    fun `test that invocations are recorded in call order with their arguments`() = runTest {
        underTest.getChatRoom(1L)
        underTest.hasCallInChatRoom(2L)
        underTest.hasUrl("content")

        assertThat(underTest.invocations.map { it.methodName })
            .containsExactly("getChatRoom", "hasCallInChatRoom", "hasUrl")
            .inOrder()
        assertThat(underTest.invocations[0].arguments).containsExactly(1L)
        assertThat(underTest.invocations[1].arguments).containsExactly(2L)
        assertThat(underTest.invocations[2].arguments).containsExactly("content")
    }

    @Test
    fun `test that invocationsOf filters recorded invocations when multiple methods were called`() {
        underTest.getChatRoom(1L)
        underTest.getChatRoom(2L)
        underTest.hasUrl("content")

        val invocations = underTest.invocationsOf(MegaChatApiGateway::getChatRoom)

        assertThat(invocations).hasSize(2)
        assertThat(invocations.map { it.arguments.single() }).containsExactly(1L, 2L).inOrder()
    }

    @Test
    fun `test that stubs stay active when clearInvocations is called`() = runTest {
        underTest.stubResult(MegaChatApiGateway::getNumUnreadChats, 5)
        underTest.getNumUnreadChats()

        underTest.clearInvocations()

        assertThat(underTest.invocations).isEmpty()
        assertThat(underTest.getNumUnreadChats()).isEqualTo(5)
    }

    @Test
    fun `test that defaults return when clearStubs is called`() = runTest {
        underTest.stubResult(MegaChatApiGateway::getNumUnreadChats, 5)

        underTest.clearStubs()

        assertThat(underTest.getNumUnreadChats()).isEqualTo(0)
    }

    @Test
    fun `test that stubs invocations and listeners are cleared when resetToDefaults is called`() =
        runTest {
            underTest.stubResult(MegaChatApiGateway::getNumUnreadChats, 5)
            underTest.getNumUnreadChats()
            underTest.addChatLocalVideoListener(1L) { _, _, _, _, _ -> }

            underTest.resetToDefaults()

            assertThat(underTest.invocations).isEmpty()
            assertThat(underTest.chatVideoListeners).isEmpty()
            assertThat(underTest.getNumUnreadChats()).isEqualTo(0)
        }
}
