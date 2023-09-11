package mega.privacy.android.domain.usecase.chat

import kotlinx.coroutines.runBlocking
import mega.privacy.android.domain.entity.ChatRequest
import mega.privacy.android.domain.entity.ChatRequestType
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.exception.ChatRoomDoesNotExistException
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.usecase.GetChatRoom
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.whenever

class JoinGuestChatCallUseCaseTest {
    private val chatRepository: ChatRepository = mock()
    private val getChatRoom: GetChatRoom = mock()
    private val createEphemeralAccountUseCase: CreateEphemeralAccountUseCase = mock()
    private val initGuestChatSessionUseCase: InitGuestChatSessionUseCase = mock()

    private lateinit var underTest: JoinGuestChatCallUseCase

    @Before
    fun setup() {
        underTest = JoinGuestChatCallUseCase(
            chatRepository,
            getChatRoom,
            createEphemeralAccountUseCase,
            initGuestChatSessionUseCase
        )
    }

    @Test
    fun `test that all methods are called in correct order`() = runBlocking {
        val chatLink = "chatLink"
        val firstName = "firstName"
        val lastName = "lastName"
        val chatId = 1L
        val chatPublicHandle = 2L
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
            isPreview = false,
            authorizationToken = null,
            title = "Chat Room",
            hasCustomTitle = false,
            unreadCount = 0,
            userTyping = 0,
            userHandle = 0,
            isActive = true,
            isArchived = false,
            retentionTime = 0,
            creationTime = 0,
            isMeeting = false,
            isWaitingRoom = false,
            isOpenInvite = false,
            isSpeakRequest = false
        )

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
            privilege = null,
            text = null,
            link = null,
            peersListByChatHandle = null,
            handleList = null,
            paramType = null
        )

        whenever(chatRepository.openChatPreview(chatLink)).thenReturn(chatRequest)
        whenever(getChatRoom(chatId)).thenReturn(chatRoom)

        underTest.invoke(chatLink, firstName, lastName)

        verify(initGuestChatSessionUseCase).invoke(false)
        verify(createEphemeralAccountUseCase).invoke(firstName, lastName)
        verify(chatRepository).openChatPreview(chatLink)
        verify(getChatRoom).invoke(chatId)
        verify(chatRepository).autojoinPublicChat(chatId)
    }

    @Test(expected = ChatRoomDoesNotExistException::class)
    fun `test that exception is thrown when chat room does not exist`() = runBlocking {
        val chatLink = "chatLink"
        val firstName = "firstName"
        val lastName = "lastName"
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
            privilege = null,
            text = null,
            link = null,
            peersListByChatHandle = null,
            handleList = null,
            paramType = null
        )

        whenever(chatRepository.openChatPreview(chatLink)).thenReturn(chatRequest)
        whenever(getChatRoom(chatId)).thenReturn(null)

        underTest.invoke(chatLink, firstName, lastName)
    }
}
