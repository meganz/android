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
class ArchiveChatRoomMenuItemTest {

    private val archiveChatUseCase = mock<ArchiveChatUseCase>()

    private lateinit var underTest: ArchiveChatRoomMenuItem

    @BeforeEach
    fun setUp() {
        underTest = ArchiveChatRoomMenuItem(archiveChatUseCase)
    }

    @Test
    fun `test that shouldDisplay is true for a non archived chat`() = runTest {
        val item = ChatRoomItem.IndividualChatRoomItem(
            chatId = 1L,
            title = "Contact",
            isArchived = false,
        )

        assertThat(underTest.shouldDisplay(item)).isTrue()
    }

    @Test
    fun `test that shouldDisplay is false for an archived chat`() = runTest {
        val item = ChatRoomItem.IndividualChatRoomItem(
            chatId = 1L,
            title = "Contact",
            isArchived = true,
        )

        assertThat(underTest.shouldDisplay(item)).isFalse()
    }

    @Test
    fun `test that onClick archives the chat`() = runTest {
        val item = ChatRoomItem.IndividualChatRoomItem(chatId = 7L, title = "Contact")

        underTest.onClick(item)

        verify(archiveChatUseCase)(7L, true)
    }
}
