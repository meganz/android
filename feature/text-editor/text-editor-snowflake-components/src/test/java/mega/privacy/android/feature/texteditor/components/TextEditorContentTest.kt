package mega.privacy.android.feature.texteditor.components

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.android.core.ui.theme.AndroidThemeForPreviews
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TextEditorContentTest {

    @get:Rule
    var composeRule = createComposeRule()

    @Test
    fun `test that a tap beyond the end of a line still focuses the editor`() {
        val state = TextFieldState("hi")
        var focusedChunk = -1
        composeRule.setContent {
            AndroidThemeForPreviews {
                TextEditorContent(
                    lazyListState = rememberLazyListState(),
                    chunkCount = 1,
                    totalLineCount = 1,
                    chunkTextProvider = { "hi" },
                    chunkStateProvider = { state },
                    chunkStartLineProvider = { 1 },
                    onChunkDisposed = {},
                    isChunkReadOnly = { false },
                    onChunkFocused = { focusedChunk = it },
                    showLineNumbers = false,
                    readOnly = false,
                )
            }
        }

        // Far right of the first line — well past the two characters of text, but inside
        // the list's 16dp end content padding.
        composeRule.onRoot().performTouchInput {
            click(Offset(right * 0.8f, 10f))
        }
        composeRule.waitForIdle()

        assertThat(focusedChunk).isEqualTo(0)
    }
}
