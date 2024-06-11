package mega.privacy.android.data.mapper.chat

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.database.entity.ChatPendingChangesEntity
import mega.privacy.android.domain.entity.chat.ChatPendingChanges
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Test class for [ChatRoomPendingChangesModelMapper]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ChatRoomPendingChangesModelMapperTest {
    private lateinit var underTest: ChatRoomPendingChangesModelMapper

    @BeforeAll
    fun setUp() {
        underTest = ChatRoomPendingChangesModelMapper()
    }

    @Test
    fun `test that the chat room changes entity is mapped to a chat room pending changes object`() =
        runTest {
            val chatPendingChangesEntity = ChatPendingChangesEntity(
                chatId = 123456L,
                draftMessage = "Draft Message",
                editingMessageId = 789012L
            )

            val expected = ChatPendingChanges(
                chatId = chatPendingChangesEntity.chatId,
                draftMessage = chatPendingChangesEntity.draftMessage,
                editingMessageId = chatPendingChangesEntity.editingMessageId,
            )
            val actual = underTest(chatPendingChangesEntity)

            assertThat(actual).isEqualTo(expected)
        }
}