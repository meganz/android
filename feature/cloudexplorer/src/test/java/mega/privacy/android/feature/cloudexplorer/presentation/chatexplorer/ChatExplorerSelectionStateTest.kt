package mega.privacy.android.feature.cloudexplorer.presentation.chatexplorer

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ChatExplorerSelectionStateTest {

    @Test
    fun `test that initial selection state is empty`() {
        val underTest = ChatExplorerSelectionState()

        assertThat(underTest.selectedChatIds).isEmpty()
        assertThat(underTest.isInSelectionMode).isFalse()
        assertThat(underTest.selectedItemsCount).isEqualTo(0)
    }

    @Test
    fun `test that initial selection state restores provided ids`() {
        val underTest = ChatExplorerSelectionState(initialSelectedIds = setOf(1L, 2L))

        assertThat(underTest.selectedChatIds).containsExactly(1L, 2L)
        assertThat(underTest.isInSelectionMode).isTrue()
        assertThat(underTest.selectedItemsCount).isEqualTo(2)
    }

    @Test
    fun `test that toggleSelection adds a missing chat id`() {
        val underTest = ChatExplorerSelectionState()

        underTest.toggleSelection(42L)

        assertThat(underTest.selectedChatIds).containsExactly(42L)
        assertThat(underTest.isInSelectionMode).isTrue()
    }

    @Test
    fun `test that toggleSelection removes an existing chat id`() {
        val underTest = ChatExplorerSelectionState(initialSelectedIds = setOf(42L))

        underTest.toggleSelection(42L)

        assertThat(underTest.selectedChatIds).isEmpty()
        assertThat(underTest.isInSelectionMode).isFalse()
    }

    @Test
    fun `test that toggleSelection toggles independently for each id`() {
        val underTest = ChatExplorerSelectionState()

        underTest.toggleSelection(1L)
        underTest.toggleSelection(2L)
        underTest.toggleSelection(1L)

        assertThat(underTest.selectedChatIds).containsExactly(2L)
    }

    @Test
    fun `test that deselectAll clears every selected chat id`() {
        val underTest = ChatExplorerSelectionState(initialSelectedIds = setOf(1L, 2L, 3L))

        underTest.deselectAll()

        assertThat(underTest.selectedChatIds).isEmpty()
        assertThat(underTest.isInSelectionMode).isFalse()
        assertThat(underTest.selectedItemsCount).isEqualTo(0)
    }

    @Test
    fun `test that Saver round trips selected chat ids`() {
        val state = ChatExplorerSelectionState(initialSelectedIds = setOf(1L, 2L))
        val saver = ChatExplorerSelectionState.Saver
        val saved = with(saver) { TestSaverScope.save(state) }
        val restored = saver.restore(saved!!)

        assertThat(restored?.selectedChatIds).containsExactly(1L, 2L)
    }

    private object TestSaverScope : androidx.compose.runtime.saveable.SaverScope {
        override fun canBeSaved(value: Any): Boolean = true
    }
}
