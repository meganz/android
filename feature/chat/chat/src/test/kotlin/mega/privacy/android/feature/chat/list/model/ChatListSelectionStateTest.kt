package mega.privacy.android.feature.chat.list.model

import androidx.compose.runtime.saveable.SaverScope
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class ChatListSelectionStateTest {

    @Test
    fun `test that a new state is not in selection mode`() {
        val underTest = ChatListSelectionState()

        assertThat(underTest.isInSelectionMode).isFalse()
        assertThat(underTest.selectedCount).isEqualTo(0)
    }

    @Test
    fun `test that toggling an unselected chat selects it and enters selection mode`() {
        val underTest = ChatListSelectionState()

        underTest.toggleSelection(1L)

        assertThat(underTest.selectedChatIds).containsExactly(1L)
        assertThat(underTest.isSelected(1L)).isTrue()
        assertThat(underTest.isInSelectionMode).isTrue()
        assertThat(underTest.selectedCount).isEqualTo(1)
    }

    @Test
    fun `test that toggling a selected chat deselects it`() {
        val underTest = ChatListSelectionState(initialSelectedChatIds = setOf(1L, 2L))

        underTest.toggleSelection(1L)

        assertThat(underTest.selectedChatIds).containsExactly(2L)
    }

    @Test
    fun `test that toggling the last selected chat leaves selection mode`() {
        val underTest = ChatListSelectionState(initialSelectedChatIds = setOf(1L))

        underTest.toggleSelection(1L)

        assertThat(underTest.isInSelectionMode).isFalse()
        assertThat(underTest.selectedChatIds).isEmpty()
    }

    @Test
    fun `test that selectAll replaces the current selection`() {
        val underTest = ChatListSelectionState(initialSelectedChatIds = setOf(1L))

        underTest.selectAll(setOf(2L, 3L, 4L))

        assertThat(underTest.selectedChatIds).containsExactly(2L, 3L, 4L)
    }

    @Test
    fun `test that deselectAll clears the selection`() {
        val underTest = ChatListSelectionState(initialSelectedChatIds = setOf(1L, 2L))

        underTest.deselectAll()

        assertThat(underTest.selectedChatIds).isEmpty()
        assertThat(underTest.isInSelectionMode).isFalse()
    }

    @Test
    fun `test that the saver round trips the selected chats`() {
        val original = ChatListSelectionState(initialSelectedChatIds = setOf(1L, 2L, 3L))
        val saver = ChatListSelectionState.Saver

        val saved = with(saver) { SaverScope { true }.save(original) }
        val restored = saved?.let { saver.restore(it) }

        assertThat(restored?.selectedChatIds).isEqualTo(setOf(1L, 2L, 3L))
    }
}
