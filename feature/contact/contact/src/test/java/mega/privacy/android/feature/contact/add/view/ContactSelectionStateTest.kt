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
    fun `test that toggling an unselected phone email selects it`() {
        val underTest = ContactSelectionState()

        underTest.togglePhoneSelection("a@test.com")

        assertThat(underTest.selectedPhoneEmails).containsExactly("a@test.com")
        assertThat(underTest.selectedItemsCount).isEqualTo(1)
    }

    @Test
    fun `test that toggling a selected phone email deselects it`() {
        val underTest = ContactSelectionState(
            initialSelectedPhoneEmails = setOf("a@test.com", "b@test.com"),
        )

        underTest.togglePhoneSelection("a@test.com")

        assertThat(underTest.selectedPhoneEmails).containsExactly("b@test.com")
    }

    @Test
    fun `test that selectedItemsCount counts both mega handles and phone emails`() {
        val underTest = ContactSelectionState(
            initialSelectedHandles = setOf(1L, 2L),
            initialSelectedPhoneEmails = setOf("a@test.com"),
        )

        assertThat(underTest.selectedItemsCount).isEqualTo(3)
    }

    @Test
    fun `test that selectPhoneEmails adds without deselecting existing selection`() {
        val underTest = ContactSelectionState(
            initialSelectedPhoneEmails = setOf("a@test.com"),
        )

        underTest.selectPhoneEmails(listOf("b@test.com", "c@test.com"))

        assertThat(underTest.selectedPhoneEmails)
            .containsExactly("a@test.com", "b@test.com", "c@test.com")
    }

    @Test
    fun `test that deselectAll clears both mega and phone selection`() {
        val underTest = ContactSelectionState(
            initialSelectedHandles = setOf(1L, 2L, 3L),
            initialSelectedPhoneEmails = setOf("a@test.com"),
        )

        underTest.deselectAll()

        assertThat(underTest.selectedHandles).isEmpty()
        assertThat(underTest.selectedPhoneEmails).isEmpty()
        assertThat(underTest.selectedItemsCount).isEqualTo(0)
    }

    @Test
    fun `test that the saver round trips both mega handles and phone emails`() {
        val original = ContactSelectionState(
            initialSelectedHandles = setOf(1L, 2L, 3L),
            initialSelectedPhoneEmails = setOf("a@test.com", "b@test.com"),
        )
        val saver = ContactSelectionState.Saver

        val saved = with(saver) { SaverScope { true }.save(original) }
        val restored = saved?.let { saver.restore(it) }

        assertThat(restored?.selectedHandles).isEqualTo(setOf(1L, 2L, 3L))
        assertThat(restored?.selectedPhoneEmails).isEqualTo(setOf("a@test.com", "b@test.com"))
    }
}
