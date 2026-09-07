package mega.privacy.android.core.formatter

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChatMessageTextFormatterTest {

    @Test
    fun `test that stripChatFormatTags leaves text without tags unchanged`() {
        assertThat("Text without format".stripChatFormatTags())
            .isEqualTo("Text without format")
    }

    @Test
    fun `test that stripChatFormatTags removes bold tags`() {
        assertThat("*bold*".stripChatFormatTags()).isEqualTo("bold")
    }

    @Test
    fun `test that stripChatFormatTags removes italic tags`() {
        assertThat("_italic_".stripChatFormatTags()).isEqualTo("italic")
    }

    @Test
    fun `test that stripChatFormatTags removes strikethrough tags`() {
        assertThat("~strike~".stripChatFormatTags()).isEqualTo("strike")
    }

    @Test
    fun `test that stripChatFormatTags removes quote tags`() {
        assertThat("`quote`".stripChatFormatTags()).isEqualTo("quote")
    }

    @Test
    fun `test that stripChatFormatTags removes multi quote tags`() {
        assertThat("```multi```".stripChatFormatTags()).isEqualTo("multi")
    }

    @Test
    fun `test that stripChatFormatTags removes nested tags`() {
        assertThat("*_both_*".stripChatFormatTags()).isEqualTo("both")
    }

    @Test
    fun `test that stripChatFormatTags removes several formats in the same text`() {
        assertThat("hello *world*, this is _mine_ and ~gone~".stripChatFormatTags())
            .isEqualTo("hello world, this is mine and gone")
    }

    @Test
    fun `test that stripChatFormatTags keeps a tag without its closing pair`() {
        assertThat("*not bold".stripChatFormatTags()).isEqualTo("*not bold")
    }

    @Test
    fun `test that stripChatFormatTags keeps tags when there is nothing between them`() {
        assertThat("**".stripChatFormatTags()).isEqualTo("**")
    }

    @Test
    fun `test that stripChatFormatTags keeps tags when a simple format contains a line break`() {
        assertThat("*first\nsecond*".stripChatFormatTags()).isEqualTo("*first\nsecond*")
    }

    @Test
    fun `test that stripChatFormatTags removes multi quote tags around a line break`() {
        assertThat("```first\nsecond```".stripChatFormatTags()).isEqualTo("first\nsecond")
    }

    @Test
    fun `test that stripChatFormatTags keeps an unpaired tag inside a format`() {
        assertThat("*a_b*".stripChatFormatTags()).isEqualTo("a_b")
    }

    @Test
    fun `test that stripChatFormatTags removes the outer pair when the message is only format tags`() {
        assertThat("*_*".stripChatFormatTags()).isEqualTo("_")
        assertThat("_*_".stripChatFormatTags()).isEqualTo("*")
        assertThat("*~*".stripChatFormatTags()).isEqualTo("~")
        assertThat("~_~".stripChatFormatTags()).isEqualTo("_")
    }
}
