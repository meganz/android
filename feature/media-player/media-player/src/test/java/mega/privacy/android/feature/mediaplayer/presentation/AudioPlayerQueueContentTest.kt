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

    @Test
    fun `test that a queue item shows its artist and duration`() {
        setContent(
            defaultData().copy(
                items = listOf(
                    AudioQueueItem(mediaId = "a", title = "Track A", artist = "Artist A"),
                    AudioQueueItem(
                        mediaId = "b",
                        title = "Track B",
                        // Distinct from the artist of the currently playing row above the list,
                        // so the assertions below can only match the track row.
                        artist = "Queue artist",
                        durationMs = 295_000L,
                    ),
                ),
            )
        )

        composeTestRule.onNodeWithText("Queue artist").assertIsDisplayed()
        composeTestRule.onNodeWithText(" \u2022 4:55").assertIsDisplayed()
    }

    @Test
    fun `test that a queue item shows its duration alone when it has no artist`() {
        setContent(
            defaultData().copy(
                items = listOf(
                    AudioQueueItem(mediaId = "a", title = "Track A", artist = "Artist A"),
                    AudioQueueItem(
                        mediaId = "b",
                        title = "Track B",
                        artist = null,
                        durationMs = 295_000L,
                    ),
                ),
            )
        )

        composeTestRule.onNodeWithText("4:55").assertIsDisplayed()
    }

    @Test
    fun `test that a queue item shows no duration when it has none`() {
        setContent(
            defaultData().copy(
                items = listOf(
                    AudioQueueItem(mediaId = "a", title = "Track A", artist = "Artist A"),
                    AudioQueueItem(
                        mediaId = "b",
                        title = "Track B",
                        artist = "Queue artist",
                        durationMs = 295_000L,
                    ),
                ),
            )
        )

        // Exactly one row carries a length, so the count distinguishes "the row without one
        // shows nothing" from "no row renders a length at all".
        composeTestRule.onAllNodesWithText(" • 4:55").assertCountEquals(1)
    }

    @Test
    fun `test that the duration stays on the row when the artist is too long to fit`() {
        setContent(
            defaultData().copy(
                items = listOf(
                    AudioQueueItem(mediaId = "a", title = "Track A", artist = "Artist A"),
                    AudioQueueItem(
                        mediaId = "b",
                        title = "Track B",
                        artist = "An artist with a name far too long to leave room for anything " +
                                "else on this line of the queue row",
                        durationMs = 295_000L,
                    ),
                ),
            )
        )

        // The artist gives up its width first, so the length is never pushed off the row.
        composeTestRule.onNodeWithText(" • 4:55").assertIsDisplayed()
    }

    @Test
    fun `test that the current queue item shows the position within its duration`() {
        setContent(
            defaultData().copy(
                items = listOf(
                    AudioQueueItem(
                        mediaId = "a",
                        title = "Track A",
                        // Distinct from the artist of the currently playing row above the list,
                        // so the assertion below can only match the track row.
                        artist = "Queue artist",
                        durationMs = 295_000L,
                    ),
                ),
                currentPositionMs = 13_000L,
                currentDurationMs = 295_000L,
            )
        )

        composeTestRule.onNodeWithText(" \u2022 0:13/4:55").assertIsDisplayed()
    }

    @Test
    fun `test that a queue item that is not playing shows no position`() {
        setContent(
            defaultData().copy(
                items = listOf(
                    AudioQueueItem(mediaId = "a", title = "Track A", artist = "Artist A"),
                    AudioQueueItem(
                        mediaId = "b",
                        title = "Track B",
                        artist = "Queue artist",
                        durationMs = 295_000L,
                    ),
                ),
                currentPositionMs = 13_000L,
            )
        )

        composeTestRule.onNodeWithText(" \u2022 4:55").assertIsDisplayed()
        composeTestRule.onAllNodesWithText(" \u2022 0:13/4:55").assertCountEquals(0)
    }

    @Test
    fun `test that the currently playing row shows the track length without a position`() {
        setContent(
            defaultData().copy(
                currentArtist = "Artist A",
                currentPositionMs = 13_000L,
                currentDurationMs = 295_000L,
            )
        )

        composeTestRule.onNodeWithText(" \u2022 4:55").assertIsDisplayed()
        composeTestRule.onAllNodesWithText(" \u2022 0:13/4:55").assertCountEquals(0)
    }

    @Test
    fun `test that the currently playing row shows no time before the duration is known`() {
        setContent(
            defaultData().copy(
                currentArtist = "Artist A",
                currentPositionMs = 0L,
                currentDurationMs = 0L,
            )
        )

        composeTestRule.onAllNodesWithText(" \u2022 0:00").assertCountEquals(0)
    }
}
