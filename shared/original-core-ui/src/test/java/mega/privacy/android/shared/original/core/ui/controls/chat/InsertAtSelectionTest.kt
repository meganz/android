package mega.privacy.android.shared.original.core.ui.controls.chat

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.text.TextRange
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class InsertAtSelectionTest {

    @Test
    fun `test that text is inserted at a collapsed cursor`() {
        val underTest = TextFieldState("hello world", TextRange(5))

        underTest.insertAtSelection("!")

        assertThat(underTest.text.toString()).isEqualTo("hello! world")
        assertThat(underTest.selection).isEqualTo(TextRange(6))
    }

    @Test
    fun `test that text replaces a forward selection`() {
        val underTest = TextFieldState("hello world", TextRange(6, 11))

        underTest.insertAtSelection("there")

        assertThat(underTest.text.toString()).isEqualTo("hello there")
        assertThat(underTest.selection).isEqualTo(TextRange(11))
    }

    @Test
    fun `test that text replaces a reversed selection`() {
        val underTest = TextFieldState("hello world", TextRange(11, 6))

        underTest.insertAtSelection("there")

        assertThat(underTest.text.toString()).isEqualTo("hello there")
        assertThat(underTest.selection).isEqualTo(TextRange(11))
    }

    @Test
    fun `test that text is appended when the cursor is at the end`() {
        val underTest = TextFieldState("hello", TextRange(5))

        underTest.insertAtSelection("!")

        assertThat(underTest.text.toString()).isEqualTo("hello!")
        assertThat(underTest.selection).isEqualTo(TextRange(6))
    }

    @Test
    fun `test that text is inserted into an empty field`() {
        val underTest = TextFieldState()

        underTest.insertAtSelection("hi")

        assertThat(underTest.text.toString()).isEqualTo("hi")
        assertThat(underTest.selection).isEqualTo(TextRange(2))
    }
}
