package mega.privacy.android.data.mapper.chat

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.ChatRoomLastMessage
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.ChatListItem
import mega.privacy.android.domain.entity.chat.ChatListItemChanges
import nz.mega.sdk.MegaChatListItem
import nz.mega.sdk.MegaChatMessage
import nz.mega.sdk.MegaChatRoom
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.spy
import org.mockito.kotlin.mock

/**
 * Test class for [ChatListItemMapper]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ChatListItemMapperTest {
    private lateinit var underTest: ChatListItemMapper

    private val chatPermissionsMapper = spy(ChatPermissionsMapper())
    private val lastMessageTypeMapper = spy(LastMessageTypeMapper())
    private val chatListItemChangesMapper = spy(ChatListItemChangesMapper())

    @BeforeAll
    fun setUp() {
        underTest = ChatListItemMapper(
            chatPermissionsMapper = chatPermissionsMapper,
            lastMessageTypeMapper = lastMessageTypeMapper,
            chatListItemChangesMapper = chatListItemChangesMapper,
        )
    }

    @Test
    fun `test that a mega chat list item is mapped into a chat list item`() = runTest {
        val megaChatListItem = mock<MegaChatListItem> {
            on { chatId }.thenReturn(123456L)
            on { changes }.thenReturn(MegaChatListItem.CHANGE_TYPE_TITLE)
            on { title }.thenReturn("Sample Title")
            on { ownPrivilege }.thenReturn(MegaChatRoom.PRIV_STANDARD)
            on { unreadCount }.thenReturn(5)
            on { lastMessage }.thenReturn("Last Message")
            on { lastMessageId }.thenReturn(444444L)
            on { lastMessageType }.thenReturn(MegaChatMessage.TYPE_NORMAL)
            on { lastMessageSender }.thenReturn(789012L)
            on { lastTimestamp }.thenReturn(111111L)
            on { isGroup }.thenReturn(false)
            on { isPublic }.thenReturn(false)
            on { isPreview }.thenReturn(false)
            on { isActive }.thenReturn(false)
            on { isArchived }.thenReturn(false)
            on { isDeleted }.thenReturn(false)
            on { isCallInProgress }.thenReturn(false)
            on { peerHandle }.thenReturn(222222L)
            on { lastMessagePriv }.thenReturn(MegaChatRoom.PRIV_STANDARD)
            on { lastMessageHandle }.thenReturn(333333L)
            on { numPreviewers }.thenReturn(5)
        }
        val expected = ChatListItem(
            chatId = megaChatListItem.chatId,
            changes = ChatListItemChanges.Title,
            title = megaChatListItem.title,
            ownPrivilege = ChatRoomPermission.Standard,
            unreadCount = megaChatListItem.unreadCount,
            lastMessage = megaChatListItem.lastMessage,
            lastMessageId = megaChatListItem.lastMessageId,
            lastMessageType = ChatRoomLastMessage.Normal,
            lastMessageSender = megaChatListItem.lastMessageSender,
            lastTimestamp = megaChatListItem.lastTimestamp,
            isGroup = megaChatListItem.isGroup,
            isPublic = megaChatListItem.isPublic,
            isPreview = megaChatListItem.isPreview,
            isActive = megaChatListItem.isActive,
            isArchived = megaChatListItem.isArchived,
            isDeleted = megaChatListItem.isDeleted,
            isCallInProgress = megaChatListItem.isCallInProgress,
            peerHandle = megaChatListItem.peerHandle,
            lastMessagePriv = megaChatListItem.lastMessagePriv,
            lastMessageHandle = megaChatListItem.lastMessageHandle,
            numPreviewers = megaChatListItem.numPreviewers,
        )

        val actual = underTest(megaChatListItem)

        assertThat(actual).isEqualTo(expected)
    }
}