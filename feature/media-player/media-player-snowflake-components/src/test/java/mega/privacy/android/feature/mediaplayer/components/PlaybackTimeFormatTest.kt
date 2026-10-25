package mega.privacy.android.feature.mediaplayer.components

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PlaybackTimeFormatTest {

    @ParameterizedTest(name = "formatPlaybackTime({0}) == {1}")
    @CsvSource(
        "0,        0:00",       // unset, which the player reports before it knows the length
        "-1000,    0:00",       // negative, same treatment as unset
        "999,      0:00",       // below a second truncates rather than rounds up
        "1000,     0:01",       // seconds stay zero-padded
        "59000,    0:59",       // last second before the minute rolls over
        "60000,    1:00",       // minute boundary
        "295000,   4:55",       // ordinary track length
        "3599000,  59:59",      // last second before the hour rolls over
        "3600000,  '1:00:00'",  // hour boundary adds the hour field
        "3661000,  '1:01:01'",  // hours keep minutes and seconds zero-padded
    )
    fun `test that formatPlaybackTime formats the milliseconds as expected`(
        ms: Long,
        expected: String,
    ) {
        assertThat(formatPlaybackTime(ms)).isEqualTo(expected)
    }
}
