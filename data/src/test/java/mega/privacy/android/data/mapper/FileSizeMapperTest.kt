package mega.privacy.android.data.mapper

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.Locale
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class FileSizeMapperTest {

    private val underTest = FileSizeMapper()

    private lateinit var originalLocale: Locale

    @BeforeAll
    fun setUp() {
        // DecimalFormat is locale-dependent; pin to US so "." is the decimal separator.
        originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
    }

    @AfterAll
    fun tearDown() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun `test that zero bytes returns zero`() {
        assertThat(underTest(0L)).isEqualTo(0.0)
    }

    @Test
    fun `test that size below 1KB returns the raw byte count`() {
        assertThat(underTest(512L)).isEqualTo(512.0)
    }

    @Test
    fun `test that size at the kilobyte boundary returns 1`() {
        assertThat(underTest(KILOBYTE)).isEqualTo(1.0)
    }

    @Test
    fun `test that size at the megabyte boundary returns 1`() {
        assertThat(underTest(MEGABYTE)).isEqualTo(1.0)
    }

    @Test
    fun `test that size at the gigabyte boundary returns 1`() {
        assertThat(underTest(GIGABYTE)).isEqualTo(1.0)
    }

    @Test
    fun `test that size at the terabyte boundary returns 1`() {
        assertThat(underTest(TERABYTE)).isEqualTo(1.0)
    }

    @Test
    fun `test that size at the petabyte boundary returns 1`() {
        assertThat(underTest(PETABYTE)).isEqualTo(1.0)
    }

    @Test
    fun `test that size at the exabyte boundary returns 1`() {
        assertThat(underTest(EXABYTE)).isEqualTo(1.0)
    }

    @ParameterizedTest(name = "test that {0} bytes returns {1}")
    @MethodSource("provideSizes")
    fun `test that size is converted to the value in its best fit unit`(
        size: Long,
        expected: Double,
    ) {
        assertThat(underTest(size)).isEqualTo(expected)
    }

    private fun provideSizes(): Stream<Arguments> = Stream.of(
        Arguments.of(2 * KILOBYTE, 2.0),
        Arguments.of(KILOBYTE + KILOBYTE / 2, 1.5),
        Arguments.of(2 * MEGABYTE, 2.0),
        Arguments.of(2 * GIGABYTE, 2.0),
        Arguments.of(2 * TERABYTE, 2.0),
        Arguments.of(2 * PETABYTE, 2.0),
    )

    companion object {
        private const val KILOBYTE = 1024L
        private const val MEGABYTE = KILOBYTE * 1024
        private const val GIGABYTE = MEGABYTE * 1024
        private const val TERABYTE = GIGABYTE * 1024
        private const val PETABYTE = TERABYTE * 1024
        private const val EXABYTE = PETABYTE * 1024
    }
}
