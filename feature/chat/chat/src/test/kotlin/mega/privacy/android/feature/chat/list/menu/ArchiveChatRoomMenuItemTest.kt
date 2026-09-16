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
class ArchiveChatRoomMenuItemTest {

    private val archiveChatUseCase = mock<ArchiveChatUseCase>()
    private val chatRoomTitleMapper = mock<ChatRoomTitleMapper>()

    private lateinit var underTest: ArchiveChatRoomMenuItem

    @BeforeEach
    fun setUp() {
        underTest = ArchiveChatRoomMenuItem(archiveChatUseCase, chatRoomTitleMapper)
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

        underTest.onClick(listOf(item))

        verify(archiveChatUseCase)(7L, true)
    }

    @Test
    fun `test that successMessage names the chat when a single chat is archived`() {
        val item = ChatRoomItem.IndividualChatRoomItem(chatId = 7L, title = "Contact")
        chatRoomTitleMapper.stub { on { invoke(item) } doReturn "Contact" }

        assertThat(underTest.successMessage(listOf(item))).isEqualTo(
            ChatRoomActionMessage.Text(
                messageId = sharedR.string.success_archive_chat,
                args = listOf("Contact"),
            )
        )
    }

    @Test
    fun `test that successMessage counts the chats when several chats are archived`() {
        val items = List(3) {
            ChatRoomItem.IndividualChatRoomItem(chatId = it.toLong(), title = "Contact $it")
        }

        assertThat(underTest.successMessage(items)).isEqualTo(
            ChatRoomActionMessage.Plural(
                messageId = sharedR.plurals.chat_list_chats_archived_message,
                quantity = 3,
            )
        )
    }
}
