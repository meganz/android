package mega.privacy.android.feature.mediaplayer.presentation

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.android.core.ui.theme.AndroidTheme
import mega.privacy.android.feature.mediaplayer.data.model.AudioQueueItem
import mega.privacy.android.feature.mediaplayer.presentation.model.AudioPlayerQueueUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@Config(sdk = [34])
@RunWith(AndroidJUnit4::class)
class AudioPlayerQueueContentTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun defaultData() = AudioPlayerQueueUiState.Data(
        items = listOf(
            AudioQueueItem(mediaId = "a", title = "Track A", artist = "Artist A"),
            AudioQueueItem(mediaId = "b", title = "Track B", artist = null),
        ),
        currentQueueIndex = 0,
        currentTitle = "Track A",
        currentArtist = "Artist A",
        currentArtworkUri = null,
        currentThumbnailData = null,
        isContinuousPlayback = false,
        playingFromName = null,
    )

    private fun setContent(uiState: AudioPlayerQueueUiState) {
        composeTestRule.setContent {
            // The content no longer applies a theme itself — the player owns it in production,
            // so the test has to supply the same one to render what users actually see.
            AndroidTheme(isDark = true) {
                AudioPlayerQueueContent(
                    uiState = uiState,
                    onQueueItemClick = {},
                    onQueueItemMoved = { _, _ -> },
                    onSetContinuousPlayback = {},
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    @Test
    fun `test that loading placeholder is shown when uiState is Loading`() {
        setContent(AudioPlayerQueueUiState.Loading)

        composeTestRule.onNodeWithTag(AUDIO_PLAYER_QUEUE_LOADING_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that queue list is not shown when uiState is Loading`() {
        setContent(AudioPlayerQueueUiState.Loading)

        composeTestRule.onNodeWithTag(AUDIO_PLAYER_QUEUE_CONTENT_TAG, useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun `test that queue list is shown when uiState is Data`() {
        setContent(defaultData())

        composeTestRule.onNodeWithTag(AUDIO_PLAYER_QUEUE_CONTENT_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that loading placeholder is not shown when uiState is Data`() {
        setContent(defaultData())

        composeTestRule.onNodeWithTag(AUDIO_PLAYER_QUEUE_LOADING_TAG, useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun `test that queue items are displayed when uiState is Data`() {
        setContent(defaultData())

        // The current track renders twice: once in the currently-playing header, once as its row.
        composeTestRule.onAllNodesWithText("Track A", useUnmergedTree = true)
            .assertCountEquals(2)
        composeTestRule.onNodeWithText("Track B", useUnmergedTree = true).assertIsDisplayed()
    }
}
