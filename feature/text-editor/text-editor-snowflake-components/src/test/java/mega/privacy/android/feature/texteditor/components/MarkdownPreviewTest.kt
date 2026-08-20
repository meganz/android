package mega.privacy.android.feature.texteditor.components

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.android.core.ui.theme.AndroidThemeForPreviews
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Rendering coverage for [MarkdownPreview]. Selection behaviour itself (drag handles, clipboard
 * contents) is not asserted here — Compose selection gestures are not reliable under Robolectric;
 * it is covered by the on-device checklist.
 */
@RunWith(AndroidJUnit4::class)
class MarkdownPreviewTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setContent(content: String) {
        composeRule.setContent {
            AndroidThemeForPreviews {
                MarkdownPreview(
                    content = content,
                    lazyListState = rememberLazyListState(),
                )
            }
        }
    }

    /** Parsing runs on a background dispatcher, so the first frames show only the spinner. */
    private fun assertTextDisplayed(text: String, substring: Boolean = false) {
        val matcher = hasText(text, substring = substring)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()
        }
        // onFirst: a repeated fragment such as a list marker legitimately matches several nodes.
        composeRule.onAllNodes(matcher).onFirst().assertIsDisplayed()
    }

    @Test
    fun `test that heading text is rendered without its markers`() {
        setContent("# Install guide")

        assertTextDisplayed("Install guide")
    }

    @Test
    fun `test that emphasis markers are not rendered as literal text`() {
        setContent("Run **npm install** now")

        assertTextDisplayed("Run npm install now")
    }

    @Test
    fun `test that bullet list items are rendered with a marker`() {
        setContent("- first item\n- second item")

        assertTextDisplayed("first item")
        assertTextDisplayed("second item")
        assertTextDisplayed("•", substring = true)
    }

    @Test
    fun `test that ordered list items are rendered with their numbers`() {
        setContent("1. alpha\n2. beta")

        assertTextDisplayed("alpha")
        assertTextDisplayed("1.", substring = true)
    }

    @Test
    fun `test that fenced code block content is rendered verbatim`() {
        setContent("```\nval x = 1\n```")

        assertTextDisplayed("val x = 1")
    }

    @Test
    fun `test that link text is rendered without its url`() {
        setContent("See [the docs](https://mega.io) for details")

        assertTextDisplayed("See the docs for details")
    }

    @Test
    fun `test that image renders its alt text only`() {
        setContent("![a diagram](https://attacker.example/beacon.png)")

        assertTextDisplayed("a diagram")
    }

    @Test
    fun `test that table cells are rendered`() {
        setContent("| Name | Size |\n| --- | --- |\n| notes | 2 KB |")

        assertTextDisplayed("Name")
        assertTextDisplayed("notes")
    }

    @Test
    fun `test that block quote content is rendered`() {
        setContent("> quoted line")

        assertTextDisplayed("quoted line")
    }

    @Test
    fun `test that content remains visible after a tap`() {
        // A clean tap recreates the SelectionContainers (selection reset); rendering must survive.
        setContent("# Hello\n\nWorld")
        assertTextDisplayed("Hello")

        composeRule.onRoot().performClick()

        assertTextDisplayed("Hello")
        assertTextDisplayed("World")
    }

    @Test
    fun `test that documents under the threshold compose every block eagerly`() {
        // The single-SelectionContainer column composes all blocks up front, so even blocks far
        // below the fold are present in the semantics tree.
        val filler = buildString {
            repeat(100) { append("eager paragraph number $it\n\n") }
        }
        setContent("# Small document\n\n$filler")

        assertTextDisplayed("Small document")
        composeRule.onAllNodes(hasText("eager paragraph number 99", substring = true))
            .assertCountEquals(1)
    }

    @Test
    fun `test that content above the full-selection threshold still renders`() {
        // Over 50k chars and 500 blocks: takes the virtualized per-block path instead of the
        // single-SelectionContainer column.
        val filler = buildString {
            repeat(3_000) { append("filler paragraph number $it with some words\n\n") }
        }
        setContent("# Big document\n\n$filler")

        assertTextDisplayed("Big document")
        // Virtualization is what distinguishes this path: blocks far below the fold must NOT be
        // composed, unlike the eager column path asserted above.
        composeRule.onAllNodes(hasText("filler paragraph number 2999", substring = true))
            .assertCountEquals(0)
    }
}
