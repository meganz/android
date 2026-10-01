package mega.privacy.android.domain.entity.chat

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.messages.normal.TextLinkMessage
import mega.privacy.android.domain.entity.chat.messages.normal.TextMessage
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import kotlin.time.Duration

/**
 * Chat entities are routinely interpolated into log statements, so their toString() must never
 * carry message content, chat titles or the last message preview.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChatEntityToStringTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("entities")
    fun `test that toString does not expose user content`(name: String, entity: Any) {
        val actual = entity.toString()

        assertThat(actual).doesNotContain(CONTENT)
        assertThat(actual).startsWith(name)
    }

    @Test
    fun `test that ChatMessage toString keeps the identifiers`() {
        val actual = chatMessage().toString()

        assertThat(actual).contains("messageId=1")
        assertThat(actual).contains("userHandle=3")
    }

    private fun entities(): Stream<Arguments> = listOf(
        chatMessage(),
        ChatListItem(chatId = 1L, title = CONTENT, lastMessage = CONTENT),
        ChatRoomItem.IndividualChatRoomItem(
            chatId = 1L,
            title = CONTENT,
            lastMessage = CONTENT,
            header = CONTENT,
            description = CONTENT,
        ),
        TextMessage(
            chatId = 1L,
            msgId = 2L,
            time = 3L,
            isMine = true,
            isDeletable = true,
            isEditable = true,
            userHandle = 4L,
            reactions = emptyList(),
            status = ChatMessageStatus.UNKNOWN,
            content = CONTENT,
            rowId = 5L,
            isEdited = false,
            hasOtherLink = false,
        ),
        TextLinkMessage(
            chatId = 1L,
            msgId = 2L,
            time = 3L,
            isDeletable = true,
            isEditable = true,
            isMine = true,
            userHandle = 4L,
            reactions = emptyList(),
            status = ChatMessageStatus.UNKNOWN,
            content = CONTENT,
            rowId = 5L,
            isEdited = false,
            links = emptyList(),
        ),
    ).map { Arguments.of(it::class.simpleName, it) }.stream()

    private fun chatMessage() = ChatMessage(
        status = ChatMessageStatus.UNKNOWN,
        messageId = 1L,
        tempId = 2L,
        msgIndex = 0,
        userHandle = 3L,
        type = ChatMessageType.NORMAL,
        hasConfirmedReactions = false,
        timestamp = 4L,
        content = CONTENT,
        isEdited = false,
        isDeleted = false,
        isEditable = true,
        isDeletable = true,
        isManagementMessage = false,
        handleOfAction = -1L,
        privilege = ChatRoomPermission.Unknown,
        code = ChatMessageCode.DECRYPTING,
        usersCount = 1L,
        userHandles = emptyList(),
        userNames = listOf(CONTENT),
        userEmails = listOf(CONTENT),
        nodeList = emptyList(),
        handleList = emptyList(),
        duration = Duration.ZERO,
        retentionTime = 0L,
        termCode = ChatMessageTermCode.ENDED,
        rowId = 5L,
        changes = emptyList(),
        containsMeta = null,
    )

    private companion object {
        const val CONTENT = "KRYPTON5567 secret message body"
    }
}
