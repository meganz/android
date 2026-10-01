package mega.privacy.android.feature.mediaplayer.components

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import java.util.Locale

/**
 * Screenshot tests for [AudioPlayerSeekBar], covering the presence of the optional sleep timer
 * label, which is the only part of the bar a caller can turn on and off.
 */
class AudioPlayerSeekBarScreenshotTest {

    private companion object {
        // The position and duration labels are built with String.format, which falls back to the
        // JVM's default formatting locale. A locale that renders digits in another script would
        // produce goldens that only match on the machine that recorded them.
        //
        // The component renders durations in the default locale on purpose, as the app-wide
        // DurationInSecondsTextMapper and Media3's own time labels do, so the locale is pinned
        // here rather than in the component.
        init {
            Locale.setDefault(Locale.US)
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun SeekBarWithoutSleepTimer() {
        AndroidThemeForPreviews {
            AudioPlayerSeekBar(
                currentPosition = 65_000L,
                duration = 240_000L,
                onSeekTo = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun SeekBarWithSleepTimer() {
        AndroidThemeForPreviews {
            AudioPlayerSeekBar(
                currentPosition = 65_000L,
                duration = 240_000L,
                onSeekTo = {},
                sleepTimerLabel = "14:59",
            )
        }
    }
}
