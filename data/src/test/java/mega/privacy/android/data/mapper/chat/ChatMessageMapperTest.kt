package mega.privacy.android.data.mapper.chat

import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.ChatMessageCode
import mega.privacy.android.domain.entity.chat.ChatMessageStatus
import mega.privacy.android.domain.entity.chat.ChatMessageTermCode
import mega.privacy.android.domain.entity.chat.ChatMessageType
import nz.mega.sdk.MegaChatMessage
import nz.mega.sdk.MegaChatRoom
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChatMessageMapperTest {

    private lateinit var underTest: ChatMessageMapper
    private lateinit var chatPermissionsMapper: ChatPermissionsMapper

    @BeforeAll
    fun setup() {
        chatPermissionsMapper = ChatPermissionsMapper()
        underTest = ChatMessageMapper(chatPermissionsMapper)
    }

    @Test
    fun `test that chat message mapper returns correctly`() {
        val userHandle1 = 111L
        val userName1 = "userName1"
        val userEmail1 = "userEmail1"
        val userHandle2 = 222L
        val userName2 = "userName2"
        val userEmail2 = "userEmail2"
        val userHandle3 = 333L
        val userName3 = "userName3"
        val userEmail3 = "userEmail3"
        val megaChatMessage = mock<MegaChatMessage> {
            on { status }.thenReturn(MegaChatMessage.STATUS_DELIVERED)
            on { msgId }.thenReturn(1L)
            on { tempId }.thenReturn(-1L)
            on { msgIndex }.thenReturn(3)
            on { userHandle }.thenReturn(userHandle1)
            on { type }.thenReturn(MegaChatMessage.TYPE_CALL_ENDED)
            on { hasConfirmedReactions() }.thenReturn(true)
            on { timestamp }.thenReturn(2356L)
            on { content }.thenReturn(null)
            on { isEdited }.thenReturn(false)
            on { isDeleted }.thenReturn(false)
            on { isEditable }.thenReturn(false)
            on { isDeletable }.thenReturn(false)
            on { isManagementMessage }.thenReturn(false)
            on { privilege }.thenReturn(MegaChatRoom.PRIV_MODERATOR)
            on { code }.thenReturn(MegaChatMessage.INVALID_FORMAT)
            on { usersCount }.thenReturn(3)
            on { getUserHandle(0) }.thenReturn(userHandle1)
            on { getUserName(0) }.thenReturn(userName1)
            on { getUserEmail(0) }.thenReturn(userEmail1)
            on { getUserHandle(1) }.thenReturn(userHandle2)
            on { getUserName(1) }.thenReturn(userName2)
            on { getUserEmail(1) }.thenReturn(userEmail2)
            on { getUserHandle(2) }.thenReturn(userHandle3)
            on { getUserName(2) }.thenReturn(userName3)
            on { getUserEmail(2) }.thenReturn(userEmail3)
            on { duration }.thenReturn(24356)
            on { retentionTime }.thenReturn(2345)
            on { termCode }.thenReturn(MegaChatMessage.END_CALL_REASON_ENDED)
            on { rowId }.thenReturn(456)
        }
        val chatMessage = ChatMessage(
            status = ChatMessageStatus.DELIVERED,
            msgId = megaChatMessage.msgId,
            tempId = megaChatMessage.tempId,
            msgIndex = megaChatMessage.msgIndex,
            userHandle = megaChatMessage.userHandle,
            type = ChatMessageType.CALL_ENDED,
            hasConfirmedReactions = megaChatMessage.hasConfirmedReactions(),
            timestamp = megaChatMessage.timestamp,
            content = megaChatMessage.content.orEmpty(),
            isEdited = megaChatMessage.isEdited,
            isDeleted = megaChatMessage.isDeleted,
            isEditable = megaChatMessage.isEditable,
            isDeletable = megaChatMessage.isDeletable,
            isManagementMessage = megaChatMessage.isManagementMessage,
            privilege = ChatRoomPermission.Moderator,
            code = ChatMessageCode.INVALID_FORMAT,
            usersCount = megaChatMessage.usersCount,
            userHandles = listOf(userHandle1, userHandle2, userHandle3),
            userNames = listOf(userName1, userName2, userName3),
            userEmails = listOf(userEmail1, userEmail2, userEmail3),
            duration = megaChatMessage.duration,
            retentionTime = megaChatMessage.retentionTime,
            termCode = ChatMessageTermCode.ENDED,
            rowId = megaChatMessage.rowId
        )
        assertEquals(
            underTest.invoke(megaChatMessage),
            chatMessage
        )
    }
}