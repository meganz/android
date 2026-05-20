package mega.privacy.android.feature.cloudexplorer.presentation.chatexplorer

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.chat.ChatStatus
import mega.privacy.android.shared.chats.model.ChatExplorerUiItem
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ChatExplorerUiStateTest {

    @Test
    fun `test that Data is empty when recents and others are both empty`() {
        val data = ChatExplorerUiState.Data(
            noteToSelf = null,
            recents = emptyList(),
            others = emptyList(),
        )

        assertThat(data.isEmpty).isTrue()
    }

    @Test
    fun `test that Data is not empty when recents has items`() {
        val data = ChatExplorerUiState.Data(
            noteToSelf = null,
            recents = listOf(groupChat(id = 1L)),
            others = emptyList(),
        )

        assertThat(data.isEmpty).isFalse()
    }

    @Test
    fun `test that Data is not empty when others has items`() {
        val data = ChatExplorerUiState.Data(
            noteToSelf = null,
            recents = emptyList(),
            others = listOf(groupChat(id = 1L)),
        )

        assertThat(data.isEmpty).isFalse()
    }

    @Test
    fun `test that Data is empty even when only noteToSelf is present`() {
        val data = ChatExplorerUiState.Data(
            noteToSelf = noteToSelf(id = 10L),
            recents = emptyList(),
            others = emptyList(),
        )

        assertThat(data.isEmpty).isTrue()
    }

    @Test
    fun `test that withSelected updates NoteToSelf isSelected flag`() {
        val item = noteToSelf(id = 10L, isSelected = false)

        val result = item.withSelected(true) as ChatExplorerUiItem.NoteToSelf

        assertThat(result.isSelected).isTrue()
    }

    @Test
    fun `test that withSelected updates GroupChat isSelected flag`() {
        val item = groupChat(id = 11L, isSelected = false)

        val result = item.withSelected(true) as ChatExplorerUiItem.GroupChat

        assertThat(result.isSelected).isTrue()
    }

    @Test
    fun `test that withSelected updates Meeting isSelected flag`() {
        val item = ChatExplorerUiItem.Meeting(
            id = 12L,
            title = "Weekly sync",
            participants = 3,
            isSelected = false,
            isEnabled = true,
            isArchived = false,
            lastTimestamp = 0L,
        )

        val result = item.withSelected(true) as ChatExplorerUiItem.Meeting

        assertThat(result.isSelected).isTrue()
    }

    @Test
    fun `test that withSelected updates OneToOneChat isSelected flag`() {
        val item = ChatExplorerUiItem.OneToOneChat(
            id = 13L,
            contactName = "Alice",
            primaryColor = Color.Unspecified,
            userStatus = ChatStatus.Offline,
            isSelected = false,
            isEnabled = true,
            isArchived = false,
            lastTimestamp = 0L,
        )

        val result = item.withSelected(true) as ChatExplorerUiItem.OneToOneChat

        assertThat(result.isSelected).isTrue()
    }

    @Test
    fun `test that withSelected updates Contact isSelected flag`() {
        val item = ChatExplorerUiItem.Contact(
            id = 14L,
            contactName = "Bob",
            userStatus = ChatStatus.Offline,
            primaryColor = Color.Unspecified,
            isSelected = false,
            isEnabled = true,
        )

        val result = item.withSelected(true) as ChatExplorerUiItem.Contact

        assertThat(result.isSelected).isTrue()
    }

    private fun noteToSelf(
        id: Long,
        isSelected: Boolean = false,
    ) = ChatExplorerUiItem.NoteToSelf(
        id = id,
        isHint = false,
        isSelected = isSelected,
        isEnabled = true,
        isArchived = false,
        lastTimestamp = 0L,
    )

    private fun groupChat(
        id: Long,
        isSelected: Boolean = false,
    ) = ChatExplorerUiItem.GroupChat(
        id = id,
        title = "Group $id",
        participants = 2,
        isSelected = isSelected,
        isEnabled = true,
        isArchived = false,
        lastTimestamp = 0L,
    )
}
