package mega.privacy.android.app.utils

import android.app.Application
import android.content.Context
import com.google.common.truth.Truth.assertThat
import dagger.hilt.internal.GeneratedComponent
import mega.privacy.android.app.utils.Util.toCDATA
import mega.privacy.android.app.utils.Util.toCDATAOrNull
import mega.privacy.android.core.formatter.emoji.EmojiShortcodeConverter
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Tests for [Util.toCDATA], the helper relied on to HTML-escape attacker-controlled
 * strings (node names, display names) before they reach Html.fromHtml.
 */
class UtilToCDATATest {

    private val emojiShortcodeConverter = mock<EmojiShortcodeConverter> {
        on { convert(any()) } doAnswer { it.getArgument(0) }
    }

    private val application = mock<Application>(
        extraInterfaces = arrayOf(
            GeneratedComponent::class,
            EmojiShortcodeConverterEntryPoint::class,
        ),
    ).also {
        whenever((it as EmojiShortcodeConverterEntryPoint).emojiShortcodeConverter())
            .thenReturn(emojiShortcodeConverter)
    }

    private val context = mock<Context> {
        on { applicationContext } doReturn application
    }

    @Test
    fun `test that toCDATA escapes HTML metacharacters`() {
        assertThat("<".toCDATA(context)).isEqualTo("&lt;")
        assertThat(">".toCDATA(context)).isEqualTo("&gt;")
        assertThat("&".toCDATA(context)).isEqualTo("&amp;")
        assertThat("\"".toCDATA(context)).isEqualTo("&quot;")
    }

    @Test
    fun `test that toCDATA escapes ampersand before other entities so it does not double-escape`() {
        assertThat("<a>".toCDATA(context)).isEqualTo("&lt;a&gt;")
    }

    @Test
    fun `test that toCDATA neutralises an injected HTML tag`() {
        val malicious = "<img src=x><a href=\"http://evil\">x</a>"

        val result = malicious.toCDATA(context)

        assertThat(result).doesNotContain("<")
        assertThat(result).doesNotContain(">")
        assertThat(result).isNotEqualTo(malicious)
    }

    @Test
    fun `test that toCDATA leaves a plain string unchanged`() {
        assertThat("Holiday Photos".toCDATA(context)).isEqualTo("Holiday Photos")
    }

    @Test
    fun `test that toCDATA converts emoji short-codes in the escaped text`() {
        whenever(emojiShortcodeConverter.convert(":smile: &lt;b&gt;")) doReturn "😄 &lt;b&gt;"

        assertThat(":smile: <b>".toCDATA(context)).isEqualTo("😄 &lt;b&gt;")
    }

    @Test
    fun `test that toCDATAOrNull returns null for a null receiver`() {
        assertThat((null as String?).toCDATAOrNull(context)).isNull()
    }

    @Test
    fun `test that toCDATAOrNull escapes a non-null receiver`() {
        assertThat("<b>".toCDATAOrNull(context)).isEqualTo("&lt;b&gt;")
    }
}
