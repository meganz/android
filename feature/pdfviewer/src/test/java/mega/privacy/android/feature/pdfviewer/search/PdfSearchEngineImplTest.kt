package mega.privacy.android.feature.pdfviewer.search

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.shockwave.pdfium.PdfiumCore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock

/**
 * Unit tests for [PdfSearchEngineImpl].
 *
 */
@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PdfSearchEngineImplTest {

    private val context = mock<Context>()
    private val pdfiumCore = mock<PdfiumCore>()
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var underTest: PdfSearchEngineImpl

    @BeforeEach
    fun setUp() {
        underTest = PdfSearchEngineImpl(
            context = context,
            pdfiumCore = pdfiumCore,
            dispatcher = testDispatcher
        )
    }

    @AfterEach
    fun tearDown() {
        underTest.close()
    }

    // ==================== Pure Logic Tests: pageOrderForSearch ====================

    @Test
    fun `test that pageOrderForSearch returns empty when totalPages is zero`() {
        assertThat(PdfSearchEngineImpl.pageOrderForSearch(0, 0)).isEmpty()
    }

    @Test
    fun `test that pageOrderForSearch returns empty when totalPages is negative`() {
        assertThat(PdfSearchEngineImpl.pageOrderForSearch(-1, 0)).isEmpty()
    }

    @Test
    fun `test that pageOrderForSearch with startPage 0 returns 0 until totalPages`() {
        assertThat(PdfSearchEngineImpl.pageOrderForSearch(5, 0))
            .containsExactly(0, 1, 2, 3, 4)
    }

    @Test
    fun `test that pageOrderForSearch with startPage in middle wraps around`() {
        assertThat(PdfSearchEngineImpl.pageOrderForSearch(5, 2))
            .containsExactly(2, 3, 4, 0, 1)
    }

    @Test
    fun `test that pageOrderForSearch with startPage at end returns last then rest`() {
        assertThat(PdfSearchEngineImpl.pageOrderForSearch(5, 4))
            .containsExactly(4, 0, 1, 2, 3)
    }

    @Test
    fun `test that pageOrderForSearch with startPage equal to totalPages is coerced`() {
        assertThat(PdfSearchEngineImpl.pageOrderForSearch(5, 5))
            .containsExactly(4, 0, 1, 2, 3)
    }

    @Test
    fun `test that pageOrderForSearch with startPage greater than totalPages is coerced`() {
        assertThat(PdfSearchEngineImpl.pageOrderForSearch(3, 10))
            .containsExactly(2, 0, 1)
    }

    @Test
    fun `test that pageOrderForSearch with negative startPage is coerced to 0`() {
        assertThat(PdfSearchEngineImpl.pageOrderForSearch(4, -1))
            .containsExactly(0, 1, 2, 3)
    }

    @Test
    fun `test that pageOrderForSearch with single page returns single element`() {
        assertThat(PdfSearchEngineImpl.pageOrderForSearch(1, 0))
            .containsExactly(0)
    }

    @Test
    fun `test that pageOrderForSearch with two pages starting from 0 returns 0 then 1`() {
        assertThat(PdfSearchEngineImpl.pageOrderForSearch(2, 0))
            .containsExactly(0, 1)
    }

    @Test
    fun `test that pageOrderForSearch with two pages starting from 1 wraps around`() {
        assertThat(PdfSearchEngineImpl.pageOrderForSearch(2, 1))
            .containsExactly(1, 0)
    }

    @Test
    fun `test that pageOrderForSearch with ten pages starting from 7 wraps correctly`() {
        assertThat(PdfSearchEngineImpl.pageOrderForSearch(10, 7))
            .containsExactly(7, 8, 9, 0, 1, 2, 3, 4, 5, 6)
    }

    // ==================== Pure Logic Tests: shouldSkipSearch ====================

    @Test
    fun `test that shouldSkipSearch returns true when totalPages is zero`() {
        assertThat(PdfSearchEngineImpl.shouldSkipSearch(0, "query")).isTrue()
    }

    @Test
    fun `test that shouldSkipSearch returns false when totalPages is negative and query is valid`() {
        // Negative totalPages is not a valid PDF state, but the function only checks == 0
        assertThat(PdfSearchEngineImpl.shouldSkipSearch(-1, "query")).isFalse()
    }

    @Test
    fun `test that shouldSkipSearch returns true when query is empty`() {
        assertThat(PdfSearchEngineImpl.shouldSkipSearch(10, "")).isTrue()
    }

    @Test
    fun `test that shouldSkipSearch returns true when query is blank`() {
        assertThat(PdfSearchEngineImpl.shouldSkipSearch(10, "   ")).isTrue()
    }

    @Test
    fun `test that shouldSkipSearch returns true when query length is one character`() {
        assertThat(PdfSearchEngineImpl.shouldSkipSearch(10, "a")).isTrue()
    }

    @Test
    fun `test that shouldSkipSearch returns false when query length equals MIN_QUERY_LENGTH`() {
        assertThat(PdfSearchEngineImpl.shouldSkipSearch(10, "ab")).isFalse()
    }

    @Test
    fun `test that shouldSkipSearch returns false when query length is greater than MIN_QUERY_LENGTH`() {
        assertThat(PdfSearchEngineImpl.shouldSkipSearch(10, "abc")).isFalse()
    }

    @Test
    fun `test that shouldSkipSearch trims query before checking - single char trimmed`() {
        assertThat(PdfSearchEngineImpl.shouldSkipSearch(10, "  a  ")).isTrue()
    }

    @Test
    fun `test that shouldSkipSearch trims query before checking - two chars trimmed`() {
        assertThat(PdfSearchEngineImpl.shouldSkipSearch(10, "  ab  ")).isFalse()
    }

    @Test
    fun `test that shouldSkipSearch returns false for valid multi-word query`() {
        assertThat(PdfSearchEngineImpl.shouldSkipSearch(10, "search term")).isFalse()
    }

    @Test
    fun `test that shouldSkipSearch returns false with minimum valid inputs`() {
        assertThat(PdfSearchEngineImpl.shouldSkipSearch(1, "ab")).isFalse()
    }

    @Test
    fun `test that shouldSkipSearch returns true for blank query even with valid totalPages`() {
        assertThat(PdfSearchEngineImpl.shouldSkipSearch(100, "   \t\n   ")).isTrue()
    }

    @Test
    fun `test that shouldSkipSearch handles unicode characters correctly`() {
        assertThat(PdfSearchEngineImpl.shouldSkipSearch(10, "日")).isTrue() // Single unicode char
        assertThat(PdfSearchEngineImpl.shouldSkipSearch(10, "日本")).isFalse() // Two unicode chars
    }

    @Test
    fun `test that shouldSkipSearch handles special characters correctly`() {
        assertThat(PdfSearchEngineImpl.shouldSkipSearch(10, "@")).isTrue()
        assertThat(PdfSearchEngineImpl.shouldSkipSearch(10, "@#")).isFalse()
    }
}
