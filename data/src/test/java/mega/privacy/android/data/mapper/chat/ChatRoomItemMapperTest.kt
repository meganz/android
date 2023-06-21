package mega.privacy.android.data.mapper.chat

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.ChatRoomLastMessage
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.ChatRoomItem
import mega.privacy.android.domain.entity.chat.CombinedChatRoom
import org.junit.Before
import org.junit.Test

class ChatRoomItemMapperTest {

    private lateinit var underTest: ChatRoomItemMapper

    @Before
    fun setUp() {
        underTest = ChatRoomItemMapper()
    }

    @Test
    fun `test MeetingChatRoomItem mapper`() {
        val combinedChatRoom = CombinedChatRoom(
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

        val result = underTest.invoke(combinedChatRoom)

        assertThat(result).isInstanceOf(ChatRoomItem.MeetingChatRoomItem::class.java)
        assertThat(result.chatId).isEqualTo(-1L)
        assertThat(result.title).isEqualTo("Meeting Room")
        assertThat(result.isLastMessageVoiceClip).isEqualTo(true)
        assertThat(result.unreadCount).isEqualTo(5)
        assertThat(result.isActive).isEqualTo(true)
        assertThat(result.isArchived).isEqualTo(false)
        assertThat(result.hasPermissions).isEqualTo(true)
        assertThat(result.highlight).isEqualTo(true)
        assertThat(result.lastTimestamp).isEqualTo(1629156234L)
    }

    @Test
    fun `test IndividualChatRoomItem mapper`() {
        val combinedChatRoom = CombinedChatRoom(
            chatId = -1L,
            title = "John Doe",
            isMeeting = false,
            isGroup = false,
            ownPrivilege = ChatRoomPermission.Moderator,
            lastMessageType = ChatRoomLastMessage.VoiceClip,
            unreadCount = 2,
            isActive = true,
            isArchived = false,
            isPublic = false,
            isCallInProgress = true,
            lastTimestamp = 1629157345L,
            peerHandle = -1L
        )

        val result = underTest.invoke(combinedChatRoom)

        assertThat(result).isInstanceOf(ChatRoomItem.IndividualChatRoomItem::class.java)
        assertThat(result.chatId).isEqualTo(-1L)
        assertThat(result.title).isEqualTo("John Doe")
        assertThat(result.isLastMessageVoiceClip).isEqualTo(true)
        assertThat(result.unreadCount).isEqualTo(2)
        assertThat(result.isActive).isEqualTo(true)
        assertThat(result.isArchived).isEqualTo(false)
        assertThat(result.hasPermissions).isEqualTo(true)
        assertThat(result.highlight).isEqualTo(true)
        assertThat(result.lastTimestamp).isEqualTo(1629157345L)
    }

    @Test
    fun `test GroupChatRoomItem mapper`() {
        val combinedChatRoom = CombinedChatRoom(
            chatId = -1L,
            title = "Developers Group",
            isMeeting = false,
            isGroup = true,
            ownPrivilege = ChatRoomPermission.Moderator,
            lastMessageType = ChatRoomLastMessage.VoiceClip,
            unreadCount = 10,
            isActive = true,
            isArchived = false,
            isPublic = true,
            isCallInProgress = false,
            lastTimestamp = 1629158456L,
        )

        val result = underTest.invoke(combinedChatRoom)

        assertThat(result).isInstanceOf(ChatRoomItem.GroupChatRoomItem::class.java)
        assertThat(result.chatId).isEqualTo(-1L)
        assertThat(result.title).isEqualTo("Developers Group")
        assertThat(result.isLastMessageVoiceClip).isEqualTo(true)
        assertThat(result.unreadCount).isEqualTo(10)
        assertThat(result.isActive).isEqualTo(true)
        assertThat(result.isArchived).isEqualTo(false)
        assertThat(result.hasPermissions).isEqualTo(true)
        assertThat(result.highlight).isEqualTo(true)
        assertThat(result.lastTimestamp).isEqualTo(1629158456L)
    }
}
