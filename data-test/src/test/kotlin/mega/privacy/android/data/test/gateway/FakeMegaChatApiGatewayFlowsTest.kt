package mega.privacy.android.data.test.gateway

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.model.ChatRoomUpdate
import mega.privacy.android.data.model.ChatUpdate
import mega.privacy.android.data.model.ScheduledMeetingUpdate
import mega.privacy.android.data.model.meeting.ChatCallUpdate
import mega.privacy.android.domain.entity.chat.ChatVideoUpdate
import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaChatMessage
import nz.mega.sdk.MegaChatNotificationListenerInterface
import nz.mega.sdk.MegaChatVideoListenerInterface
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Documents the [FakeMegaChatApiGateway] flow properties, their emit helpers, and the
 * video/notification listener registration collections.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FakeMegaChatApiGatewayFlowsTest {

    private lateinit var underTest: FakeMegaChatApiGateway

    @BeforeEach
    fun setUp() {
        underTest = FakeMegaChatApiGateway()
    }

    private fun noOpVideoListener() = MegaChatVideoListenerInterface { _, _, _, _, _ -> }

    @Test
    fun `test that chatUpdates emits when emitChatUpdate is called`() = runTest {
        underTest.chatUpdates.test {
            underTest.emitChatUpdate(ChatUpdate.OnChatInitStateUpdate(7))

            assertThat(awaitItem()).isEqualTo(ChatUpdate.OnChatInitStateUpdate(7))
        }
    }

    @Test
    fun `test that chatCallUpdates emits when emitChatCallUpdate is called`() = runTest {
        underTest.chatCallUpdates.test {
            underTest.emitChatCallUpdate(ChatCallUpdate.OnChatCallUpdate(null))

            assertThat(awaitItem()).isEqualTo(ChatCallUpdate.OnChatCallUpdate(null))
        }
    }

    @Test
    fun `test that scheduledMeetingUpdates emits when emitScheduledMeetingUpdate is called`() =
        runTest {
            underTest.scheduledMeetingUpdates.test {
                underTest.emitScheduledMeetingUpdate(
                    ScheduledMeetingUpdate.OnSchedMeetingOccurrencesUpdate(1L, true)
                )

                assertThat(awaitItem()).isEqualTo(
                    ScheduledMeetingUpdate.OnSchedMeetingOccurrencesUpdate(1L, true)
                )
            }
        }

    @Test
    fun `test that openChatRoom flow emits when emitChatRoomUpdate targets its chat id`() =
        runTest {
            underTest.openChatRoom(1L).test {
                underTest.emitChatRoomUpdate(1L, ChatRoomUpdate.OnReactionUpdate(2L, "+1", 1))

                assertThat(awaitItem())
                    .isEqualTo(ChatRoomUpdate.OnReactionUpdate(2L, "+1", 1))
            }
        }

    @Test
    fun `test that openChatRoom flow stays silent when emitChatRoomUpdate targets another chat id`() =
        runTest {
            underTest.openChatRoom(1L).test {
                underTest.emitChatRoomUpdate(2L, ChatRoomUpdate.OnReactionUpdate(2L, "+1", 1))

                expectNoEvents()
            }
        }

    @Test
    fun `test that getChatLocalVideoUpdates flow emits when emitChatLocalVideoUpdate is called`() =
        runTest {
            underTest.getChatLocalVideoUpdates(1L).test {
                underTest.emitChatLocalVideoUpdate(1L, ChatVideoUpdate(320, 240, ByteArray(0)))

                val update = awaitItem()
                assertThat(update.width).isEqualTo(320)
                assertThat(update.height).isEqualTo(240)
            }
        }

    @Test
    fun `test that getChatRemoteVideoUpdates flow emits when emitChatRemoteVideoUpdate is called`() =
        runTest {
            underTest.getChatRemoteVideoUpdates(1L, 2L, true).test {
                underTest.emitChatRemoteVideoUpdate(1L, ChatVideoUpdate(640, 480, ByteArray(0)))

                val update = awaitItem()
                assertThat(update.width).isEqualTo(640)
                assertThat(update.height).isEqualTo(480)
            }
        }

    @Test
    fun `test that local video updates stay silent when a remote update is emitted for the same chat`() =
        runTest {
            underTest.getChatLocalVideoUpdates(1L).test {
                underTest.emitChatRemoteVideoUpdate(1L, ChatVideoUpdate(640, 480, ByteArray(0)))

                expectNoEvents()
            }
        }

    @Test
    fun `test that chat video listeners collection tracks local registration when added`() {
        val listener = noOpVideoListener()

        underTest.addChatLocalVideoListener(1L, listener)

        val registration = underTest.chatVideoListeners.single()
        assertThat(registration.chatId).isEqualTo(1L)
        assertThat(registration.clientId).isNull()
        assertThat(registration.hiRes).isNull()
        assertThat(registration.listener).isSameInstanceAs(listener)
    }

    @Test
    fun `test that chat video listeners collection tracks remote registration when added`() {
        val listener = noOpVideoListener()

        underTest.addChatRemoteVideoListener(1L, 2L, true, listener)

        val registration = underTest.chatVideoListeners.single()
        assertThat(registration.chatId).isEqualTo(1L)
        assertThat(registration.clientId).isEqualTo(2L)
        assertThat(registration.hiRes).isTrue()
        assertThat(registration.listener).isSameInstanceAs(listener)
    }

    @Test
    fun `test that chat video listeners collection removes registration when removeChatVideoListener is called`() {
        val listener = noOpVideoListener()
        underTest.addChatRemoteVideoListener(1L, 2L, true, listener)

        underTest.removeChatVideoListener(1L, 2L, true, listener)

        assertThat(underTest.chatVideoListeners).isEmpty()
        assertThat(underTest.invocations.map { it.methodName })
            .containsExactly("addChatRemoteVideoListener", "removeChatVideoListener")
            .inOrder()
    }

    @Test
    fun `test that chat notification listeners collection tracks register and deregister when called`() {
        val listener = object : MegaChatNotificationListenerInterface {
            override fun onChatNotification(
                api: MegaChatApiJava?,
                chatid: Long,
                msg: MegaChatMessage?,
            ) = Unit
        }

        underTest.registerChatNotificationListener(listener)

        assertThat(underTest.chatNotificationListeners).containsExactly(listener)

        underTest.deregisterChatNotificationListener(listener)

        assertThat(underTest.chatNotificationListeners).isEmpty()
    }
}
