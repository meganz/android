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
    fun `test that selectedItemsCount counts mega handles and phone and manual emails`() {
        val underTest = ContactSelectionState(
            initialSelectedHandles = setOf(1L, 2L),
            initialSelectedPhoneEmails = setOf("a@test.com"),
            initialSelectedManualEmails = setOf("m@test.com"),
        )

        assertThat(underTest.selectedItemsCount).isEqualTo(4)
    }

    @Test
    fun `test that selectManualEmail adds without deselecting existing selection`() {
        val underTest = ContactSelectionState(
            initialSelectedManualEmails = setOf("a@test.com"),
        )

        underTest.selectManualEmail("b@test.com")

        assertThat(underTest.selectedManualEmails)
            .containsExactly("a@test.com", "b@test.com")
    }

    @Test
    fun `test that removeManualEmail removes only the given email`() {
        val underTest = ContactSelectionState(
            initialSelectedManualEmails = setOf("a@test.com", "b@test.com"),
        )

        underTest.removeManualEmail("a@test.com")

        assertThat(underTest.selectedManualEmails).containsExactly("b@test.com")
    }

    @Test
    fun `test that isEmailSelected returns true when a manual email matches case-insensitively`() {
        val underTest = ContactSelectionState(
            initialSelectedManualEmails = setOf("Guest@Test.com"),
        )

        assertThat(underTest.isEmailSelected("guest@test.com")).isTrue()
    }

    @Test
    fun `test that isEmailSelected returns true when a phone email matches case-insensitively`() {
        val underTest = ContactSelectionState(
            initialSelectedPhoneEmails = setOf("phone@test.com"),
        )

        assertThat(underTest.isEmailSelected("PHONE@test.com")).isTrue()
    }

    @Test
    fun `test that isEmailSelected returns false when the email is not selected`() {
        val underTest = ContactSelectionState(
            initialSelectedPhoneEmails = setOf("phone@test.com"),
            initialSelectedManualEmails = setOf("manual@test.com"),
        )

        assertThat(underTest.isEmailSelected("other@test.com")).isFalse()
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
    fun `test that deselectAll clears mega and phone and manual selection`() {
        val underTest = ContactSelectionState(
            initialSelectedHandles = setOf(1L, 2L, 3L),
            initialSelectedPhoneEmails = setOf("a@test.com"),
            initialSelectedManualEmails = setOf("m@test.com"),
        )

        underTest.deselectAll()

        assertThat(underTest.selectedHandles).isEmpty()
        assertThat(underTest.selectedPhoneEmails).isEmpty()
        assertThat(underTest.selectedManualEmails).isEmpty()
        assertThat(underTest.selectedItemsCount).isEqualTo(0)
    }

    @Test
    fun `test that the saver round trips mega handles and phone and manual emails`() {
        val original = ContactSelectionState(
            initialSelectedHandles = setOf(1L, 2L, 3L),
            initialSelectedPhoneEmails = setOf("a@test.com", "b@test.com"),
            initialSelectedManualEmails = setOf("m@test.com"),
        )
        val saver = ContactSelectionState.Saver

        val saved = with(saver) { SaverScope { true }.save(original) }
        val restored = saved?.let { saver.restore(it) }

        assertThat(restored?.selectedHandles).isEqualTo(setOf(1L, 2L, 3L))
        assertThat(restored?.selectedPhoneEmails).isEqualTo(setOf("a@test.com", "b@test.com"))
        assertThat(restored?.selectedManualEmails).isEqualTo(setOf("m@test.com"))
    }
}
