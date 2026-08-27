package mega.privacy.android.feature.mediaplayer.presentation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.media3.common.Player
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.feature.mediaplayer.presentation.model.AudioPlayerUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.annotation.Config

@Config(sdk = [34])
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
        nodeSourceType = NodeSourceType.MEDIA_PLAYER_DEFAULT,
        fileLinkUrl = null,
        localFilePath = null,
        chatId = null,
        msgId = null,
        currentPlaybackSpeed = 1f,
    )

    private fun setContent(
        uiState: AudioPlayerUiState = defaultData(),
        isPodcastMode: Boolean = true,
        onPlayPauseClicked: () -> Unit = {},
        onSeekTo: (Long) -> Unit = {},
        onNextClicked: () -> Unit = {},
        onPreviousClicked: () -> Unit = {},
        onShuffleClicked: () -> Unit = {},
        onRepeatClicked: () -> Unit = {},
        onPlaylistClicked: () -> Unit = {},
        onBackPressed: () -> Unit = {},
        onMoreActionsClicked: () -> Unit = {},
        onToggleMode: () -> Unit = {},
        onSeekForward15: () -> Unit = {},
        onSeekBackward15: () -> Unit = {},
        onSpeedClicked: () -> Unit = {},
        onSleepTimerClicked: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            AudioPlayerScreen(
                uiState = uiState,
                isPodcastMode = isPodcastMode,
                onPlayPauseClicked = onPlayPauseClicked,
                onSeekTo = onSeekTo,
                onNextClicked = onNextClicked,
                onPreviousClicked = onPreviousClicked,
                onShuffleClicked = onShuffleClicked,
                onRepeatClicked = onRepeatClicked,
                onPlaylistClicked = onPlaylistClicked,
                onBackPressed = onBackPressed,
                onMoreActionsClicked = onMoreActionsClicked,
                onToggleMode = onToggleMode,
                onSeekForward15 = onSeekForward15,
                onSeekBackward15 = onSeekBackward15,
                onSpeedClicked = onSpeedClicked,
                onSleepTimerClicked = onSleepTimerClicked,
            )
        }
    }

    // region Loading state

    @Test
    fun `test that player content is shown when uiState is Loading`() {
        setContent(uiState = AudioPlayerUiState.Loading)

        composeTestRule.onNodeWithTag(AUDIO_PLAYER_CONTENT_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
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
        setContent(uiState = AudioPlayerUiState.Loading, isPodcastMode = false)

        composeTestRule.onNodeWithContentDescription("Previous").assertIsNotEnabled()
    }

    @Test
    fun `test that next button is disabled when uiState is Loading`() {
        setContent(uiState = AudioPlayerUiState.Loading, isPodcastMode = false)

        composeTestRule.onNodeWithContentDescription("Next").assertIsNotEnabled()
    }

    @Test
    fun `test that next button is enabled when uiState is Data with isLoading true`() {
        setContent(uiState = defaultData(isLoading = true), isPodcastMode = false)

        composeTestRule.onNodeWithContentDescription("Next").assertIsEnabled()
    }

    @Test
    fun `test that previous button is enabled when uiState is Data with isLoading true`() {
        setContent(uiState = defaultData(isLoading = true), isPodcastMode = false)

        composeTestRule.onNodeWithContentDescription("Previous").assertIsEnabled()
    }

    @Test
    fun `test that seek forward button is enabled when uiState is Data with isLoading true`() {
        setContent(uiState = defaultData(isLoading = true), isPodcastMode = true)

        composeTestRule.onNodeWithContentDescription("Seek forward 15 seconds").assertIsEnabled()
    }

    // endregion

    // region Data state – content visibility

    @Test
    fun `test that player content is shown when uiState is Data`() {
        setContent(uiState = defaultData())

        composeTestRule.onNodeWithTag(AUDIO_PLAYER_CONTENT_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
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
        setContent(
            uiState = defaultData(
                title = null,
                currentPlayingItemName = "podcast_episode_42.mp3"
            )
        )

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
            isPodcastMode = false,
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
            isPodcastMode = false,
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
            isPodcastMode = false,
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
            isPodcastMode = false,
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

    // region Podcast mode – content visibility

    @Test
    fun `test that seek backward 15 button is shown in podcast mode`() {
        setContent(uiState = defaultData(), isPodcastMode = true)

        composeTestRule.onNodeWithContentDescription("Seek backward 15 seconds").assertIsDisplayed()
    }

    @Test
    fun `test that seek forward 15 button is shown in podcast mode`() {
        setContent(uiState = defaultData(), isPodcastMode = true)

        composeTestRule.onNodeWithContentDescription("Seek forward 15 seconds").assertIsDisplayed()
    }

    @Test
    fun `test that sleep timer button is shown in podcast mode`() {
        setContent(uiState = defaultData(), isPodcastMode = true)

        composeTestRule.onNodeWithContentDescription("Sleep timer").assertIsDisplayed()
    }

    @Test
    fun `test that speed indicator is shown in podcast mode`() {
        setContent(uiState = defaultData(), isPodcastMode = true)

        composeTestRule.onNodeWithText("1x", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `test that seek backward 15 button is enabled when isLoading is true in podcast mode`() {
        setContent(uiState = defaultData(isLoading = true), isPodcastMode = true)

        composeTestRule.onNodeWithContentDescription("Seek backward 15 seconds")
            .assertIsEnabled()
    }

    // endregion

    // region Podcast mode – callbacks

    @Test
    fun `test that onSeekBackward15 is invoked when seek backward 15 button is clicked`() {
        val onSeekBackward15 = mock<() -> Unit>()
        setContent(
            uiState = defaultData(isLoading = false),
            isPodcastMode = true,
            onSeekBackward15 = onSeekBackward15,
        )

        composeTestRule.onNodeWithContentDescription("Seek backward 15 seconds").performClick()

        verify(onSeekBackward15).invoke()
    }

    @Test
    fun `test that onSeekForward15 is invoked when seek forward 15 button is clicked`() {
        val onSeekForward15 = mock<() -> Unit>()
        setContent(
            uiState = defaultData(isLoading = false),
            isPodcastMode = true,
            onSeekForward15 = onSeekForward15,
        )

        composeTestRule.onNodeWithContentDescription("Seek forward 15 seconds").performClick()

        verify(onSeekForward15).invoke()
    }

    @Test
    fun `test that onSleepTimerClicked is invoked when sleep timer button is clicked`() {
        val onSleepTimerClicked = mock<() -> Unit>()
        setContent(
            uiState = defaultData(),
            isPodcastMode = true,
            onSleepTimerClicked = onSleepTimerClicked,
        )

        composeTestRule.onNodeWithContentDescription("Sleep timer").performClick()

        verify(onSleepTimerClicked).invoke()
    }

    @Test
    fun `test that onSpeedClicked is invoked when speed indicator is clicked`() {
        val onSpeedClicked = mock<() -> Unit>()
        setContent(
            uiState = defaultData(),
            isPodcastMode = true,
            onSpeedClicked = onSpeedClicked,
        )

        composeTestRule.onNodeWithText("1x", useUnmergedTree = true).performClick()

        verify(onSpeedClicked).invoke()
    }

    // endregion

    // region Mode toggle – callbacks

    @Test
    fun `test that onToggleMode is invoked when podcast mode button is clicked`() {
        val onToggleMode = mock<() -> Unit>()
        setContent(
            uiState = defaultData(),
            isPodcastMode = true,
            onToggleMode = onToggleMode,
        )

        composeTestRule.onNodeWithText("Podcast mode", useUnmergedTree = true).performClick()

        verify(onToggleMode).invoke()
    }

    @Test
    fun `test that onToggleMode is invoked when music mode button is clicked`() {
        val onToggleMode = mock<() -> Unit>()
        setContent(
            uiState = defaultData(),
            isPodcastMode = false,
            onToggleMode = onToggleMode,
        )

        composeTestRule.onNodeWithText("Music mode", useUnmergedTree = true).performClick()

        verify(onToggleMode).invoke()
    }

    // endregion
}
