package mega.privacy.android.feature.chat.list.menu

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.ChatRoomLastMessage
import mega.privacy.android.domain.entity.chat.ChatRoomItem
import mega.privacy.android.domain.usecase.chat.ClearChatHistoryUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClearHistoryChatRoomMenuItemTest {

    private val clearChatHistoryUseCase = mock<ClearChatHistoryUseCase>()

    private lateinit var underTest: ClearHistoryChatRoomMenuItem

    @BeforeEach
    fun setUp() {
        underTest = ClearHistoryChatRoomMenuItem(clearChatHistoryUseCase)
    }

    @Test
    fun `test that shouldDisplay is true for a chat with permissions`() = runTest {
        val item = ChatRoomItem.IndividualChatRoomItem(
            chatId = 1L,
            title = "Contact",
            hasPermissions = true,
        )

        assertThat(underTest.shouldDisplay(item)).isTrue()
    }

    @Test
    fun `test that shouldDisplay is false without permissions`() = runTest {
        val item = ChatRoomItem.IndividualChatRoomItem(
            chatId = 1L,
            title = "Contact",
            hasPermissions = false,
        )

        assertThat(underTest.shouldDisplay(item)).isFalse()
    }

    @Test
    fun `test that shouldDisplay is false for an archived chat`() = runTest {
        val item = ChatRoomItem.IndividualChatRoomItem(
            chatId = 1L,
            title = "Contact",
            hasPermissions = true,
            isArchived = true,
        )

        assertThat(underTest.shouldDisplay(item)).isFalse()
    }

    @Test
    fun `test that shouldDisplay is true for a non empty note to self chat`() = runTest {
        val item = ChatRoomItem.NoteToSelfChatRoomItem(
            chatId = 1L,
            title = "Note to self",
            hasPermissions = true,
            lastMessageType = ChatRoomLastMessage.Normal,
        )

        assertThat(underTest.shouldDisplay(item)).isTrue()
    }

    @Test
    fun `test that shouldDisplay is false for an empty note to self chat`() = runTest {
        val item = ChatRoomItem.NoteToSelfChatRoomItem(
            chatId = 1L,
            title = "Note to self",
            hasPermissions = true,
            lastMessageType = ChatRoomLastMessage.Unknown,
        )

        assertThat(underTest.shouldDisplay(item)).isFalse()
    }

    @Test
    fun `test that onClick clears the chat history`() = runTest {
        val item = ChatRoomItem.IndividualChatRoomItem(chatId = 7L, title = "Contact")

        underTest.onClick(item)

        verify(clearChatHistoryUseCase)(7L)
    }
}
