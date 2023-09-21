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
import org.mockito.Mockito
import org.mockito.kotlin.whenever

class JoinChatCallUseCaseTest {
    private val chatRepository: ChatRepository = Mockito.mock()
    private val getChatRoom: GetChatRoom = Mockito.mock()

    private lateinit var underTest: JoinChatCallUseCase

    @Before
    fun setup() {
        underTest = JoinChatCallUseCase(
            chatRepository,
            getChatRoom
        )
    }

    @Test
    fun `test that all methods are called in correct order`() = runBlocking {
        val chatLink = "chatLink"
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

        underTest.invoke(chatLink)

        Mockito.verify(chatRepository).openChatPreview(chatLink)
        Mockito.verify(getChatRoom).invoke(chatId)
        Mockito.verifyNoMoreInteractions(chatRepository)
    }

    @Test(expected = ChatRoomDoesNotExistException::class)
    fun `test that exception is thrown when chat room does not exist`() = runBlocking {
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
            privilege = null,
            text = null,
            link = null,
            peersListByChatHandle = null,
            handleList = null,
            paramType = null
        )

        whenever(chatRepository.openChatPreview(chatLink)).thenReturn(chatRequest)
        whenever(getChatRoom(chatId)).thenReturn(null)

        underTest.invoke(chatLink)
    }

    @Test
    fun `test that autorejoinPublicChat is called when chat room is not active and public handle is not null`() =
        runBlocking {
            val chatLink = "chatLink"
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

            underTest.invoke(chatLink)

            Mockito.verify(chatRepository).openChatPreview(chatLink)
            Mockito.verify(getChatRoom).invoke(chatId)
            Mockito.verify(chatRepository).autorejoinPublicChat(chatId, chatPublicHandle)
        }

    @Test
    fun `test that autojoinPublicChat is called when chat room is active or public handle is null`() =
        runBlocking {
            val chatLink = "chatLink"
            val chatId = 1L
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

            val chatRequest = ChatRequest(
                type = ChatRequestType.LoadPreview,
                requestString = null,
                tag = 0,
                number = 0,
                numRetry = 0,
                flag = false,
                peersList = null,
                chatHandle = chatId,
                userHandle = null,
                privilege = null,
                text = null,
                link = null,
                peersListByChatHandle = null,
                handleList = null,
                paramType = null
            )

            whenever(chatRepository.openChatPreview(chatLink)).thenReturn(chatRequest)
            whenever(getChatRoom(chatId)).thenReturn(chatRoom)

            underTest.invoke(chatLink)

            Mockito.verify(chatRepository).openChatPreview(chatLink)
            Mockito.verify(getChatRoom).invoke(chatId)
            Mockito.verify(chatRepository).autojoinPublicChat(chatId)
        }
}
