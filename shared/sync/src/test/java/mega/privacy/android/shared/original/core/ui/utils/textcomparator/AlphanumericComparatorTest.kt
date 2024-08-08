package mega.privacy.android.shared.original.core.ui.utils.textcomparator

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AlphanumericComparatorTest {

    @Test
    fun `test that compare returns 0 when both strings are equal`() {
        val comparator = AlphanumericComparator()
        assertEquals(0, comparator.compare("abc", "abc"))
    }

    @Test
    fun `test that compare returns a negative integer when the first string is less than the second string`() {
        val comparator = AlphanumericComparator()
        assertEquals(-1, comparator.compare("abc", "def"))
    }

    @Test
    fun `test that compare returns a positive integer when the first string is greater than the second string`() {
        val comparator = AlphanumericComparator()
        assertEquals(1, comparator.compare("def", "abc"))
    }

    @Test
    fun `test that compare returns a negative integer when the first string is shorter than the second string`() {
        val comparator = AlphanumericComparator()
        assertEquals(-1, comparator.compare("abc", "abcdef"))
    }

    @Test
    fun `test that compare returns a positive integer when the first string is longer than the second string`() {
        val comparator = AlphanumericComparator()
        assertEquals(1, comparator.compare("abcdef", "abc"))
    }

    @Test
    fun `test that compare returns a negative integer when the first string has a smaller number than the second string`() {
        val comparator = AlphanumericComparator()
        assertEquals(-1, comparator.compare("abc1", "abc2"))
    }

    @Test
    fun `test that compare returns a positive integer when the first string has a greater number than the second string`() {
        val comparator = AlphanumericComparator()
        assertEquals(1, comparator.compare("abc2", "abc1"))
    }

    @Test
    fun `test that compare returns a negative integer when the first string has a smaller number than the second string with leading zeros`() {
        val comparator = AlphanumericComparator()
        assertEquals(-1, comparator.compare("abc01", "abc02"))
    }

    @Test
    fun `test that compare returns a positive integer when the first string has a greater number than the second string with leading zeros`() {
        val comparator = AlphanumericComparator()
        assertEquals(1, comparator.compare("abc02", "abc01"))
    }

    @Test
    fun `test that compare returns a negative integer when the first string has a smaller number than the second string with different number of digits`() {
        val comparator = AlphanumericComparator()
        assertEquals(-1, comparator.compare("abc1", "abc10"))
    }

    @Test
    fun `test that compare returns a positive integer when the first string has a greater number than the second string with different number of digits`() {
        val comparator = AlphanumericComparator()
        assertEquals(1, comparator.compare("abc10", "abc1"))
    }

    @Test
    fun `test that compare returns a negative integer when the first string has a smaller number than the second string with different number of digits and leading zeros`() {
        val comparator = AlphanumericComparator()
        assertEquals(-1, comparator.compare("abc01", "abc10"))
    }

    @Test
    fun `test that compare returns a positive integer when the first string has a greater number than the second string with different number of digits and leading zeros`() {
        val comparator = AlphanumericComparator()
        assertEquals(1, comparator.compare("abc10", "abc01"))
    }

    @Test
    fun `test that compare returns a negative integer when the first string has a smaller number than the second string with different number of digits and leading zeros and other characters`() {
        val comparator = AlphanumericComparator()
        assertEquals(-1, comparator.compare("abc01def", "abc10def"))
    }

    @Test
    fun `test that compare returns a positive integer when the first string has a greater number than the second string with different number of digits and leading zeros and other characters`() {
        val comparator = AlphanumericComparator()
        assertEquals(1, comparator.compare("abc10def", "abc01def"))
    }

    @Test
    fun `test that compare returns a negative integer when the first string has a smaller number than the second string with different number of digits and leading zeros and other characters and the first string is shorter`() {
        val comparator = AlphanumericComparator()
        assertEquals(-1, comparator.compare("abc1", "abc10def"))
    }

    @Test
    fun `test that compare returns a positive integer when the first string has a greater number than the second string with different number of digits and leading zeros and other characters and the first string is shorter`() {
        val comparator = AlphanumericComparator()
        assertEquals(1, comparator.compare("abc10def", "abc1"))
    }
}