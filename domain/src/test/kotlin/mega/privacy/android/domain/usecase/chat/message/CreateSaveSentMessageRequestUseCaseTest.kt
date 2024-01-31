package mega.privacy.android.domain.usecase.chat.message

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.ChatMessageChange
import mega.privacy.android.domain.entity.chat.ChatMessageCode
import mega.privacy.android.domain.entity.chat.ChatMessageStatus
import mega.privacy.android.domain.entity.chat.ChatMessageTermCode
import mega.privacy.android.domain.entity.chat.ChatMessageType
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

class CreateSaveSentMessageRequestUseCaseTest {
    private val underTest = CreateSaveSentMessageRequestUseCase()

    @Test
    fun `test that message has same temp and message id`() {
        val tempId = 123L
        val msgId = tempId + 1
        val chatMessage = createChatMessage(msgId, tempId)
        val actual = underTest(chatMessage)
        assertThat(actual.message.msgId).isEqualTo(tempId)
    }

    private fun createChatMessage(
        msgId: Long,
        tempId: Long,
    ) = ChatMessage(
        msgId = msgId,
        tempId = tempId,
        status = ChatMessageStatus.DELIVERED,
        msgIndex = 1,
        userHandle = 1,
        type = ChatMessageType.CALL_ENDED,
        hasConfirmedReactions = false,
        timestamp = 1,
        content = null,
        isEdited = false,
        isDeleted = false,
        isEditable = false,
        isDeletable = false,
        isManagementMessage = false,
        handleOfAction = 1,
        privilege = ChatRoomPermission.Moderator,
        code = ChatMessageCode.INVALID_FORMAT,
        usersCount = 1,
        userHandles = emptyList(),
        userNames = emptyList(),
        userEmails = emptyList(),
        nodeList = emptyList(),
        handleList = emptyList(),
        duration = 1.seconds,
        retentionTime = 1,
        termCode = ChatMessageTermCode.ENDED,
        rowId = 1,
        changes = listOf(ChatMessageChange.CONTENT, ChatMessageChange.ACCESS),
        containsMeta = null,
    )

}