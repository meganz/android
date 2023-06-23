package mega.privacy.android.data.mapper.chat

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.entity.chat.ChatRoomChange
import nz.mega.sdk.MegaChatRoom
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import kotlin.random.Random.Default.nextLong

@TestInstance(
    TestInstance.Lifecycle.PER_CLASS
)
class ChatRoomMapperTest {

    private lateinit var underTest: ChatRoomMapper
    private lateinit var chatPermissionsMapper: ChatPermissionsMapper
    private lateinit var chatRoomChangesMapper: ChatRoomChangesMapper

    @BeforeAll
    fun setup() {
        chatPermissionsMapper = ChatPermissionsMapper()
        chatRoomChangesMapper = ChatRoomChangesMapper()
        underTest = ChatRoomMapper(chatPermissionsMapper, chatRoomChangesMapper)
    }

    @Test
    fun `test that chat room mapper returns correctly`() {
        val userHandle1 = 111L
        val userHandle2 = 222L
        val userHandle3 = 222L
        val megaChatRoom = mock<MegaChatRoom> {
            on { chatId }.thenReturn(444L)
            on { ownPrivilege }.thenReturn(MegaChatRoom.PRIV_MODERATOR)
            on { numPreviewers }.thenReturn(2)
            on { getPeerPrivilegeByHandle(userHandle1) }.thenReturn(MegaChatRoom.PRIV_RM)
            on { getPeerPrivilegeByHandle(userHandle2) }.thenReturn(MegaChatRoom.PRIV_STANDARD)
            on { getPeerPrivilegeByHandle(userHandle3) }.thenReturn(MegaChatRoom.PRIV_RO)
            on { peerCount }.thenReturn(3)
            on { getPeerHandle(0) }.thenReturn(userHandle1)
            on { getPeerHandle(1) }.thenReturn(userHandle2)
            on { getPeerHandle(2) }.thenReturn(userHandle3)
            on { getPeerPrivilege(0) }.thenReturn(MegaChatRoom.PRIV_RM)
            on { getPeerPrivilege(1) }.thenReturn(MegaChatRoom.PRIV_STANDARD)
            on { getPeerPrivilege(2) }.thenReturn(MegaChatRoom.PRIV_RO)
            on { isGroup }.thenReturn(true)
            on { isPublic }.thenReturn(true)
            on { isPreview }.thenReturn(false)
            on { authorizationToken }.thenReturn("authorizationToken")
            on { title }.thenReturn("title")
            on { hasCustomTitle() }.thenReturn(true)
            on { unreadCount }.thenReturn(0)
            on { userTyping }.thenReturn(userHandle1)
            on { userHandle }.thenReturn(userHandle3)
            on { isActive }.thenReturn(true)
            on { isArchived }.thenReturn(false)
            on { retentionTime }.thenReturn(nextLong())
            on { creationTs }.thenReturn(nextLong())
            on { isMeeting }.thenReturn(true)
            on { isWaitingRoom }.thenReturn(false)
            on { isOpenInvite }.thenReturn(false)
            on { isSpeakRequest }.thenReturn(false)
            on { changes }.thenReturn(MegaChatRoom.CHANGE_TYPE_CHAT_MODE + MegaChatRoom.CHANGE_TYPE_UPDATE_PREVIEWERS)

        }
        val chatRoom = ChatRoom(
            chatId = megaChatRoom.chatId,
            ownPrivilege = ChatRoomPermission.Moderator,
            numPreviewers = megaChatRoom.numPreviewers,
            peerPrivilegesByHandles = mapOf(
                userHandle1 to ChatRoomPermission.Removed,
                userHandle2 to ChatRoomPermission.Standard,
                userHandle3 to ChatRoomPermission.ReadOnly
            ),
            peerCount = megaChatRoom.peerCount,
            peerHandlesList = listOf(userHandle1, userHandle2, userHandle3),
            peerPrivilegesList = listOf(
                ChatRoomPermission.Removed,
                ChatRoomPermission.Standard,
                ChatRoomPermission.ReadOnly
            ),
            isGroup = megaChatRoom.isGroup,
            isPublic = megaChatRoom.isPublic,
            isPreview = megaChatRoom.isPreview,
            authorizationToken = megaChatRoom.authorizationToken,
            title = megaChatRoom.title,
            hasCustomTitle = megaChatRoom.hasCustomTitle(),
            unreadCount = megaChatRoom.unreadCount,
            userTyping = megaChatRoom.userTyping,
            userHandle = megaChatRoom.userHandle,
            isActive = megaChatRoom.isActive,
            isArchived = megaChatRoom.isArchived,
            retentionTime = megaChatRoom.retentionTime,
            creationTime = megaChatRoom.creationTs,
            isMeeting = megaChatRoom.isMeeting,
            isWaitingRoom = megaChatRoom.isWaitingRoom,
            isOpenInvite = megaChatRoom.isOpenInvite,
            isSpeakRequest = megaChatRoom.isSpeakRequest,
            changes = listOf(ChatRoomChange.ChatMode, ChatRoomChange.UpdatePreviewers),
        )
        Truth.assertThat(underTest.invoke(megaChatRoom)).isEqualTo(chatRoom)
    }
}