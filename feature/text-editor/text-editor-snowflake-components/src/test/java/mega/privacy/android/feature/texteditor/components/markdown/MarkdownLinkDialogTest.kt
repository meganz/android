package mega.privacy.android.feature.texteditor.components.markdown

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.android.core.ui.theme.AndroidThemeForPreviews
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MarkdownLinkDialogTest {

    @get:Rule
    var composeRule = createComposeRule()

    private val confirmed = mutableListOf<Pair<String, String>>()
    private var removed = 0

    private fun setDialog(
        initialText: String = "MEGA",
        initialUrl: String = "https://mega.io",
        showRemove: Boolean = false,
    ) {
        composeRule.setContent {
            AndroidThemeForPreviews {
                MarkdownLinkDialog(
                    initialText = initialText,
                    initialUrl = initialUrl,
                    showRemove = showRemove,
                    onConfirm = { text, url -> confirmed.add(text to url) },
                    onRemove = { removed++ },
                    onDismiss = {},
                )
            }
        }
    }

    @Test
    fun `test that the link field sits above the text field`() {
        setDialog()

        val linkTop = composeRule.onNodeWithText("Link").getBoundsInRoot().top
        val textTop = composeRule.onNodeWithText("Text").getBoundsInRoot().top

        assertThat(linkTop.value).isLessThan(textTop.value)
    }

    @Test
    fun `test that save does nothing when the url is blank`() {
        setDialog(initialUrl = "")

        // A disabled BasicDialogButton drops its clickable rather than reporting as disabled.
        composeRule.onNodeWithText("Save").assertHasNoClickAction()
    }

    @Test
    fun `test that save reports the text and the url`() {
        setDialog()

        composeRule.onNodeWithText("Save").assertHasClickAction().performClick()

        assertThat(confirmed).containsExactly("MEGA" to "https://mega.io")
    }

    @Test
    fun `test that remove link is only offered for an existing link`() {
        setDialog(showRemove = false)

        composeRule.onNodeWithText("Remove link").assertDoesNotExist()
    }

    @Test
    fun `test that remove link unwraps an existing link`() {
        setDialog(showRemove = true)

        composeRule.onNodeWithText("Remove link").assertIsDisplayed().performClick()

        assertThat(removed).isEqualTo(1)
    }
}
