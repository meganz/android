package mega.privacy.android.domain.usecase.chat

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.ChatRoomLastMessage
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.CombinedChatRoom
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HasArchivedChatsUseCaseTest {

    private lateinit var underTest: HasArchivedChatsUseCase

    private val getArchivedChatRoomsUseCase: GetArchivedChatRoomsUseCase = mock()

    @BeforeEach
    fun setUp() {
        underTest = HasArchivedChatsUseCase(
            getArchivedChatRoomsUseCase = getArchivedChatRoomsUseCase
        )
    }

    @AfterEach
    fun tearDown() {
        reset(getArchivedChatRoomsUseCase)
    }

    @Test
    fun `test that true is returned when the archived chat rooms is not empty`() = runTest {
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
        whenever(getArchivedChatRoomsUseCase()) doReturn archivedChatRooms

        val actual = underTest()

        assertThat(actual).isTrue()
    }

    @Test
    fun `test that false is returned when the archived chat rooms is empty`() = runTest {
        whenever(getArchivedChatRoomsUseCase()) doReturn emptyList()

        val actual = underTest()

        assertThat(actual).isFalse()
    }
}
