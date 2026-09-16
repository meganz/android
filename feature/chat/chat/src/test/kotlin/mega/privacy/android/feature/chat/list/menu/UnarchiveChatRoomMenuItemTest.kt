package mega.privacy.android.feature.chat.list.menu

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatRoomItem
import mega.privacy.android.domain.usecase.chat.ArchiveChatUseCase
import mega.privacy.android.feature.chat.list.mapper.ChatRoomTitleMapper
import mega.privacy.android.feature.chat.list.model.ChatRoomActionMessage
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UnarchiveChatRoomMenuItemTest {

    private val archiveChatUseCase = mock<ArchiveChatUseCase>()
    private val chatRoomTitleMapper = mock<ChatRoomTitleMapper>()

    private lateinit var underTest: UnarchiveChatRoomMenuItem

    @BeforeEach
    fun setUp() {
        underTest = UnarchiveChatRoomMenuItem(archiveChatUseCase, chatRoomTitleMapper)
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

        underTest.onClick(listOf(item))

        verify(archiveChatUseCase)(7L, false)
    }

    @Test
    fun `test that successMessage names the chat when a single chat is unarchived`() {
        val item = ChatRoomItem.GroupChatRoomItem(chatId = 7L, title = "Group")
        chatRoomTitleMapper.stub { on { invoke(item) } doReturn "Group" }

        assertThat(underTest.successMessage(listOf(item))).isEqualTo(
            ChatRoomActionMessage.Text(
                messageId = sharedR.string.success_unarchive_chat,
                args = listOf("Group"),
            )
        )
    }

    @Test
    fun `test that successMessage counts the chats when several chats are unarchived`() {
        val items = List(3) {
            ChatRoomItem.GroupChatRoomItem(chatId = it.toLong(), title = "Group $it")
        }

        assertThat(underTest.successMessage(items)).isEqualTo(
            ChatRoomActionMessage.Plural(
                messageId = sharedR.plurals.chat_list_chats_unarchived_message,
                quantity = 3,
            )
        )
    }
}
