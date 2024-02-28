package mega.privacy.android.domain.usecase.chat

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.ChatRoom
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Get1On1ChatIdUseCaseTest {

    private lateinit var underTest: Get1On1ChatIdUseCase

    private val getChatRoomByUserUseCase: GetChatRoomByUserUseCase = mock()
    private val createChatRoomUseCase: CreateChatRoomUseCase = mock()

    @BeforeEach
    fun setup() {
        underTest = Get1On1ChatIdUseCase(
            getChatRoomByUserUseCase = getChatRoomByUserUseCase,
            createChatRoomUseCase = createChatRoomUseCase
        )
    }

    @Test
    fun `test that the existing chat ID is returned when the chat room exists`() = runTest {
        val userHandler = 123L
        val chatID = 456L
        whenever(
            getChatRoomByUserUseCase(userHandler)
        ).thenReturn(
            ChatRoom(
                chatId = chatID,
                ownPrivilege = ChatRoomPermission.Standard,
                numPreviewers = 0,
                peerPrivilegesByHandles = emptyMap(),
                peerCount = 0,
                peerHandlesList = emptyList(),
                peerPrivilegesList = emptyList(),
                isGroup = false,
                isPublic = false,
                isPreview = false,
                authorizationToken = null,
                title = "",
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
        )

        val actual = underTest(userHandler)

        assertThat(actual).isEqualTo(chatID)
    }

    @Test
    fun `test that a new chat room is created when the chat room doesn't exist`() = runTest {
        val userHandler = 123L
        whenever(getChatRoomByUserUseCase(userHandler)).thenReturn(null)

        underTest(userHandler)

        verify(
            createChatRoomUseCase
        ).invoke(
            isGroup = false,
            userHandles = listOf(userHandler)
        )
    }

    @Test
    fun `test that the new chat ID is returned when the chat room doesn't exist`() = runTest {
        val userHandler = 123L
        val chatID = 456L
        whenever(getChatRoomByUserUseCase(userHandler)).thenReturn(null)
        whenever(
            createChatRoomUseCase(
                isGroup = false,
                userHandles = listOf(userHandler)
            )
        ).thenReturn(chatID)

        val actual = underTest(userHandler)

        assertThat(actual).isEqualTo(chatID)
    }

    @AfterEach
    fun tearDown() {
        reset(
            getChatRoomByUserUseCase,
            createChatRoomUseCase
        )
    }
}
