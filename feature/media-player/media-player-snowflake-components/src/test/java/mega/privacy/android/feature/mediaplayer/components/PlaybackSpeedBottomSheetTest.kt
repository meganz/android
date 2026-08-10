package mega.privacy.android.feature.mediaplayer.components

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PlaybackSpeedBottomSheetTest {

    @ParameterizedTest(name = "snapToNearestTick({0}) == {1}")
    @CsvSource(
        "0.5,  0.5",   // lower boundary
        "2.0,  2.0",   // upper boundary
        "1.0,  1.0",   // exact tick — integer speed
        "1.25, 1.25",  // exact tick — preset speed
        "1.27, 1.25",  // rounds down to nearest 0.05 tick
        "1.28, 1.30",  // rounds up to nearest 0.05 tick
        "0.49, 0.5",   // below minimum clamps to 0.5
        "2.01, 2.0",   // above maximum clamps to 2.0
        "0.99, 1.0",   // accumulation-safe: avoids 0.9500001 artefact
        "1.49, 1.5",   // accumulation-safe: avoids 1.4900001 artefact
    )
    fun `test that snapToNearestTick returns expected tick value`(input: Float, expected: Float) {
        assertThat(snapToNearestTick(input)).isEqualTo(expected)
    }

    @ParameterizedTest(name = "formatPlaybackSpeed({0}) == \"{1}\"")
    @CsvSource(
        "1.0,  1x",
        "2.0,  2x",
        "1.5,  1.5x",
        "1.25, 1.25x",
        "1.75, 1.75x",
        "0.5,  0.5x",
    )
    fun `test that formatPlaybackSpeed formats speed with x suffix`(
        input: Float,
        expected: String,
    ) {
        assertThat(formatPlaybackSpeed(input)).isEqualTo(expected)
    }
}
