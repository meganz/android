package mega.privacy.android.feature.mediaplayer.presentation.miniplayer

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews

/**
 * Baseline screenshots for [AudioMiniPlayer].
 *
 * The bar follows the app theme rather than the always-dark full-screen player, so every state is
 * recorded in both themes. That is what the throbber needs: it is drawn on a canvas and takes its
 * colour explicitly, and a light-theme golden is the only thing that catches it turning invisible.
 */
class AudioMiniPlayerScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun AudioMiniPlayerPlaying() {
        AndroidThemeForPreviews {
            AudioMiniPlayer(
                state = AudioMiniPlayerUiState.Data(
                    title = "Pioneer To The Falls (Live)",
                    artist = "Interpol",
                    isPlaying = true,
                    progress = 0.35f,
                    isLoading = false,
                ),
                onPlayPauseClicked = {},
                onCloseClicked = {},
                onPlayerClicked = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun AudioMiniPlayerPaused() {
        AndroidThemeForPreviews {
            AudioMiniPlayer(
                state = AudioMiniPlayerUiState.Data(
                    title = "Orange (Live)",
                    artist = "Arcy Drive",
                    isPlaying = false,
                    progress = 0.7f,
                    isLoading = false,
                ),
                onPlayPauseClicked = {},
                onCloseClicked = {},
                onPlayerClicked = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun AudioMiniPlayerBuffering() {
        AndroidThemeForPreviews {
            AudioMiniPlayer(
                state = AudioMiniPlayerUiState.Data(
                    title = "Superbloomer (Live)",
                    artist = "Arcy Drive",
                    isLoading = true,
                    isPlaying = false,
                    progress = 0f,
                ),
                onPlayPauseClicked = {},
                onCloseClicked = {},
                onPlayerClicked = {},
            )
        }
    }

    /** A file whose metadata carries no artist: the second line is dropped, not left blank. */
    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun AudioMiniPlayerWithoutArtist() {
        AndroidThemeForPreviews {
            AudioMiniPlayer(
                state = AudioMiniPlayerUiState.Data(
                    title = "Interview part two",
                    isPlaying = true,
                    progress = 0.1f,
                    artist = null,
                    isLoading = false,
                ),
                onPlayPauseClicked = {},
                onCloseClicked = {},
                onPlayerClicked = {},
            )
        }
    }

    /** Titles long enough to marquee still have to leave the close button room. */
    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun AudioMiniPlayerLongTitle() {
        AndroidThemeForPreviews {
            AudioMiniPlayer(
                state = AudioMiniPlayerUiState.Data(
                    title = "midnight_drive_chill_synthwave_city_lights_night_vibes_track_01_2026.wav",
                    artist = "An artist name long enough to need the marquee as well",
                    isPlaying = true,
                    progress = 0.5f,
                    isLoading = false,
                ),
                onPlayPauseClicked = {},
                onCloseClicked = {},
                onPlayerClicked = {},
            )
        }
    }
}
