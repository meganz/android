package mega.privacy.android.feature.chat.list.menu

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatRoomItem
import mega.privacy.android.domain.usecase.chat.ArchiveChatUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UnarchiveChatRoomMenuItemTest {

    private val archiveChatUseCase = mock<ArchiveChatUseCase>()

    private lateinit var underTest: UnarchiveChatRoomMenuItem

    @BeforeEach
    fun setUp() {
        underTest = UnarchiveChatRoomMenuItem(archiveChatUseCase)
    }

    @Test
    fun `test that shouldDisplay is true for an archived chat`() = runTest {
        val item = ChatRoomItem.GroupChatRoomItem(
            chatId = 1L,
            title = "Group",
            isArchived = true,
        )

        assertThat(underTest.shouldDisplay(item)).isTrue()
    }

    @Test
    fun `test that shouldDisplay is false for a non archived chat`() = runTest {
        val item = ChatRoomItem.GroupChatRoomItem(
            chatId = 1L,
            title = "Group",
            isArchived = false,
        )

        assertThat(underTest.shouldDisplay(item)).isFalse()
    }

    @Test
    fun `test that onClick unarchives the chat`() = runTest {
        val item = ChatRoomItem.GroupChatRoomItem(chatId = 7L, title = "Group")

        underTest.onClick(item)

        verify(archiveChatUseCase)(7L, false)
    }
}
