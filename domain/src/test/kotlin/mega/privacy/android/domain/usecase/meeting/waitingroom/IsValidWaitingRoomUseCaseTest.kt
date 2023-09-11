package mega.privacy.android.domain.usecase.meeting.waitingroom

import kotlinx.coroutines.runBlocking
import mega.privacy.android.domain.entity.ChatRequest
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.exception.ChatRoomDoesNotExistException
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.usecase.CheckChatLinkUseCase
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.whenever

class IsValidWaitingRoomUseCaseTest {

    private lateinit var underTest: IsValidWaitingRoomUseCase
    private val chatRepository = Mockito.mock(ChatRepository::class.java)
    private val checkChatLink = Mockito.mock(CheckChatLinkUseCase::class.java)

    private val mainChatRoom = ChatRoom(
        chatId = 1L,
        ownPrivilege = ChatRoomPermission.Standard,
        numPreviewers = 0L,
        peerPrivilegesByHandles = emptyMap(),
        peerCount = 0L,
        peerHandlesList = emptyList(),
        peerPrivilegesList = emptyList(),
        isGroup = false,
        isPublic = false,
        isPreview = false,
        authorizationToken = null,
        title = "Test",
        hasCustomTitle = false,
        unreadCount = 0,
        userTyping = 0L,
        userHandle = 0L,
        isActive = true,
        isArchived = false,
        retentionTime = 0L,
        creationTime = 0L,
        isMeeting = false,
        isWaitingRoom = true,
        isOpenInvite = false,
        isSpeakRequest = false
    )

    @Before
    fun setUp() {
        underTest = IsValidWaitingRoomUseCase(chatRepository, checkChatLink)
    }

    @Test(expected = ChatRoomDoesNotExistException::class)
    fun `test that exception is thrown when chat room does not exist`(): Unit =
        runBlocking {
            whenever(chatRepository.getChatRoom(1L)).thenReturn(null)
            underTest.invoke(1L)
        }

    @Test
    fun `test that invoke() returns true when chat room is a waiting room and user is not a moderator`() =
        runBlocking {
            whenever(chatRepository.getChatRoom(1L)).thenReturn(mainChatRoom)
            assert(underTest.invoke(1L))
        }

    @Test
    fun `test that invoke() returns false when chat room is not a waiting room`() =
        runBlocking {
            val chatRoom = mainChatRoom.copy(isWaitingRoom = false)
            whenever(chatRepository.getChatRoom(1L)).thenReturn(chatRoom)
            assert(!underTest.invoke(1L))
        }

    @Test
    fun `test that invoke() returns false when user is a moderator in the waiting room`() =
        runBlocking {
            val chatRoom = mainChatRoom.copy(ownPrivilege = ChatRoomPermission.Moderator)
            whenever(chatRepository.getChatRoom(1L)).thenReturn(chatRoom)
            assert(!underTest.invoke(1L))
        }

    @Test
    fun `test that invoke() returns true when chat link is valid and has waiting room options`() =
        runBlocking {
            val chatRequest = Mockito.mock(ChatRequest::class.java)
            whenever(chatRequest.privilege).thenReturn(1)
            whenever(checkChatLink("validChatLink")).thenReturn(chatRequest)
            whenever(chatRepository.hasWaitingRoomChatOptions(1)).thenReturn(true)
            assert(underTest.invoke("validChatLink"))
        }

    @Test
    fun `test that invoke() returns false when chat link is valid but does not have waiting room options`() =
        runBlocking {
            val chatRequest = Mockito.mock(ChatRequest::class.java)
            whenever(chatRequest.privilege).thenReturn(1)
            whenever(checkChatLink("validChatLink")).thenReturn(chatRequest)
            whenever(chatRepository.hasWaitingRoomChatOptions(1)).thenReturn(false)
            assert(!underTest.invoke("validChatLink"))
        }

    @Test(expected = IllegalStateException::class)
    fun `test that exception is thrown when chat link is invalid`(): Unit = runBlocking {
        val chatRequest = Mockito.mock(ChatRequest::class.java)
        whenever(chatRequest.privilege).thenReturn(null)
        whenever(checkChatLink("invalidChatLink")).thenReturn(chatRequest)
        underTest.invoke("invalidChatLink")
    }
}
