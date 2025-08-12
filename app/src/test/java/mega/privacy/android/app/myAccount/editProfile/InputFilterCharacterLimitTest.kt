package mega.privacy.android.app.myAccount.editProfile

import android.text.InputFilter
import android.text.SpannableString
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InputFilterCharacterLimitTest {

    @Test
    fun `test that InputFilter with 40 character limit allows text up to 40 characters`() {
        val filter = InputFilter.LengthFilter(40)
        val text40Chars = "a".repeat(40)
        val dest = SpannableString("")

        val result = filter.filter(text40Chars, 0, text40Chars.length, dest, 0, 0)

        assertThat(result).isNull() // null means no filtering needed
    }

    @Test
    fun `test that InputFilter with 40 character limit truncates text longer than 40 characters`() {
        val filter = InputFilter.LengthFilter(40)
        val text50Chars = "a".repeat(50)
        val dest = SpannableString("")

        val result = filter.filter(text50Chars, 0, text50Chars.length, dest, 0, 0)

        assertThat(result).isEqualTo("a".repeat(40))
    }

    @Test
    fun `test that InputFilter with 40 character limit allows text shorter than 40 characters`() {
        val filter = InputFilter.LengthFilter(40)
        val shortText = "John"
        val dest = SpannableString("")

        val result = filter.filter(shortText, 0, shortText.length, dest, 0, 0)

        assertThat(result).isNull() // null means no filtering needed
    }

    @Test
    fun `test that InputFilter with 40 character limit handles empty string`() {
        val filter = InputFilter.LengthFilter(40)
        val emptyText = ""
        val dest = SpannableString("")

        val result = filter.filter(emptyText, 0, emptyText.length, dest, 0, 0)

        assertThat(result).isNull() // null means no filtering needed
    }

    @Test
    fun `test that InputFilter with 40 character limit handles unicode characters correctly`() {
        val filter = InputFilter.LengthFilter(40)
        val unicodeText = "José-María Müller-Schmidt Žáček" // 32 chars with unicode
        val dest = SpannableString("")

        val result = filter.filter(unicodeText, 0, unicodeText.length, dest, 0, 0)

        assertThat(result).isNull() // null means no filtering needed
        assertThat(unicodeText.length).isLessThan(40)
    }

    @Test
    fun `test that InputFilter with 40 character limit handles very long unicode text`() {
        val filter = InputFilter.LengthFilter(40)
        val longUnicodeText = "José-María Müller-Schmidt Žáček González-Fernández" // 50 chars
        val dest = SpannableString("")

        val result = filter.filter(longUnicodeText, 0, longUnicodeText.length, dest, 0, 0)

        assertThat(result).isEqualTo(longUnicodeText.take(40))
        assertThat(result?.length).isEqualTo(40)
    }

    @Test
    fun `test that InputFilter with 40 character limit handles special characters`() {
        val filter = InputFilter.LengthFilter(40)
        val specialText = "John-O'Connor.Jr@Domain#123!$%&*()+" // 35 chars
        val dest = SpannableString("")

        val result = filter.filter(specialText, 0, specialText.length, dest, 0, 0)

        assertThat(result).isNull() // null means no filtering needed
    }

    @Test
    fun `test that InputFilter with 40 character limit truncates spaces correctly`() {
        val filter = InputFilter.LengthFilter(40)
        val spacesText = " ".repeat(50)
        val dest = SpannableString("")

        val result = filter.filter(spacesText, 0, spacesText.length, dest, 0, 0)

        assertThat(result).isEqualTo(" ".repeat(40))
        assertThat(result?.length).isEqualTo(40)
    }

    @Test
    fun `test that multiple InputFilters can be applied together`() {
        val lengthFilter = InputFilter.LengthFilter(40)
        val allCapsFilter = InputFilter.AllCaps()
        val filters = arrayOf(lengthFilter, allCapsFilter)

        val inputText = "john".repeat(15) // 60 chars total
        val dest = SpannableString("")

        // Apply length filter first
        val lengthResult = lengthFilter.filter(inputText, 0, inputText.length, dest, 0, 0)
        val truncatedText = lengthResult ?: inputText.take(40)

        // Apply caps filter to truncated text
        val capsResult = allCapsFilter.filter(truncatedText, 0, truncatedText.length, dest, 0, 0)
        val finalText = capsResult ?: truncatedText

        assertThat(finalText.length).isEqualTo(40)
        assertThat(finalText).isEqualTo("JOHN".repeat(10)) // 40 chars of "JOHN"
    }

    @Test
    fun `test that InputFilter handles edge case of exactly 40 character boundary`() {
        val filter = InputFilter.LengthFilter(40)
        val dest = SpannableString("")

        // Test 39 characters
        val text39 = "a".repeat(39)
        val result39 = filter.filter(text39, 0, text39.length, dest, 0, 0)
        assertThat(result39).isNull()

        // Test 40 characters
        val text40 = "a".repeat(40)
        val result40 = filter.filter(text40, 0, text40.length, dest, 0, 0)
        assertThat(result40).isNull()

        // Test 41 characters
        val text41 = "a".repeat(41)
        val result41 = filter.filter(text41, 0, text41.length, dest, 0, 0)
        assertThat(result41).isEqualTo("a".repeat(40))
    }

    @Test
    fun `test that InputFilter correctly reports max length`() {
        val filter = InputFilter.LengthFilter(40)

        assertThat(filter.max).isEqualTo(40)
    }

    @Test
    fun `test that InputFilter behavior is consistent across multiple calls`() {
        val filter = InputFilter.LengthFilter(40)
        val longText = "a".repeat(100)
        val dest = SpannableString("")

        // Call filter multiple times with same input
        repeat(5) {
            val result = filter.filter(longText, 0, longText.length, dest, 0, 0)
            assertThat(result).isEqualTo("a".repeat(40))
            assertThat(result?.length).isEqualTo(40)
        }
    }
}