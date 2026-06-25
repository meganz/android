package mega.privacy.android.domain.extension

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class MarkdownFileExtensionTest {

    @Test
    fun `test that isMarkdownFile returns true for md extension`() {
        assertThat("README.md".isMarkdownFile()).isTrue()
    }

    @Test
    fun `test that isMarkdownFile returns true for markdown extension`() {
        assertThat("notes.markdown".isMarkdownFile()).isTrue()
    }

    @Test
    fun `test that isMarkdownFile is case insensitive`() {
        assertThat("READ.MD".isMarkdownFile()).isTrue()
        assertThat("a.Markdown".isMarkdownFile()).isTrue()
    }

    @Test
    fun `test that isMarkdownFile returns false for non markdown files`() {
        assertThat("notes.txt".isMarkdownFile()).isFalse()
        assertThat("script.mdx".isMarkdownFile()).isFalse()
        assertThat("noextension".isMarkdownFile()).isFalse()
        assertThat("".isMarkdownFile()).isFalse()
        assertThat("md".isMarkdownFile()).isFalse()
    }
}
