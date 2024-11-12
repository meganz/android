package mega.privacy.android.shared.original.core.ui.utils

import com.google.common.truth.Truth
import org.junit.Test

class StringExtensionsTest {

    @Test
    fun `test that accented text is normalised as expected`() {
        val text = "áéíóů"
        val expected = "aeiou"
        Truth.assertThat(text.normalize()).isEqualTo(expected)
    }

    @Test
    fun `test that non-accented text is not changed`() {
        val text = "aeiou"
        val expected = "aeiou"
        Truth.assertThat(text.normalize()).isEqualTo(expected)
    }

    @Test
    fun `test that mixed text is normalised as expected`() {
        val text = "áeíou"
        val expected = "aeiou"
        Truth.assertThat(text.normalize()).isEqualTo(expected)
    }

    @Test
    fun `test that empty text is not changed`() {
        val text = ""
        val expected = ""
        Truth.assertThat(text.normalize()).isEqualTo(expected)
    }

    @Test
    fun `test that text with spaces is normalised as expected`() {
        val text = "á é í o u"
        val expected = "a e i o u"
        Truth.assertThat(text.normalize()).isEqualTo(expected)
    }

    @Test
    fun `test that text with special characters is normalised as expected`() {
        val text = "áéíóů!@#$%^&*()_+"
        val expected = "aeiou!@#$%^&*()_+"
        Truth.assertThat(text.normalize()).isEqualTo(expected)
    }

    @Test
    fun `test that text with numbers is not changed`() {
        val text = "1234567890"
        val expected = "1234567890"
        Truth.assertThat(text.normalize()).isEqualTo(expected)
    }

    @Test
    fun `test that text with special characters and numbers is not changed`() {
        val text = "áéíóů!@#$%^&*()_+1234567890"
        val expected = "aeiou!@#$%^&*()_+1234567890"
        Truth.assertThat(text.normalize()).isEqualTo(expected)
    }
}