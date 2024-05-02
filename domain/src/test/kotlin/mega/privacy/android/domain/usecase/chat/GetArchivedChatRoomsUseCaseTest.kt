package mega.privacy.android.domain.usecase.chat

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.ChatRoomLastMessage
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.CombinedChatRoom
import mega.privacy.android.domain.repository.ChatRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetArchivedChatRoomsUseCaseTest {

    private lateinit var underTest: GetArchivedChatRoomsUseCase

    private val chatRepository: ChatRepository = mock()

    @BeforeEach
    fun setUp() {
        underTest = GetArchivedChatRoomsUseCase(
            chatRepository = chatRepository
        )
    }

    @AfterEach
    fun tearDown() {
        reset(chatRepository)
    }

    @Test
    fun `test that the archived chat rooms is returned`() = runTest {
        val archivedChatRooms = listOf(
            CombinedChatRoom(
                chatId = -1L,
                title = "Meeting Room",
                isMeeting = true,
                ownPrivilege = ChatRoomPermission.Moderator,
                lastMessageType = ChatRoomLastMessage.VoiceClip,
                unreadCount = 5,
                isActive = true,
                isArchived = false,
                isPublic = false,
                isCallInProgress = false,
                lastTimestamp = 1629156234L
            )
        )
        whenever(chatRepository.getArchivedChatRooms()) doReturn archivedChatRooms

        val actual = underTest()

        assertThat(actual).isEqualTo(archivedChatRooms)
    }
}
