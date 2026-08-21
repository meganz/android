package mega.privacy.android.feature.texteditor.components.markdown.rich

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.android.core.ui.theme.AndroidThemeForPreviews
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class RichSpanEditingTest {

    @get:Rule
    var composeRule = createComposeRule()

    private lateinit var state: RichTextBlockState
    private var layoutResult: TextLayoutResult? = null

    @Composable
    private fun RichField() {
        BasicTextField(
            state = state.textFieldState,
            inputTransformation = RichSpanInputTransformation(state),
            outputTransformation = RichSpanOutputTransformation(
                state,
                rememberRichSpanVisualStyles(),
            ),
            onTextLayout = { getResult -> getResult()?.let { layoutResult = it } },
            modifier = Modifier.testTag(FIELD_TAG),
        )
    }

    private fun setField(text: String, spans: List<RichSpan> = emptyList()) {
        state = RichTextBlockState(text, spans)
        composeRule.setContent {
            AndroidThemeForPreviews {
                RichField()
            }
        }
    }

    @Test
    fun `test that typing inside a bold span grows it and restyles the layout`() {
        setField("plain bold plain", listOf(RichSpan(6, 10, RichSpanStyle.Bold)))
        composeRule.onNodeWithTag(FIELD_TAG).performTextInputSelection(TextRange(8))

        composeRule.onNodeWithTag(FIELD_TAG).performTextInput("xx")
        composeRule.waitForIdle()

        assertThat(state.spans).containsExactly(RichSpan(6, 12, RichSpanStyle.Bold))
        val boldRanges = layoutResult?.layoutInput?.text?.spanStyles
            ?.filter { it.item.fontWeight == FontWeight.Bold }
        assertThat(boldRanges?.any { it.start == 6 && it.end == 12 }).isTrue()
    }

    @Test
    fun `test that typing with an active typing style creates a styled span`() {
        setField("ab")
        composeRule.onNodeWithTag(FIELD_TAG).performTextInputSelection(TextRange(2))
        state.toggleStyle(RichSpanStyle.Bold, TextRange(2))

        composeRule.onNodeWithTag(FIELD_TAG).performTextInput("cd")
        composeRule.waitForIdle()

        assertThat(state.textFieldState.text.toString()).isEqualTo("abcd")
        assertThat(state.spans).containsExactly(RichSpan(2, 4, RichSpanStyle.Bold))
    }

    @Test
    fun `test that typing after a bold span without the typing style stays plain`() {
        setField("bold", listOf(RichSpan(0, 4, RichSpanStyle.Bold)))
        composeRule.onNodeWithTag(FIELD_TAG).performTextInputSelection(TextRange(4))

        composeRule.onNodeWithTag(FIELD_TAG).performTextInput("!")
        composeRule.waitForIdle()

        assertThat(state.spans).containsExactly(RichSpan(0, 4, RichSpanStyle.Bold))
    }

    @Test
    fun `test that toggleStyle applies a span over the selection`() {
        setField("hello world")

        state.toggleStyle(RichSpanStyle.Italic, TextRange(0, 5))

        assertThat(state.spans).containsExactly(RichSpan(0, 5, RichSpanStyle.Italic))
    }

    @Test
    fun `test that stylesAt reports the styles covering the caret`() {
        setField("bold plain", listOf(RichSpan(0, 4, RichSpanStyle.Bold)))

        assertThat(state.stylesAt(3)).containsExactly(RichSpanStyle.Bold)
        assertThat(state.stylesAt(4)).containsExactly(RichSpanStyle.Bold)
        assertThat(state.stylesAt(7)).isEmpty()
    }

    private companion object {
        const val FIELD_TAG = "rich_span_field"
    }
}
