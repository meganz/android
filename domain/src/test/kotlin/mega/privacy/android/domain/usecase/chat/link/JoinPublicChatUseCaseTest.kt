package mega.privacy.android.domain.usecase.chat.link

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.ChatRequest
import mega.privacy.android.domain.entity.ChatRequestType
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.exception.ChatRoomDoesNotExistException
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.usecase.GetChatRoomUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.*
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JoinPublicChatUseCaseTest {
    private lateinit var underTest: JoinPublicChatUseCase

    private val chatRepository = mock<ChatRepository>()

    private val getChatRoomUseCase = mock<GetChatRoomUseCase>()

    @BeforeAll
    internal fun initialise() {
        underTest = JoinPublicChatUseCase(
            chatRepository = chatRepository,
            getChatRoomUseCase = getChatRoomUseCase
        )
    }

    @BeforeEach
    fun reset() {
        reset(chatRepository, getChatRoomUseCase)
    }

    @Test
    fun `test that autorejoinPublicChat is called when chat room is not active and public handle is not null`() =
        runTest {
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


            whenever(getChatRoomUseCase(chatId)).thenReturn(chatRoom)

            underTest.invoke(chatId, chatPublicHandle, true)

            verify(getChatRoomUseCase).invoke(chatId)
            verify(chatRepository).autorejoinPublicChat(chatId, chatPublicHandle)
        }

    @Test
    fun `test that autorejoinPublicChat is called when chat room is active`() =
        runTest {
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
                privilege = 0,
                text = null,
                link = null,
                peersListByChatHandle = null,
                handleList = null,
                paramType = null
            )

            whenever(getChatRoomUseCase(chatId)).thenReturn(chatRoom)

            underTest.invoke(chatId, chatPublicHandle, true)

            verify(getChatRoomUseCase).invoke(chatId)
            verify(chatRepository).autorejoinPublicChat(chatId, chatRequest.userHandle)
        }

    @Test
    fun `test that exception is thrown when chat room does not exist`() = runTest {
        val chatId = 1L
        val chatPublicHandle = 2L


        whenever(getChatRoomUseCase(chatId)).thenReturn(null)

        assertThrows<ChatRoomDoesNotExistException> {
            underTest.invoke(
                chatId,
                chatPublicHandle,
                true
            )
        }
    }
}