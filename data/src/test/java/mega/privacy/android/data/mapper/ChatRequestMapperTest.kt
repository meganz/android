package mega.privacy.android.data.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.data.mapper.chat.ChatRequestMapper
import mega.privacy.android.domain.entity.ChatPeer
import mega.privacy.android.domain.entity.ChatRequestParamType
import mega.privacy.android.domain.entity.ChatRequestType
import mega.privacy.android.domain.entity.ChatRoomPermission
import nz.mega.sdk.MegaChatPeerList
import nz.mega.sdk.MegaChatRequest
import nz.mega.sdk.MegaChatRoom
import nz.mega.sdk.MegaHandleList
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ChatRequestMapperTest {

    private lateinit var underTest: ChatRequestMapper

    private val chatId1 = 123L
    private val peerHandle1 = 345L
    private val peerHandle2 = 456L
    private val megaHandleListTest = mock<MegaHandleList>()

    @Before
    fun setUp() {
        underTest = ChatRequestMapper()
    }

    @Test
    fun `test mapping initialize request`() {
        val request = mock<MegaChatRequest> {
            on { type }.thenReturn(MegaChatRequest.TYPE_INITIALIZE)
        }

        val actual = underTest(request)
        assertThat(actual.type).isEqualTo(ChatRequestType.Initialize)
    }

    @Test
    fun `test mapping create chat room request contains a valid peer list`() {
        val peerList = mock<MegaChatPeerList>()
        whenever(peerList.size()).thenReturn(2)
        whenever(peerList.getPeerHandle(0)).thenReturn(peerHandle1)
        whenever(peerList.getPeerPrivilege(0)).thenReturn(MegaChatRoom.PRIV_MODERATOR)
        whenever(peerList.getPeerHandle(1)).thenReturn(peerHandle2)
        whenever(peerList.getPeerPrivilege(1)).thenReturn(MegaChatRoom.PRIV_STANDARD)

        val request = mock<MegaChatRequest> {
            on { type }.thenReturn(MegaChatRequest.TYPE_CREATE_CHATROOM)
            on { megaChatPeerList }.thenReturn(peerList)
        }

        val actual = underTest(request)
        val peer1 = ChatPeer(peerHandle1, ChatRoomPermission.Moderator)
        val peer2 = ChatPeer(peerHandle2, ChatRoomPermission.Standard)
        assertThat(actual.type).isEqualTo(ChatRequestType.CreateChatRoom)
        assertThat(actual.peersList).isNotNull()
        assertThat(actual.peersList?.get(0)).isEqualTo(peer1)
        assertThat(actual.peersList?.get(1)).isEqualTo(peer2)
    }

    @Test
    fun `test mapping push received request contains a valid chatId list`() {
        val chatId2 = 234L
        whenever(megaHandleListTest.size()).thenReturn(2)
        whenever(megaHandleListTest[0]).thenReturn(chatId1)
        whenever(megaHandleListTest[1]).thenReturn(chatId2)

        val request = mock<MegaChatRequest> {
            on { type }.thenReturn(MegaChatRequest.TYPE_PUSH_RECEIVED)
            on { megaHandleList }.thenReturn(megaHandleListTest)
        }

        val actual = underTest(request)
        assertThat(actual.type).isEqualTo(ChatRequestType.PushReceived)
        assertThat(actual.handleList).isNotNull()
        assertThat(actual.handleList?.get(0)).isEqualTo(chatId1)
        assertThat(actual.handleList?.get(1)).isEqualTo(chatId2)
    }

    @Test
    fun `test mapping push received request contains a valid peer list`() {
        whenever(megaHandleListTest.size()).thenReturn(1)
        whenever(megaHandleListTest[0]).thenReturn(chatId1)

        val megaHandlePeerListTest = mock<MegaHandleList>()
        whenever(megaHandlePeerListTest.size()).thenReturn(3)
        whenever(megaHandlePeerListTest[0]).thenReturn(peerHandle1)
        whenever(megaHandlePeerListTest[1]).thenReturn(peerHandle2)
        val request = mock<MegaChatRequest> {
            on { type }.thenReturn(MegaChatRequest.TYPE_PUSH_RECEIVED)
            on { megaHandleList }.thenReturn(megaHandleListTest)
            on { getMegaHandleListByChat(chatId1) }.thenReturn(megaHandlePeerListTest)
        }

        val actual = underTest(request)
        assertThat(actual.type).isEqualTo(ChatRequestType.PushReceived)
        assertThat(actual.handleList).isNotNull()
        assertThat(actual.peersListByChatHandle).isNotNull()
        assertThat(actual.peersListByChatHandle?.get(chatId1)?.get(0)).isEqualTo(peerHandle1)
        assertThat(actual.peersListByChatHandle?.get(chatId1)?.get(1)).isEqualTo(peerHandle2)
    }

    @Test
    fun `test that mapping disable audio request contain a valid param type`() {
        val request = mock<MegaChatRequest> {
            on { type }.thenReturn(MegaChatRequest.TYPE_DISABLE_AUDIO_VIDEO_CALL)
            on { paramType }.thenReturn(MegaChatRequest.AUDIO)
        }

        val actual = underTest(request)
        assertThat(actual.type).isEqualTo(ChatRequestType.DisableAudioVideoCall)
        assertThat(actual.paramType).isEqualTo(ChatRequestParamType.Audio)
    }

    @Test
    fun `test that mapping load preview request contain a valid param type`() {
        val request = mock<MegaChatRequest> {
            on { type }.thenReturn(MegaChatRequest.TYPE_LOAD_PREVIEW)
            on { paramType }.thenReturn(1)
        }

        val actual = underTest(request)
        assertThat(actual.type).isEqualTo(ChatRequestType.LoadPreview)
        assertThat(actual.paramType).isEqualTo(ChatRequestParamType.MEETING_LINK)
    }
}