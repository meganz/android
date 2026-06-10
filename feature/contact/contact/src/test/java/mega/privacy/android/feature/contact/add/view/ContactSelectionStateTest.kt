package mega.privacy.android.feature.contact.add.view

import androidx.compose.runtime.saveable.SaverScope
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class ContactSelectionStateTest {

    @Test
    fun `test that toggling an unselected handle selects it`() {
        val underTest = ContactSelectionState()

        underTest.toggleSelection(1L)

        assertThat(underTest.selectedHandles).containsExactly(1L)
        assertThat(underTest.selectedItemsCount).isEqualTo(1)
    }

    @Test
    fun `test that toggling a selected handle deselects it`() {
        val underTest = ContactSelectionState(initialSelectedHandles = setOf(1L, 2L))

        underTest.toggleSelection(1L)

        assertThat(underTest.selectedHandles).containsExactly(2L)
    }

    @Test
    fun `test that deselectAll clears the selection`() {
        val underTest = ContactSelectionState(initialSelectedHandles = setOf(1L, 2L, 3L))

        underTest.deselectAll()

        assertThat(underTest.selectedHandles).isEmpty()
        assertThat(underTest.selectedItemsCount).isEqualTo(0)
    }

    @Test
    fun `test that the saver round trips the selected handles`() {
        val original = ContactSelectionState(initialSelectedHandles = setOf(1L, 2L, 3L))
        val saver = ContactSelectionState.Saver

        val saved = with(saver) { SaverScope { true }.save(original) }
        val restored = saved?.let { saver.restore(it) }

        assertThat(restored?.selectedHandles).isEqualTo(setOf(1L, 2L, 3L))
    }
}
