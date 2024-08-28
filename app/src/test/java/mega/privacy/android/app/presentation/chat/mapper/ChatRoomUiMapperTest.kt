package mega.privacy.android.app.presentation.chat.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.presentation.chat.model.ChatRoomUiState
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import mega.privacy.android.app.presentation.meeting.model.newChatRoom

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChatRoomUiMapperTest {

    private lateinit var underTest: ChatRoomUiMapper

    @BeforeEach
    fun setUp() {
        underTest = ChatRoomUiMapper()
    }

    @Test
    fun `test that the chat room is successfully mapped into chat room ui state`() {
        val chatRoom = newChatRoom()

        val actual = underTest(chatRoom)

        val expected = ChatRoomUiState(
            chatId = chatRoom.chatId,
            ownPrivilege = chatRoom.ownPrivilege,
            numPreviewers = chatRoom.numPreviewers,
            peerPrivilegesByHandles = chatRoom.peerPrivilegesByHandles,
            peerCount = chatRoom.peerCount,
            peerHandlesList = chatRoom.peerHandlesList,
            peerPrivilegesList = chatRoom.peerPrivilegesList,
            isGroup = chatRoom.isGroup,
            isPublic = chatRoom.isPublic,
            isPreview = chatRoom.isPreview,
            authorizationToken = chatRoom.authorizationToken,
            title = chatRoom.title,
            hasCustomTitle = chatRoom.hasCustomTitle,
            unreadCount = chatRoom.unreadCount,
            userTyping = chatRoom.userTyping,
            userHandle = chatRoom.userHandle,
            isActive = chatRoom.isActive,
            isArchived = chatRoom.isArchived,
            retentionTime = chatRoom.retentionTime,
            creationTime = chatRoom.creationTime,
            isMeeting = chatRoom.isMeeting,
            isWaitingRoom = chatRoom.isWaitingRoom,
            isOpenInvite = chatRoom.isOpenInvite,
            isSpeakRequest = chatRoom.isSpeakRequest,
            changes = chatRoom.changes
        )
        assertThat(actual).isEqualTo(expected)
    }
}
