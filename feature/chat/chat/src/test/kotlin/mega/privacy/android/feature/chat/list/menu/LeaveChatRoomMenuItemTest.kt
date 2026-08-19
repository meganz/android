package mega.privacy.android.feature.chat.list.menu

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatRoomItem
import mega.privacy.android.domain.usecase.chat.LeaveChatUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LeaveChatRoomMenuItemTest {

    private val leaveChatUseCase = mock<LeaveChatUseCase>()

    private lateinit var underTest: LeaveChatRoomMenuItem

    @BeforeEach
    fun setUp() {
        underTest = LeaveChatRoomMenuItem(leaveChatUseCase)
    }

    @Test
    fun `test that the action is destructive`() {
        assertThat(underTest.isDestructive).isTrue()
    }

    @Test
    fun `test that shouldDisplay is true for an active group`() = runTest {
        val item = ChatRoomItem.GroupChatRoomItem(
            chatId = 1L,
            title = "Group",
            isActive = true,
        )

        assertThat(underTest.shouldDisplay(item)).isTrue()
    }

    @Test
    fun `test that shouldDisplay is false for an inactive group`() = runTest {
        val item = ChatRoomItem.GroupChatRoomItem(
            chatId = 1L,
            title = "Group",
            isActive = false,
        )

        assertThat(underTest.shouldDisplay(item)).isFalse()
    }

    @Test
    fun `test that shouldDisplay is false for an archived active group`() = runTest {
        val item = ChatRoomItem.GroupChatRoomItem(
            chatId = 1L,
            title = "Group",
            isActive = true,
            isArchived = true,
        )

        assertThat(underTest.shouldDisplay(item)).isFalse()
    }

    @Test
    fun `test that shouldDisplay is false for an individual chat`() = runTest {
        val item = ChatRoomItem.IndividualChatRoomItem(
            chatId = 1L,
            title = "Contact",
            hasPermissions = true,
        )

        assertThat(underTest.shouldDisplay(item)).isFalse()
    }

    @Test
    fun `test that shouldDisplay is false for a meeting`() = runTest {
        val item = ChatRoomItem.MeetingChatRoomItem(
            chatId = 1L,
            title = "Meeting",
            hasPermissions = true,
            isActive = true,
        )

        assertThat(underTest.shouldDisplay(item)).isFalse()
    }

    @Test
    fun `test that onClick leaves the chat`() = runTest {
        val item = ChatRoomItem.GroupChatRoomItem(chatId = 7L, title = "Group", isActive = true)

        underTest.onClick(item)

        verify(leaveChatUseCase)(7L)
    }
}
