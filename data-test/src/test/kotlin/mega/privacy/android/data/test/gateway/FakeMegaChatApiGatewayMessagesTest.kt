package mega.privacy.android.data.test.gateway

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.model.ChatRoomUpdate
import mega.privacy.android.data.test.stub.StubMegaChatMessage
import nz.mega.sdk.MegaChatApi
import nz.mega.sdk.MegaChatMessage
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Documents how [FakeMegaChatApiGateway] feeds the app's message paging pipeline: seeded history
 * delivered through `loadMessages` with the SDK's callback sequence and termination signal, the
 * own-message echo of `sendMessage`, and live delivery via [FakeMegaChatApiGateway.emitMessageReceived].
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FakeMegaChatApiGatewayMessagesTest {

    private lateinit var underTest: FakeMegaChatApiGateway

    @BeforeEach
    fun setUp() {
        underTest = FakeMegaChatApiGateway()
    }

    private fun seedMessages(chatId: Long, vararg msgIds: Long) = msgIds.map { msgId ->
        StubMegaChatMessage(
            msgId = msgId,
            userHandle = PEER_HANDLE,
            type = MegaChatMessage.TYPE_NORMAL,
            status = MegaChatMessage.STATUS_SEEN,
            timestamp = msgId,
            content = "message $msgId",
        ).also { underTest.chatState.addChatMessage(chatId, it) }
    }

    @Test
    fun `test that loadMessages delivers seeded messages newest first followed by a null terminator`() =
        runTest {
            val (oldest, middle, newest) = seedMessages(CHAT_ID, 1L, 2L, 3L)

            underTest.openChatRoom(CHAT_ID).test {
                underTest.loadMessages(CHAT_ID, 32)

                assertThat(awaitItem()).isEqualTo(ChatRoomUpdate.OnMessageLoaded(newest))
                assertThat(awaitItem()).isEqualTo(ChatRoomUpdate.OnMessageLoaded(middle))
                assertThat(awaitItem()).isEqualTo(ChatRoomUpdate.OnMessageLoaded(oldest))
                assertThat(awaitItem()).isEqualTo(ChatRoomUpdate.OnMessageLoaded(null))
            }
        }

    @Test
    fun `test that loadMessages returns SOURCE_LOCAL when seeded messages are delivered`() =
        runTest {
            seedMessages(CHAT_ID, 1L)

            underTest.openChatRoom(CHAT_ID).test {
                assertThat(underTest.loadMessages(CHAT_ID, 32))
                    .isEqualTo(MegaChatApi.SOURCE_LOCAL)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that loadMessages emits only the null terminator and returns SOURCE_NONE when no history is seeded`() =
        runTest {
            underTest.openChatRoom(CHAT_ID).test {
                val source = underTest.loadMessages(CHAT_ID, 32)

                assertThat(source).isEqualTo(MegaChatApi.SOURCE_NONE)
                assertThat(awaitItem()).isEqualTo(ChatRoomUpdate.OnMessageLoaded(null))
                expectNoEvents()
            }
        }

    @Test
    fun `test that loadMessages limits a batch to count and terminates it`() = runTest {
        val messages = seedMessages(CHAT_ID, 1L, 2L, 3L)

        underTest.openChatRoom(CHAT_ID).test {
            underTest.loadMessages(CHAT_ID, 2)

            assertThat(awaitItem()).isEqualTo(ChatRoomUpdate.OnMessageLoaded(messages[2]))
            assertThat(awaitItem()).isEqualTo(ChatRoomUpdate.OnMessageLoaded(messages[1]))
            assertThat(awaitItem()).isEqualTo(ChatRoomUpdate.OnMessageLoaded(null))
            expectNoEvents()
        }
    }

    @Test
    fun `test that loadMessages delivers the remaining older messages when called again`() =
        runTest {
            val messages = seedMessages(CHAT_ID, 1L, 2L, 3L)

            underTest.openChatRoom(CHAT_ID).test {
                underTest.loadMessages(CHAT_ID, 2)
                skipItems(3)

                underTest.loadMessages(CHAT_ID, 2)

                assertThat(awaitItem()).isEqualTo(ChatRoomUpdate.OnMessageLoaded(messages[0]))
                assertThat(awaitItem()).isEqualTo(ChatRoomUpdate.OnMessageLoaded(null))
            }
        }

    @Test
    fun `test that loadMessages returns SOURCE_NONE when the history is exhausted`() = runTest {
        seedMessages(CHAT_ID, 1L)

        underTest.openChatRoom(CHAT_ID).test {
            assertThat(underTest.loadMessages(CHAT_ID, 32)).isEqualTo(MegaChatApi.SOURCE_LOCAL)
            assertThat(underTest.loadMessages(CHAT_ID, 32)).isEqualTo(MegaChatApi.SOURCE_NONE)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that loadMessages replays the full history when called after reporting SOURCE_NONE`() =
        runTest {
            val (message) = seedMessages(CHAT_ID, 1L)

            underTest.openChatRoom(CHAT_ID).test {
                underTest.loadMessages(CHAT_ID, 32)
                underTest.loadMessages(CHAT_ID, 32)
                skipItems(3)

                assertThat(underTest.loadMessages(CHAT_ID, 32))
                    .isEqualTo(MegaChatApi.SOURCE_LOCAL)
                assertThat(awaitItem()).isEqualTo(ChatRoomUpdate.OnMessageLoaded(message))
                assertThat(awaitItem()).isEqualTo(ChatRoomUpdate.OnMessageLoaded(null))
            }
        }

    @Test
    fun `test that sendMessage returns an own in-flight message echoing the content`() {
        val message = underTest.sendMessage(CHAT_ID, "hello")

        assertThat(message).isNotNull()
        assertThat(message?.content).isEqualTo("hello")
        assertThat(message?.userHandle).isEqualTo(underTest.chatState.myUserHandle)
        assertThat(message?.status).isEqualTo(MegaChatMessage.STATUS_SENDING)
        assertThat(message?.type).isEqualTo(MegaChatMessage.TYPE_NORMAL)
        assertThat(message?.msgId).isEqualTo(message?.tempId)
    }

    @Test
    fun `test that sendMessage appends the echoed message to the chat history`() {
        val message = underTest.sendMessage(CHAT_ID, "hello")

        assertThat(underTest.chatState.chatMessages[CHAT_ID]).containsExactly(message)
    }

    @Test
    fun `test that sendMessage assigns unique ids when called repeatedly`() {
        val first = underTest.sendMessage(CHAT_ID, "one")
        val second = underTest.sendMessage(CHAT_ID, "two")

        assertThat(first?.msgId).isNotEqualTo(second?.msgId)
    }

    @Test
    fun `test that sent messages are delivered as history when the pipeline reloads`() = runTest {
        val message = underTest.sendMessage(CHAT_ID, "hello")

        underTest.openChatRoom(CHAT_ID).test {
            underTest.loadMessages(CHAT_ID, 32)

            assertThat(awaitItem()).isEqualTo(ChatRoomUpdate.OnMessageLoaded(message))
            assertThat(awaitItem()).isEqualTo(ChatRoomUpdate.OnMessageLoaded(null))
        }
    }

    @Test
    fun `test that emitMessageReceived emits OnMessageReceived into the openChatRoom flow`() =
        runTest {
            val message = StubMegaChatMessage(msgId = 7L, content = "live")

            underTest.openChatRoom(CHAT_ID).test {
                underTest.emitMessageReceived(CHAT_ID, message)

                assertThat(awaitItem()).isEqualTo(ChatRoomUpdate.OnMessageReceived(message))
            }
        }

    @Test
    fun `test that emitMessageReceived appends the message to the chat history`() = runTest {
        val message = StubMegaChatMessage(msgId = 7L, content = "live")

        underTest.emitMessageReceived(CHAT_ID, message)

        assertThat(underTest.chatState.chatMessages[CHAT_ID]).containsExactly(message)
    }

    @Test
    fun `test that getMessage resolves through the seeded history when the id matches`() {
        val (message) = seedMessages(CHAT_ID, 5L)

        assertThat(underTest.getMessage(CHAT_ID, 5L)).isSameInstanceAs(message)
        assertThat(underTest.getMessage(CHAT_ID, 6L)).isNull()
    }

    @Test
    fun `test that resetToDefaults clears the seeded history and the delivery cursor`() = runTest {
        seedMessages(CHAT_ID, 1L)
        underTest.openChatRoom(CHAT_ID).test {
            underTest.loadMessages(CHAT_ID, 32)
            cancelAndIgnoreRemainingEvents()
        }

        underTest.resetToDefaults()

        assertThat(underTest.chatState.chatMessages).isEmpty()
        val (reseeded) = seedMessages(CHAT_ID, 2L)
        underTest.openChatRoom(CHAT_ID).test {
            underTest.loadMessages(CHAT_ID, 32)

            assertThat(awaitItem()).isEqualTo(ChatRoomUpdate.OnMessageLoaded(reseeded))
            assertThat(awaitItem()).isEqualTo(ChatRoomUpdate.OnMessageLoaded(null))
        }
    }

    private companion object {
        const val CHAT_ID = 1L
        const val PEER_HANDLE = 222L
    }
}
