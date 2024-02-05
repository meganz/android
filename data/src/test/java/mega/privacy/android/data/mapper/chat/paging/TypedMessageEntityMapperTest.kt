package mega.privacy.android.data.mapper.chat.paging

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.ChatMessageChange
import mega.privacy.android.domain.entity.chat.ChatMessageCode
import mega.privacy.android.domain.entity.chat.ChatMessageStatus
import mega.privacy.android.domain.entity.chat.ChatMessageTermCode
import mega.privacy.android.domain.entity.chat.ChatMessageType
import mega.privacy.android.domain.entity.chat.ContainsMeta
import mega.privacy.android.domain.entity.chat.ContainsMetaType
import mega.privacy.android.domain.entity.chat.messages.reactions.Reaction
import mega.privacy.android.domain.entity.chat.messages.request.CreateTypedMessageRequest
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import kotlin.time.Duration.Companion.seconds

class TypedMessageEntityMapperTest {
    private val underTest = TypedMessageEntityMapper()

    @Test
    internal fun `test that properties are mapped correctly`() {
        val expectedChatId = 45645L
        val expectedStatus = ChatMessageStatus.SENDING
        val expectedMsgId = 1L
        val expectedTempId = 2L
        val expectedMsgIndex = 1
        val expectedUserHandle = 123L
        val expectedType = ChatMessageType.NORMAL
        val expectedHasConfirmedReactions = false
        val expectedTimestamp = 65443L
        val expectedContent = "content"
        val expectedIsEdited = false
        val expectedIsDeleted = false
        val expectedIsEditable = false
        val expectedIsDeletable = false
        val expectedIsManagementMessage = false
        val expectedHandleOfAction = 9876L
        val expectedPrivilege = ChatRoomPermission.Moderator
        val expectedCode = ChatMessageCode.DECRYPTING
        val expectedUsersCount = 23L
        val expectedUserHandles = listOf(5432L)
        val expectedUserNames = listOf("userNames")
        val expectedUserEmails = listOf("userEmails")
        val expectedHandleList = listOf(1234L)
        val expectedDuration = 1.seconds
        val expectedRetentionTime = 165432L
        val expectedTermCode = ChatMessageTermCode.ENDED
        val expectedRowId = 123435424L
        val expectedChanges = listOf(ChatMessageChange.CONTENT)
        val expectedShouldShowAvatar = false
        val expectedShouldShowTime = false
        val expectedShouldShowDate = false
        val expectedIsMine = false
        val expectedTextMessage = "textMessage"
        val expectedReactions = emptyList<Reaction>()

        val chatMessage = mock<ChatMessage> {
            on { status } doReturn expectedStatus
            on { msgId } doReturn expectedMsgId
            on { tempId } doReturn expectedTempId
            on { msgIndex } doReturn expectedMsgIndex
            on { userHandle } doReturn expectedUserHandle
            on { type } doReturn expectedType
            on { hasConfirmedReactions } doReturn expectedHasConfirmedReactions
            on { timestamp } doReturn expectedTimestamp
            on { content } doReturn expectedContent
            on { isEdited } doReturn expectedIsEdited
            on { isDeleted } doReturn expectedIsDeleted
            on { isEditable } doReturn expectedIsEditable
            on { isDeletable } doReturn expectedIsDeletable
            on { isManagementMessage } doReturn expectedIsManagementMessage
            on { handleOfAction } doReturn expectedHandleOfAction
            on { privilege } doReturn expectedPrivilege
            on { code } doReturn expectedCode
            on { usersCount } doReturn expectedUsersCount
            on { userHandles } doReturn expectedUserHandles
            on { userNames } doReturn expectedUserNames
            on { userEmails } doReturn expectedUserEmails
            on { handleList } doReturn expectedHandleList
            on { duration } doReturn expectedDuration
            on { retentionTime } doReturn expectedRetentionTime
            on { termCode } doReturn expectedTermCode
            on { rowId } doReturn expectedRowId
            on { changes } doReturn expectedChanges
            on { containsMeta } doReturn ContainsMeta(
                type = ContainsMetaType.RICH_PREVIEW,
                textMessage = expectedTextMessage,
                richPreview = null,
                geolocation = null,
                giphy = null,
            )
        }

        val requestResolver = CreateTypedMessageRequest(
            chatMessage = chatMessage,
            shouldShowAvatar = expectedShouldShowAvatar,
            shouldShowTime = expectedShouldShowTime,
            shouldShowDate = expectedShouldShowDate,
            isMine = expectedIsMine,
            reactions = expectedReactions,
        )

        val actual = underTest(requestResolver, expectedChatId)

        assertThat(actual.chatId).isEqualTo(expectedChatId)
        assertThat(actual.status).isEqualTo(expectedStatus)
        assertThat(actual.msgId).isEqualTo(expectedMsgId)
        assertThat(actual.tempId).isEqualTo(expectedTempId)
        assertThat(actual.msgIndex).isEqualTo(expectedMsgIndex)
        assertThat(actual.userHandle).isEqualTo(expectedUserHandle)
        assertThat(actual.type).isEqualTo(expectedType)
        assertThat(actual.hasConfirmedReactions).isEqualTo(expectedHasConfirmedReactions)
        assertThat(actual.timestamp).isEqualTo(expectedTimestamp)
        assertThat(actual.content).isEqualTo(expectedContent)
        assertThat(actual.isEdited).isEqualTo(expectedIsEdited)
        assertThat(actual.isDeleted).isEqualTo(expectedIsDeleted)
        assertThat(actual.isEditable).isEqualTo(expectedIsEditable)
        assertThat(actual.isDeletable).isEqualTo(expectedIsDeletable)
        assertThat(actual.isManagementMessage).isEqualTo(expectedIsManagementMessage)
        assertThat(actual.handleOfAction).isEqualTo(expectedHandleOfAction)
        assertThat(actual.privilege).isEqualTo(expectedPrivilege)
        assertThat(actual.code).isEqualTo(expectedCode)
        assertThat(actual.usersCount).isEqualTo(expectedUsersCount)
        assertThat(actual.userHandles).isEqualTo(expectedUserHandles)
        assertThat(actual.userNames).isEqualTo(expectedUserNames)
        assertThat(actual.userEmails).isEqualTo(expectedUserEmails)
        assertThat(actual.handleList).isEqualTo(expectedHandleList)
        assertThat(actual.duration).isEqualTo(expectedDuration)
        assertThat(actual.retentionTime).isEqualTo(expectedRetentionTime)
        assertThat(actual.termCode).isEqualTo(expectedTermCode)
        assertThat(actual.rowId).isEqualTo(expectedRowId)
        assertThat(actual.changes).isEqualTo(expectedChanges)
        assertThat(actual.shouldShowAvatar).isEqualTo(expectedShouldShowAvatar)
        assertThat(actual.shouldShowTime).isEqualTo(expectedShouldShowTime)
        assertThat(actual.shouldShowDate).isEqualTo(expectedShouldShowDate)
        assertThat(actual.isMine).isEqualTo(expectedIsMine)
        assertThat(actual.textMessage).isEqualTo(expectedTextMessage)
        assertThat(actual.reactions).isEqualTo(expectedReactions)
    }
}