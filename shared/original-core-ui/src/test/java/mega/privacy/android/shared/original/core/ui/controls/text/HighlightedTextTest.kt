package mega.privacy.android.shared.original.core.ui.controls.text

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HighlightedTextTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that highlighted text is shown as expected`() {
        with(composeTestRule) {
            setContent {
                HighlightedText(
                    text = "This is ä tèst",
                    highlightText = "a test",
                    highlightColor = Color.Red,
                )
            }
            onNodeWithText("ä tèst", substring = true, useUnmergedTree = true).assertExists()
        }
    }
}