package mega.privacy.android.domain.usecase.chat

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.ChatRequest
import mega.privacy.android.domain.entity.ChatRequestType
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.ChatPreview
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.exception.ChatRoomDoesNotExistException
import mega.privacy.android.domain.usecase.CheckChatLinkUseCase
import mega.privacy.android.domain.usecase.GetChatRoomUseCase
import mega.privacy.android.domain.usecase.chat.link.JoinPublicChatUseCase
import mega.privacy.android.domain.usecase.chat.link.LoadChatPreviewUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JoinChatCallUseCaseTest {
    private lateinit var underTest: JoinChatCallUseCase

    private val loadChatPreviewUseCase = mock<LoadChatPreviewUseCase>()
    private val joinPublicChatUseCase = mock<JoinPublicChatUseCase>()
    private val checkChatLinkUseCase = mock<CheckChatLinkUseCase>()
    private val getChatRoomUseCase = mock<GetChatRoomUseCase>()

    @BeforeAll
    fun setup() {
        underTest = JoinChatCallUseCase(
            loadChatPreviewUseCase = loadChatPreviewUseCase,
            joinPublicChatUseCase = joinPublicChatUseCase,
            checkChatLinkUseCase = checkChatLinkUseCase,
            getChatRoomUseCase = getChatRoomUseCase,
        )
    }

    @BeforeEach
    fun reset() {
        reset(
            loadChatPreviewUseCase,
            joinPublicChatUseCase,
            checkChatLinkUseCase,
            getChatRoomUseCase
        )
    }

    @Test
    fun `test that all methods are called in correct order`() = runTest {
        val chatLink = "chatLink"
        val chatId = 1L
        val chatPublicHandle = 2L

        val chatRequest = ChatRequest(
            type = ChatRequestType.LoadPreview,
            requestString = null,
            tag = 0,
            number = 0,
            numRetry = 0,
            flag = false,
            peersList = null,
            chatHandle = chatId,
            userHandle = chatPublicHandle,
            privilege = 0,
            text = null,
            link = null,
            peersListByChatHandle = null,
            handleList = null,
            paramType = null
        )

        val chatRoom = ChatRoom(
            chatId = chatId,
            ownPrivilege = ChatRoomPermission.Standard,
            numPreviewers = 0,
            peerPrivilegesByHandles = emptyMap(),
            peerCount = 0,
            peerHandlesList = emptyList(),
            peerPrivilegesList = emptyList(),
            isGroup = false,
            isPublic = true,
            isPreview = true,
            authorizationToken = null,
            title = "Chat Room",
            hasCustomTitle = false,
            unreadCount = 0,
            userTyping = 0,
            userHandle = 0,
            isActive = false,
            isArchived = false,
            retentionTime = 0,
            creationTime = 0,
            isMeeting = false,
            isWaitingRoom = false,
            isOpenInvite = false,
            isSpeakRequest = false
        )

        whenever(checkChatLinkUseCase(chatLink)).thenReturn(chatRequest)
        whenever(getChatRoomUseCase(chatId)).thenReturn(chatRoom)
        underTest.invoke(chatLink)

        verify(joinPublicChatUseCase).invoke(chatId, chatPublicHandle, false)
    }

    @Test
    fun `test that exception is thrown when chat room does not exist`() = runTest {
        val chatId = 1L
        val chatPublicHandle = 2L
        val chatLink = "chatLink"
        val chatRequest = ChatRequest(
            type = ChatRequestType.LoadPreview,
            requestString = null,
            tag = 0,
            number = 0,
            numRetry = 0,
            flag = false,
            peersList = null,
            chatHandle = chatId,
            userHandle = chatPublicHandle,
            privilege = 0,
            text = null,
            link = null,
            peersListByChatHandle = null,
            handleList = null,
            paramType = null
        )

        val chatPreview = ChatPreview(chatRequest, false)

        whenever(checkChatLinkUseCase(chatLink)).thenReturn(chatRequest)
        whenever(getChatRoomUseCase(chatId)).thenReturn(null)
        whenever(loadChatPreviewUseCase(chatLink)).thenReturn(chatPreview)

        assertThrows<ChatRoomDoesNotExistException> {
            underTest.invoke(chatLink)
        }
    }
}
