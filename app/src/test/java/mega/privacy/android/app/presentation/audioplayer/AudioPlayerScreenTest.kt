package mega.privacy.android.app.presentation.audioplayer

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.media3.common.Player
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.mediaplayer.AUDIO_PLAYER_CONTENT_TAG
import mega.privacy.android.app.mediaplayer.AudioPlayerScreen
import mega.privacy.android.app.mediaplayer.model.AudioPlayerUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class AudioPlayerScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun defaultData(
        isPlaying: Boolean = false,
        isLoading: Boolean = false,
        title: String? = null,
        artist: String? = null,
        currentPlayingItemName: String? = null,
        repeatMode: Int = Player.REPEAT_MODE_OFF,
        shuffleEnabled: Boolean = false,
    ) = AudioPlayerUiState.Data(
        isPlaying = isPlaying,
        isLoading = isLoading,
        currentPosition = 0L,
        duration = 0L,
        title = title,
        artist = artist,
        artworkUri = null,
        repeatMode = repeatMode,
        shuffleEnabled = shuffleEnabled,
        currentPlayingHandle = -1L,
        currentPlayingItemName = currentPlayingItemName,
        hasPlaylist = false,
        currentAdapterType = -1,
        thumbnailData = null,
    )

    private fun setContent(
        uiState: AudioPlayerUiState = defaultData(),
        onPlayPauseClicked: () -> Unit = {},
        onSeekTo: (Long) -> Unit = {},
        onNextClicked: () -> Unit = {},
        onPreviousClicked: () -> Unit = {},
        onShuffleClicked: () -> Unit = {},
        onRepeatClicked: () -> Unit = {},
        onPlaylistClicked: () -> Unit = {},
        onScreenClicked: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            AudioPlayerScreen(
                uiState = uiState,
                onPlayPauseClicked = onPlayPauseClicked,
                onSeekTo = onSeekTo,
                onNextClicked = onNextClicked,
                onPreviousClicked = onPreviousClicked,
                onShuffleClicked = onShuffleClicked,
                onRepeatClicked = onRepeatClicked,
                onPlaylistClicked = onPlaylistClicked,
                onScreenClicked = onScreenClicked,
            )
        }
    }

    // region Loading state

    @Test
    fun `test that player content is shown when uiState is Loading`() {
        setContent(uiState = AudioPlayerUiState.Loading)

        composeTestRule.onNodeWithTag(AUDIO_PLAYER_CONTENT_TAG, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `test that loader throbber is shown when uiState is Loading`() {
        setContent(uiState = AudioPlayerUiState.Loading)

        composeTestRule.onNodeWithContentDescription("Loading").assertIsDisplayed()
    }

    @Test
    fun `test that play pause button is disabled when uiState is Loading`() {
        setContent(uiState = AudioPlayerUiState.Loading)

        composeTestRule.onNodeWithContentDescription("Loading").assertIsNotEnabled()
    }

    @Test
    fun `test that previous button is disabled when uiState is Loading`() {
        setContent(uiState = AudioPlayerUiState.Loading)

        composeTestRule.onNodeWithContentDescription("Previous").assertIsNotEnabled()
    }

    @Test
    fun `test that next button is disabled when uiState is Loading`() {
        setContent(uiState = AudioPlayerUiState.Loading)

        composeTestRule.onNodeWithContentDescription("Next").assertIsNotEnabled()
    }

    // endregion

    // region Data state – content visibility

    @Test
    fun `test that player content is shown when uiState is Data`() {
        setContent(uiState = defaultData())

        composeTestRule.onNodeWithTag(AUDIO_PLAYER_CONTENT_TAG, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `test that loader throbber is shown when uiState is Data with isLoading true`() {
        setContent(uiState = defaultData(isLoading = true))

        composeTestRule.onNodeWithContentDescription("Loading").assertIsDisplayed()
    }

    @Test
    fun `test that title is displayed when uiState has title`() {
        setContent(uiState = defaultData(title = "Bohemian Rhapsody"))

        composeTestRule
            .onNodeWithText("Bohemian Rhapsody", substring = true, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that item name is displayed as title when title is null`() {
        setContent(uiState = defaultData(title = null, currentPlayingItemName = "podcast_episode_42.mp3"))

        composeTestRule
            .onNodeWithText("podcast_episode_42.mp3", substring = true, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that artist is displayed when uiState has artist`() {
        setContent(uiState = defaultData(artist = "Queen"))

        composeTestRule
            .onNodeWithText("Queen", substring = true, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that play button is shown when not playing and not loading`() {
        setContent(uiState = defaultData(isPlaying = false, isLoading = false))

        composeTestRule.onNodeWithContentDescription("Play").assertIsDisplayed()
    }

    @Test
    fun `test that pause button is shown when playing`() {
        setContent(uiState = defaultData(isPlaying = true, isLoading = false))

        composeTestRule.onNodeWithContentDescription("Pause").assertIsDisplayed()
    }

    // endregion

    // region Data state – callbacks

    @Test
    fun `test that onPlayPauseClicked is invoked when play button is clicked`() {
        val onPlayPauseClicked = mock<() -> Unit>()
        setContent(
            uiState = defaultData(isPlaying = false, isLoading = false),
            onPlayPauseClicked = onPlayPauseClicked,
        )

        composeTestRule.onNodeWithContentDescription("Play").performClick()

        verify(onPlayPauseClicked).invoke()
    }

    @Test
    fun `test that onPlayPauseClicked is invoked when pause button is clicked`() {
        val onPlayPauseClicked = mock<() -> Unit>()
        setContent(
            uiState = defaultData(isPlaying = true, isLoading = false),
            onPlayPauseClicked = onPlayPauseClicked,
        )

        composeTestRule.onNodeWithContentDescription("Pause").performClick()

        verify(onPlayPauseClicked).invoke()
    }

    @Test
    fun `test that onNextClicked is invoked when next button is clicked`() {
        val onNextClicked = mock<() -> Unit>()
        setContent(
            uiState = defaultData(isLoading = false),
            onNextClicked = onNextClicked,
        )

        composeTestRule.onNodeWithContentDescription("Next").performClick()

        verify(onNextClicked).invoke()
    }

    @Test
    fun `test that onPreviousClicked is invoked when previous button is clicked`() {
        val onPreviousClicked = mock<() -> Unit>()
        setContent(
            uiState = defaultData(isLoading = false),
            onPreviousClicked = onPreviousClicked,
        )

        composeTestRule.onNodeWithContentDescription("Previous").performClick()

        verify(onPreviousClicked).invoke()
    }

    @Test
    fun `test that onShuffleClicked is invoked when shuffle button is clicked`() {
        val onShuffleClicked = mock<() -> Unit>()
        setContent(
            uiState = defaultData(),
            onShuffleClicked = onShuffleClicked,
        )

        composeTestRule.onNodeWithContentDescription("Shuffle").performClick()

        verify(onShuffleClicked).invoke()
    }

    @Test
    fun `test that onRepeatClicked is invoked when repeat button is clicked`() {
        val onRepeatClicked = mock<() -> Unit>()
        setContent(
            uiState = defaultData(repeatMode = Player.REPEAT_MODE_OFF),
            onRepeatClicked = onRepeatClicked,
        )

        composeTestRule.onNodeWithContentDescription("Repeat").performClick()

        verify(onRepeatClicked).invoke()
    }

    @Test
    fun `test that onPlaylistClicked is invoked when playlist button is clicked`() {
        val onPlaylistClicked = mock<() -> Unit>()
        setContent(
            uiState = defaultData(),
            onPlaylistClicked = onPlaylistClicked,
        )

        composeTestRule.onNodeWithContentDescription("Playlist").performClick()

        verify(onPlaylistClicked).invoke()
    }

    // endregion
}
