package mega.privacy.android.core.nodecomponents.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.core.nodecomponents.R
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FileSizeMapperTest {

    private lateinit var underTest: FileSizeMapper

    @BeforeEach
    fun setUp() {
        underTest = FileSizeMapper()
    }

    @Test
    fun `test that invoke returns correct pair for bytes`() {
        val result = underTest(500L)

        assertThat(result.first).isEqualTo(R.string.label_file_size_byte)
        assertThat(result.second).isEqualTo(500.0)
    }

    @Test
    fun `test that invoke returns correct pair for kilobytes`() {
        val result = underTest(2048L)

        assertThat(result.first).isEqualTo(R.string.label_file_size_kilo_byte)
        assertThat(result.second).isEqualTo(2.0)
    }

    @Test
    fun `test that invoke returns correct pair for megabytes`() {
        val result = underTest(2097152L) // 2MB

        assertThat(result.first).isEqualTo(R.string.label_file_size_mega_byte)
        assertThat(result.second).isEqualTo(2.0)
    }

    @Test
    fun `test that invoke returns correct pair for gigabytes`() {
        val result = underTest(2147483648L) // 2GB

        assertThat(result.first).isEqualTo(R.string.label_file_size_giga_byte)
        assertThat(result.second).isEqualTo(2.0)
    }

    @Test
    fun `test that invoke returns correct pair for terabytes`() {
        val result = underTest(2199023255552L) // 2TB

        assertThat(result.first).isEqualTo(R.string.label_file_size_tera_byte)
        assertThat(result.second).isEqualTo(2.0)
    }

    @Test
    fun `test that invoke returns correct pair for petabytes`() {
        val result = underTest(2251799813685248L) // 2PB

        assertThat(result.first).isEqualTo(R.string.label_file_size_peta_byte)
        assertThat(result.second).isEqualTo(2.0)
    }

    @Test
    fun `test that invoke returns correct pair for exabytes`() {
        val result = underTest(2305843009213693952L) // 2EB

        assertThat(result.first).isEqualTo(R.string.label_file_size_exa_byte)
        assertThat(result.second).isEqualTo(2.0)
    }

    @Test
    fun `test that invoke returns correct pair for fractional values`() {
        val result = underTest(1536L) // 1.5KB

        assertThat(result.first).isEqualTo(R.string.label_file_size_kilo_byte)
        assertThat(result.second).isEqualTo(1.5)
    }
} 