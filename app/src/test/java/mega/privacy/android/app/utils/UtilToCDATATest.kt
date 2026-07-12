package mega.privacy.android.app.utils

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.utils.Util.toCDATA
import mega.privacy.android.app.utils.Util.toCDATAOrNull
import org.junit.Test

/**
 * Tests for [Util.toCDATA], the helper relied on to HTML-escape attacker-controlled
 * strings (node names, display names) before they reach Html.fromHtml.
 */
class UtilToCDATATest {

    @Test
    fun `test that toCDATA escapes HTML metacharacters`() {
        assertThat("<".toCDATA()).isEqualTo("&lt;")
        assertThat(">".toCDATA()).isEqualTo("&gt;")
        assertThat("&".toCDATA()).isEqualTo("&amp;")
        assertThat("\"".toCDATA()).isEqualTo("&quot;")
    }

    @Test
    fun `test that toCDATA escapes ampersand before other entities so it does not double-escape`() {
        assertThat("<a>".toCDATA()).isEqualTo("&lt;a&gt;")
    }

    @Test
    fun `test that toCDATA neutralises an injected HTML tag`() {
        val malicious = "<img src=x><a href=\"http://evil\">x</a>"

        val result = malicious.toCDATA()

        assertThat(result).doesNotContain("<")
        assertThat(result).doesNotContain(">")
        assertThat(result).isNotEqualTo(malicious)
    }

    @Test
    fun `test that toCDATA leaves a plain string unchanged`() {
        assertThat("Holiday Photos".toCDATA()).isEqualTo("Holiday Photos")
    }

    @Test
    fun `test that toCDATAOrNull returns null for a null receiver`() {
        assertThat((null as String?).toCDATAOrNull()).isNull()
    }

    @Test
    fun `test that toCDATAOrNull escapes a non-null receiver`() {
        assertThat("<b>".toCDATAOrNull()).isEqualTo("&lt;b&gt;")
    }
}
