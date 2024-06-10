package mega.privacy.android.data.mapper.chat

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.database.entity.ChatPendingChangesEntity
import mega.privacy.android.domain.entity.chat.ChatPendingChanges
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Test class for [ChatRoomPendingChangesEntityMapper]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ChatRoomPendingChangesEntityMapperTest {
    private lateinit var underTest: ChatRoomPendingChangesEntityMapper

    @BeforeAll
    fun setUp() {
        underTest = ChatRoomPendingChangesEntityMapper()
    }

    @Test
    fun `test that the chat pending changes is mapped into chat pending changes entity`() =
        runTest {
            val chatPendingChanges = ChatPendingChanges(
                chatId = 123456L,
                draftMessage = "Draft Message",
                editingMessageId = 789012L,
            )

            val expected = ChatPendingChangesEntity(
                chatId = chatPendingChanges.chatId,
                draftMessage = chatPendingChanges.draftMessage,
                editingMessageId = chatPendingChanges.editingMessageId,
            )
            val actual = underTest(chatPendingChanges)

            assertThat(actual).isEqualTo(expected)
        }
}