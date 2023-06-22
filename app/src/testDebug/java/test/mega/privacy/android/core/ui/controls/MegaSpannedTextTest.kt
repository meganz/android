package test.mega.privacy.android.core.ui.controls

import androidx.compose.material.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.SpanStyle
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.core.ui.controls.text.MegaSpannedText
import mega.privacy.android.core.ui.model.SpanIndicator
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class MegaSpannedTextTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that show value correctly when the order of the tag is same with text`() {
        val text = "Do you want to [A]Google Pay[/A] [B]Huawei[/B] (subscription)"
        val styles = hashMapOf(
            SpanIndicator('A') to SpanStyle(color = Color.Red),
            SpanIndicator('B') to SpanStyle(color = Color.Green)
        )
        composeTestRule.setContent {
            MegaSpannedText(
                value = text,
                baseStyle = MaterialTheme.typography.subtitle1,
                styles = styles
            )
        }
        var expectedText = text
        styles.forEach {
            expectedText = expectedText.replace(it.key.openTag, "")
                .replace(it.key.closeTag, "")
        }
        composeTestRule.onNodeWithText(expectedText)
            .assertIsDisplayed()
    }

    @Test
    fun `test that show value correctly when the order of the tag is different with text`() {
        val text = "Do you want to [A]Google Pay[/A] [B]Huawei[/B] (subscription)"
        val styles = hashMapOf(
            SpanIndicator('B') to SpanStyle(color = Color.Green),
            SpanIndicator('A') to SpanStyle(color = Color.Red)
        )
        composeTestRule.setContent {
            MegaSpannedText(
                value = text,
                baseStyle = MaterialTheme.typography.subtitle1,
                styles = styles
            )
        }
        var expectedText = text
        styles.forEach {
            expectedText = expectedText.replace(it.key.openTag, "")
                .replace(it.key.closeTag, "")
        }
        composeTestRule.onNodeWithText(expectedText)
            .assertIsDisplayed()
    }
}