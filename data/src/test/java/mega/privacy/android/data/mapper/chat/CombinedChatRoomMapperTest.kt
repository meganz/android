package mega.privacy.android.data.mapper.chat

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.ChatRoomLastMessage
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.ChatRoomChange
import mega.privacy.android.domain.entity.chat.CombinedChatRoom
import nz.mega.sdk.MegaChatListItem
import nz.mega.sdk.MegaChatMessage
import nz.mega.sdk.MegaChatRoom
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.spy
import org.mockito.kotlin.mock

/**
 * Test class for [CombinedChatRoomMapper]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CombinedChatRoomMapperTest {
    private lateinit var underTest: CombinedChatRoomMapper

    private val chatPermissionsMapper = spy(ChatPermissionsMapper())
    private val lastMessageTypeMapper = spy(LastMessageTypeMapper())
    private val chatRoomChangesMapper = spy(ChatRoomChangesMapper())

    @BeforeAll
    fun setUp() {
        underTest = CombinedChatRoomMapper(
            chatPermissionsMapper = chatPermissionsMapper,
            lastMessageTypeMapper = lastMessageTypeMapper,
            chatRoomChangesMapper = chatRoomChangesMapper,
        )
    }

    @Test
    fun `test that both mega chat room and mega chat list item are mapped into a combined chat room`() =
        runTest {
            val megaChatRoom = mock<MegaChatRoom> {
                on { chatId }.thenReturn(123456L)
                on { changes }.thenReturn(MegaChatRoom.CHANGE_TYPE_ARCHIVE)
                on { title }.thenReturn("Chat Room")
                on { hasCustomTitle() }.thenReturn(false)
                on { ownPrivilege }.thenReturn(MegaChatRoom.PRIV_STANDARD)
                on { peerCount }.thenReturn(10)
                on { isGroup }.thenReturn(false)
                on { isPublic }.thenReturn(false)
                on { isPreview }.thenReturn(false)
                on { isArchived }.thenReturn(false)
                on { isActive }.thenReturn(false)
                on { retentionTime }.thenReturn(2000L)
                on { isMeeting }.thenReturn(false)
                on { isWaitingRoom }.thenReturn(false)
                on { isOpenInvite }.thenReturn(false)
                on { isSpeakRequest }.thenReturn(false)
            }
            val megaChatListItem = mock<MegaChatListItem> {
                on { unreadCount }.thenReturn(0)
                on { lastMessage }.thenReturn("Last Message")
                on { lastMessageId }.thenReturn(3000L)
                on { lastMessageType }.thenReturn(MegaChatMessage.TYPE_NORMAL)
                on { lastMessageSender }.thenReturn(4000L)
                on { lastTimestamp }.thenReturn(565656L)
                on { isDeleted }.thenReturn(false)
                on { isCallInProgress }.thenReturn(false)
                on { peerHandle }.thenReturn(232323L)
                on { lastMessagePriv }.thenReturn(10)
                on { lastMessageHandle }.thenReturn(98765L)
                on { numPreviewers }.thenReturn(0)
            }

            val expected = CombinedChatRoom(
                chatId = megaChatRoom.chatId,
                changes = listOf(ChatRoomChange.Archive),
                title = megaChatRoom.title,
                hasCustomTitle = megaChatRoom.hasCustomTitle(),
                ownPrivilege = ChatRoomPermission.Standard,
                unreadCount = megaChatListItem.unreadCount,
                lastMessage = megaChatListItem.lastMessage,
                lastMessageId = megaChatListItem.lastMessageId,
                lastMessageType = ChatRoomLastMessage.Normal,
                lastMessageSender = megaChatListItem.lastMessageSender,
                lastTimestamp = megaChatListItem.lastTimestamp,
                peerCount = megaChatRoom.peerCount,
                isGroup = megaChatRoom.isGroup,
                isPublic = megaChatRoom.isPublic,
                isPreview = megaChatRoom.isPreview,
                isArchived = megaChatRoom.isArchived,
                isActive = megaChatRoom.isActive,
                isDeleted = megaChatListItem.isDeleted,
                isCallInProgress = megaChatListItem.isCallInProgress,
                peerHandle = megaChatListItem.peerHandle,
                lastMessagePriv = megaChatListItem.lastMessagePriv,
                lastMessageHandle = megaChatListItem.lastMessageHandle,
                numPreviewers = megaChatListItem.numPreviewers,
                retentionTime = megaChatRoom.retentionTime,
                isMeeting = megaChatRoom.isMeeting,
                isWaitingRoom = megaChatRoom.isWaitingRoom,
                isOpenInvite = megaChatRoom.isOpenInvite,
                isSpeakRequest = megaChatRoom.isSpeakRequest,
            )
            val actual = underTest(megaChatRoom = megaChatRoom, megaChatListItem = megaChatListItem)

            assertThat(actual).isEqualTo(expected)
        }
}