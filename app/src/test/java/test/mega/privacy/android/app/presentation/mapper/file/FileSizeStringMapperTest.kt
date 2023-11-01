package test.mega.privacy.android.app.presentation.mapper.file

import android.content.Context
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.mapper.file.FileSizeStringMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.text.DecimalFormat

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FileSizeStringMapperTest {
    private lateinit var underTest: FileSizeStringMapper

    private var context: Context = mock()

    private val kilobyte = 1024f
    private val megabyte = kilobyte * 1024
    private val gigabyte = megabyte * 1024
    private val terabyte = gigabyte * 1024
    private val petabyte = terabyte * 1024
    private val exabyte = petabyte * 1024

    private val df = DecimalFormat("#.##")

    @BeforeAll
    fun setUp() {
        underTest = FileSizeStringMapper(
            context = context,
        )
    }

    @ParameterizedTest(name = "when size is {0} bytes")
    @ValueSource(longs = [100, 1024, 1048576, 1073741824, 1099511627776, 1125899906842624, 1152921504606847000])
    fun `test that mapper returns the correct formatted string`(size: Long) {
        val format = when {
            size < kilobyte -> size.toString()
            size < megabyte -> df.format(size / kilobyte)
            size < gigabyte -> df.format(size / megabyte)
            size < terabyte -> df.format(size / gigabyte)
            size < petabyte -> df.format(size / terabyte)
            size < exabyte -> df.format(size / petabyte)
            else -> df.format(size / exabyte)
        }

        whenever(context.getString(R.string.label_file_size_byte, format))
            .thenReturn("$format B")
        whenever(context.getString(R.string.label_file_size_kilo_byte, format))
            .thenReturn("$format KB")
        whenever(context.getString(R.string.label_file_size_mega_byte, format))
            .thenReturn("$format MB")
        whenever(context.getString(R.string.label_file_size_giga_byte, format))
            .thenReturn("$format GB")
        whenever(context.getString(R.string.label_file_size_tera_byte, format))
            .thenReturn("$format TB")
        whenever(context.getString(R.string.label_file_size_peta_byte, format))
            .thenReturn("$format PB")
        whenever(context.getString(R.string.label_file_size_exa_byte, format))
            .thenReturn("$format EB")

        val expected = when {
            size < kilobyte -> context.getString(R.string.label_file_size_byte, format)
            size < megabyte -> context.getString(R.string.label_file_size_kilo_byte, format)
            size < gigabyte -> context.getString(R.string.label_file_size_mega_byte, format)
            size < terabyte -> context.getString(R.string.label_file_size_giga_byte, format)
            size < petabyte -> context.getString(R.string.label_file_size_tera_byte, format)
            size < exabyte -> context.getString(R.string.label_file_size_peta_byte, format)
            else -> context.getString(R.string.label_file_size_exa_byte, format)
        }

        val actual = underTest(size)

        assertThat(actual).isEqualTo(expected)
    }
}
