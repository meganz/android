package mega.privacy.android.data.test.state

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.data.test.stub.StubMegaChatMessage
import mega.privacy.android.data.test.stub.StubMegaChatRoom
import nz.mega.sdk.MegaChatApi
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FakeChatStateTest {

    @Test
    fun `test that the chat state describes an online session when created`() {
        val underTest = FakeChatState()

        assertThat(underTest.initState).isEqualTo(MegaChatApi.INIT_ONLINE_SESSION)
        assertThat(underTest.onlineStatus).isEqualTo(MegaChatApi.STATUS_ONLINE)
        assertThat(underTest.myUserHandle).isEqualTo(111L)
        assertThat(underTest.myFullname).isEqualTo("Test User")
        assertThat(underTest.myEmail).isEqualTo("test@mega.nz")
        assertThat(underTest.chatRooms).isEmpty()
    }

    @Test
    fun `test that chat rooms are resolvable by chat id when registered`() {
        val underTest = FakeChatState()
        val chatRoom = StubMegaChatRoom(chatId = 1L, title = "Chat")

        underTest.chatRooms[1L] = chatRoom

        assertThat(underTest.chatRooms[1L]).isSameInstanceAs(chatRoom)
    }

    @Test
    fun `test that addChatMessage appends to the history of its chat id in order`() {
        val underTest = FakeChatState()
        val first = StubMegaChatMessage(msgId = 1L)
        val second = StubMegaChatMessage(msgId = 2L)

        underTest.addChatMessage(1L, first)
        underTest.addChatMessage(1L, second)
        underTest.addChatMessage(2L, StubMegaChatMessage(msgId = 3L))

        assertThat(underTest.chatMessages[1L]).containsExactly(first, second).inOrder()
        assertThat(underTest.chatMessages[2L]).hasSize(1)
    }

    @Test
    fun `test that reset restores the defaults when fields were mutated`() {
        val underTest = FakeChatState().apply {
            initState = MegaChatApi.INIT_OFFLINE_SESSION
            onlineStatus = MegaChatApi.STATUS_OFFLINE
            myUserHandle = 999L
            myFullname = "Other User"
            myEmail = "other@mega.nz"
            chatRooms[1L] = StubMegaChatRoom(chatId = 1L)
            addChatMessage(1L, StubMegaChatMessage(msgId = 1L))
        }

        underTest.reset()

        assertThat(underTest.initState).isEqualTo(MegaChatApi.INIT_ONLINE_SESSION)
        assertThat(underTest.onlineStatus).isEqualTo(MegaChatApi.STATUS_ONLINE)
        assertThat(underTest.myUserHandle).isEqualTo(111L)
        assertThat(underTest.myFullname).isEqualTo("Test User")
        assertThat(underTest.myEmail).isEqualTo("test@mega.nz")
        assertThat(underTest.chatRooms).isEmpty()
        assertThat(underTest.chatMessages).isEmpty()
    }
}
