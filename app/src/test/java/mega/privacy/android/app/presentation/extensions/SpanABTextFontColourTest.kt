package mega.privacy.android.app.presentation.extensions

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.mockito.kotlin.mock

class SpanABTextFontColourTest {

    @Test
    fun `test that string is spanned correctly`() {
        val input = getTestString(a = "[A]", aClose = "[/A]", b = "[B]", bClose = "[/B]")
        val aColour = "aColourHex"
        val bColour = "bColourHex"
        val expected = getTestString(a = "<font color='$aColour'>", aClose = "</font>", b = "<font color='$bColour'>", bClose = "</font>")

        val actual = input.spanABTextFontColour(
            context = mock(),
            aColourHex = aColour,
            bColourHex = bColour,
            toSpannedString = {it}
        )

        assertThat(actual).isEqualTo(expected)
    }

    private fun getTestString(a: String, aClose: String, b: String, bClose: String) =
        "This is a $a test $aClose string with $b replaced $bClose values"
}