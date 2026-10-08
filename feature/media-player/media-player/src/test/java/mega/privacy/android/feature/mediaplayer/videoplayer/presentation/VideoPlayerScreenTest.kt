package mega.privacy.android.feature.mediaplayer.videoplayer.presentation

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.feature.mediaplayer.videoplayer.presentation.model.VideoPlayerLaunchSourceType
import mega.privacy.android.feature.mediaplayer.videoplayer.presentation.model.VideoPlayerUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@Config(sdk = [34])
@RunWith(AndroidJUnit4::class)
class VideoPlayerScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun data(
        videoWidth: Int = 1920,
        videoHeight: Int = 1080,
        isLoading: Boolean = false,
        playWhenReady: Boolean = false,
    ) = VideoPlayerUiState.Data(
        currentPlayingHandle = 1L,
        currentPlayingItemName = "video.mp4",
        launchSourceType = VideoPlayerLaunchSourceType.VideoBrowse,
        videoWidth = videoWidth,
        videoHeight = videoHeight,
        isLoading = isLoading,
        playWhenReady = playWhenReady,
    )

    /**
     * Rendered inside a fixed box rather than the whole screen, so the bounds the picture is laid
     * out to are a known number instead of whatever the test device reports.
     */
    private fun setScreen(
        uiState: VideoPlayerUiState,
        onPlayPauseClick: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            Box(modifier = Modifier.size(CONTAINER_WIDTH, CONTAINER_HEIGHT)) {
                VideoPlayerScreen(
                    uiState = uiState,
                    onSurfaceReady = {},
                    onSurfaceReleased = {},
                    onPlayPauseClick = onPlayPauseClick,
                )
            }
        }
    }

    @Test
    fun `test that no picture is shown while the launch is being resolved`() {
        setScreen(VideoPlayerUiState.Loading)

        composeTestRule.onNodeWithTag(VIDEO_SURFACE_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that no picture is shown when the launch source is unusable`() {
        setScreen(VideoPlayerUiState.InvalidLaunchSource)

        composeTestRule.onNodeWithTag(VIDEO_SURFACE_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that the picture fills the container before the video size is known`() {
        setScreen(data(videoWidth = 0, videoHeight = 0))

        val bounds = composeTestRule.onNodeWithTag(VIDEO_SURFACE_TEST_TAG)
            .assertIsDisplayed()
            .getBoundsInRoot()

        assertThat((bounds.right.value - bounds.left.value)).isWithin(TOLERANCE).of(CONTAINER_WIDTH.value)
        assertThat((bounds.bottom.value - bounds.top.value)).isWithin(TOLERANCE).of(CONTAINER_HEIGHT.value)
    }

    @Test
    fun `test that the picture is laid out to the video aspect ratio`() {
        // 16:9 inside a 4:3 container: the width is taken and the height derived, which is what
        // letterboxing a landscape video looks like.
        setScreen(data(videoWidth = 1920, videoHeight = 1080))

        val bounds = composeTestRule.onNodeWithTag(VIDEO_SURFACE_TEST_TAG)
            .assertIsDisplayed()
            .getBoundsInRoot()

        assertThat((bounds.right.value - bounds.left.value)).isWithin(TOLERANCE).of(CONTAINER_WIDTH.value)
        assertThat((bounds.bottom.value - bounds.top.value)).isWithin(TOLERANCE).of(CONTAINER_WIDTH.value * 1080f / 1920f)
    }

    @Test
    fun `test that a portrait video is laid out to the container height`() {
        // 9:16 inside a 4:3 container: taking the width would overflow, so the height is taken
        // instead and the width derived.
        setScreen(data(videoWidth = 1080, videoHeight = 1920))

        val bounds = composeTestRule.onNodeWithTag(VIDEO_SURFACE_TEST_TAG)
            .assertIsDisplayed()
            .getBoundsInRoot()

        assertThat((bounds.bottom.value - bounds.top.value)).isWithin(TOLERANCE).of(CONTAINER_HEIGHT.value)
        assertThat((bounds.right.value - bounds.left.value))
            .isWithin(TOLERANCE).of(CONTAINER_HEIGHT.value * 1080f / 1920f)
    }

    @Test
    fun `test that the loading indicator is shown while the launch is being resolved`() {
        setScreen(VideoPlayerUiState.Loading)

        composeTestRule.onNodeWithTag(VIDEO_PLAYER_LOADING_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that no loading indicator is shown when the launch source is unusable`() {
        setScreen(VideoPlayerUiState.InvalidLaunchSource)

        // The screen is closing, so a spinner would flash on the way out.
        composeTestRule.onNodeWithTag(VIDEO_PLAYER_LOADING_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that the launch spinner gives way to the play pause button once the launch resolves`() {
        // Two spinners at once is what this guards against: from Data onwards the one inside the
        // play/pause button is the only one, matching the live player.
        setScreen(data(isLoading = true))

        composeTestRule.onNodeWithTag(VIDEO_PLAYER_LOADING_TEST_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithTag(VIDEO_PLAYER_PLAY_PAUSE_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that the play pause button shows no icon while the player works towards a picture`() {
        setScreen(data(isLoading = true, playWhenReady = true))

        composeTestRule.onNodeWithContentDescription(PAUSE_DESCRIPTION).assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription(PLAY_DESCRIPTION).assertDoesNotExist()
    }

    @Test
    fun `test that the play pause button shows pause while playback is running`() {
        setScreen(data(isLoading = false, playWhenReady = true))

        composeTestRule.onNodeWithContentDescription(PAUSE_DESCRIPTION).assertIsDisplayed()
    }

    @Test
    fun `test that the play pause button shows play while playback is paused`() {
        setScreen(data(isLoading = false, playWhenReady = false))

        composeTestRule.onNodeWithContentDescription(PLAY_DESCRIPTION).assertIsDisplayed()
    }

    @Test
    fun `test that tapping the play pause button reports the click`() {
        var clicks = 0
        setScreen(data(isLoading = false, playWhenReady = true), onPlayPauseClick = { clicks++ })

        composeTestRule.onNodeWithTag(VIDEO_PLAYER_PLAY_PAUSE_TEST_TAG).performClick()

        assertThat(clicks).isEqualTo(1)
    }

    @Test
    fun `test that no controls are shown while the launch is being resolved`() {
        setScreen(VideoPlayerUiState.Loading)

        composeTestRule.onNodeWithTag(VIDEO_PLAYER_PLAY_PAUSE_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that no controls are shown when the launch source is unusable`() {
        setScreen(VideoPlayerUiState.InvalidLaunchSource)

        composeTestRule.onNodeWithTag(VIDEO_PLAYER_PLAY_PAUSE_TEST_TAG).assertDoesNotExist()
    }

    private companion object {
        /**
         * 4:3, and small enough to fit the default Robolectric screen — a container wider than
         * the screen is clipped by the root constraints and the measured bounds stop matching
         * what was asked for.
         */
        val CONTAINER_WIDTH = 240.dp
        val CONTAINER_HEIGHT = 180.dp
        const val TOLERANCE = 1f

        /** What `VideoPlayerPlayPauseButton` labels its icon with; it carries no string resource. */
        const val PLAY_DESCRIPTION = "Play"
        const val PAUSE_DESCRIPTION = "Pause"
    }
}
