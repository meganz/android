package mega.privacy.android.app.presentation.settings.camerauploads.tiles

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.settings.camerauploads.model.VideoQualityUiItem
import mega.privacy.android.app.presentation.settings.camerauploads.tiles.VIDEO_QUALITY_TILE
import mega.privacy.android.app.presentation.settings.camerauploads.tiles.VIDEO_QUALITY_TILE_DIVIDER
import mega.privacy.android.app.presentation.settings.camerauploads.tiles.VideoQualityTile
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import mega.privacy.android.app.onNodeWithText

/**
 * Test class for [VideoQualityTile]
 */
@RunWith(AndroidJUnit4::class)
internal class VideoQualityTileTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that the tile is shown`() {
        initializeComposeContent(videoQualityUiItem = VideoQualityUiItem.Low)

        composeTestRule.onNodeWithTag(VIDEO_QUALITY_TILE).assertIsDisplayed()
        composeTestRule.onNodeWithTag(VIDEO_QUALITY_TILE_DIVIDER).assertIsDisplayed()
        composeTestRule.onNodeWithText(R.string.settings_camera_uploads_video_quality_dialog_option_low)
            .assertExists()
    }

    @Test
    fun `test that the tile with correct content is shown when the video quality ui item is low`() {
        initializeComposeContent(videoQualityUiItem = VideoQualityUiItem.Low)

        composeTestRule.onNodeWithText(R.string.settings_camera_uploads_video_quality_dialog_option_low)
            .assertExists()
    }

    @Test
    fun `test that the tile with correct content is shown when the video quality ui item is medium`() {
        initializeComposeContent(videoQualityUiItem = VideoQualityUiItem.Medium)

        composeTestRule.onNodeWithText(R.string.settings_camera_uploads_video_quality_dialog_option_medium)
            .assertExists()
    }

    @Test
    fun `test that the tile with correct content is shown when the video quality ui item is high`() {
        initializeComposeContent(videoQualityUiItem = VideoQualityUiItem.High)

        composeTestRule.onNodeWithText(R.string.settings_camera_uploads_video_quality_dialog_option_high)
            .assertExists()
    }

    @Test
    fun `test that the tile with correct content is shown when the video quality ui item is original`() {
        initializeComposeContent(videoQualityUiItem = VideoQualityUiItem.Original)

        composeTestRule.onNodeWithText(R.string.settings_camera_uploads_video_quality_dialog_option_original)
            .assertExists()
    }

    private fun initializeComposeContent(videoQualityUiItem: VideoQualityUiItem) {
        composeTestRule.setContent {
            VideoQualityTile(
                videoQualityUiItem = videoQualityUiItem,
                onItemClicked = {},
            )
        }
    }
}