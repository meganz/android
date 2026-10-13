package mega.privacy.android.domain.extension

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class MarkdownFileNameTest {

    @Test
    fun `test that isMarkdownFileName returns true for md extension`() {
        assertThat(isMarkdownFileName("README.md")).isTrue()
    }

    @Test
    fun `test that isMarkdownFileName returns true for markdown extension`() {
        assertThat(isMarkdownFileName("notes.markdown")).isTrue()
    }

    @Test
    fun `test that isMarkdownFileName is case insensitive`() {
        assertThat(isMarkdownFileName("READ.MD")).isTrue()
        assertThat(isMarkdownFileName("a.Markdown")).isTrue()
    }

    @Test
    fun `test that isMarkdownFileName returns false for non markdown files`() {
        assertThat(isMarkdownFileName("notes.txt")).isFalse()
        assertThat(isMarkdownFileName("script.mdx")).isFalse()
        assertThat(isMarkdownFileName("noextension")).isFalse()
        assertThat(isMarkdownFileName("")).isFalse()
        assertThat(isMarkdownFileName("md")).isFalse()
    }
}
