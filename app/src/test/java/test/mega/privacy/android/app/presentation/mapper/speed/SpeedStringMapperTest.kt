package test.mega.privacy.android.app.presentation.mapper.speed

import android.content.Context
import com.google.common.truth.Truth
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.mapper.speed.SpeedStringMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.text.DecimalFormat

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SpeedStringMapperTest {
    private lateinit var underTest: SpeedStringMapper

    private var context: Context = mock()

    private val kilobyte = 1024f
    private val megabyte = kilobyte * 1024
    private val gigabyte = megabyte * 1024
    private val terabyte = gigabyte * 1024

    private val df = DecimalFormat("#.##")

    @BeforeAll
    fun setUp() {
        underTest = SpeedStringMapper(
            context = context,
        )
    }

    @ParameterizedTest(name = "when speed is {0} bytes/s")
    @ValueSource(longs = [100, 1024, 1048576, 1073741824, 1099511627776])
    fun `test that mapper returns the correct formatted string`(speed: Long) {
        val format = when {
            speed < kilobyte -> speed.toString()
            speed < megabyte -> df.format(speed / kilobyte)
            speed < gigabyte -> df.format(speed / megabyte)
            speed < terabyte -> df.format(speed / gigabyte)
            else -> df.format(speed / terabyte)
        }

        whenever(context.getString(R.string.label_file_speed_byte, format))
            .thenReturn("$format B/s")
        whenever(context.getString(R.string.label_file_speed_kilo_byte, format))
            .thenReturn("$format KB/s")
        whenever(context.getString(R.string.label_file_speed_mega_byte, format))
            .thenReturn("$format MB/s")
        whenever(context.getString(R.string.label_file_speed_giga_byte, format))
            .thenReturn("$format GB/s")
        whenever(context.getString(R.string.label_file_speed_tera_byte, format))
            .thenReturn("$format TB/s")

        val expected = when {
            speed < kilobyte -> context.getString(R.string.label_file_speed_byte, format)
            speed < megabyte -> context.getString(R.string.label_file_speed_kilo_byte, format)
            speed < gigabyte -> context.getString(R.string.label_file_speed_mega_byte, format)
            speed < terabyte -> context.getString(R.string.label_file_speed_giga_byte, format)
            else -> context.getString(R.string.label_file_speed_tera_byte, format)
        }

        val actual = underTest(speed)

        Truth.assertThat(actual).isEqualTo(expected)
    }
}
