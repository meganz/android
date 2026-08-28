package mega.privacy.android.feature.texteditor.components.markdown

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.text.TextRange
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.android.core.ui.theme.AndroidThemeForPreviews
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class MarkdownEditInputTransformationTest {

    @get:Rule
    var composeRule = createComposeRule()

    private lateinit var state: TextFieldState

    private fun setField(text: String, cursor: Int = text.length) {
        state = TextFieldState(text, initialSelection = TextRange(cursor))
        composeRule.setContent {
            AndroidThemeForPreviews {
                BasicTextField(
                    state = state,
                    inputTransformation = MarkdownEditInputTransformation,
                    modifier = Modifier.testTag(FIELD_TAG),
                )
            }
        }
    }

    @Test
    fun `test that transformInput continues a bullet item on Enter`() {
        setField("- item")

        composeRule.onNodeWithTag(FIELD_TAG).performTextInput("\n")

        assertThat(state.text.toString()).isEqualTo("- item\n- ")
        assertThat(state.selection).isEqualTo(TextRange(9))
    }

    @Test
    fun `test that transformInput increments an ordered item on Enter`() {
        setField("1. one")

        composeRule.onNodeWithTag(FIELD_TAG).performTextInput("\n")

        assertThat(state.text.toString()).isEqualTo("1. one\n2. ")
    }

    @Test
    fun `test that transformInput continues a quote on Enter`() {
        setField("> q")

        composeRule.onNodeWithTag(FIELD_TAG).performTextInput("\n")

        assertThat(state.text.toString()).isEqualTo("> q\n> ")
    }

    @Test
    fun `test that transformInput exits the list on Enter at an empty item`() {
        setField("- one\n- ")

        composeRule.onNodeWithTag(FIELD_TAG).performTextInput("\n")

        assertThat(state.text.toString()).isEqualTo("- one\n")
        assertThat(state.selection).isEqualTo(TextRange(6))
    }

    @Test
    fun `test that transformInput removes the whole marker on Backspace after it`() {
        setField("- item", cursor = 2)

        composeRule.onNodeWithTag(FIELD_TAG).requestFocus()
        composeRule.onNodeWithTag(FIELD_TAG).performKeyInput { pressKey(Key.Backspace) }

        assertThat(state.text.toString()).isEqualTo("item")
        assertThat(state.selection).isEqualTo(TextRange(0))
    }

    @Test
    fun `test that transformInput pushes content down as a new item when Enter is pressed right after the marker`() {
        setField("- item", cursor = 2)

        composeRule.onNodeWithTag(FIELD_TAG).performTextInput("\n")

        assertThat(state.text.toString()).isEqualTo("- \n- item")
        assertThat(state.selection).isEqualTo(TextRange(5))
    }

    @Test
    fun `test that transformInput leaves plain typing untouched`() {
        setField("abc")

        composeRule.onNodeWithTag(FIELD_TAG).performTextInput("d")

        assertThat(state.text.toString()).isEqualTo("abcd")
    }

    @Test
    fun `test that transformInput leaves Enter in plain text untouched`() {
        setField("plain")

        composeRule.onNodeWithTag(FIELD_TAG).performTextInput("\n")

        assertThat(state.text.toString()).isEqualTo("plain\n")
    }

    private companion object {
        const val FIELD_TAG = "markdown_input_field"
    }
}
