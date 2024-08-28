package mega.privacy.android.app.presentation.settings.camerauploads.tiles

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.settings.camerauploads.tiles.VIDEO_COMPRESSION_TILE
import mega.privacy.android.app.presentation.settings.camerauploads.tiles.VIDEO_COMPRESSION_TILE_DIVIDER
import mega.privacy.android.app.presentation.settings.camerauploads.tiles.VideoCompressionTile
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

/**
 * Test class for [VideoCompressionTile]
 */
@RunWith(AndroidJUnit4::class)
internal class VideoCompressionTileTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that the tile is shown`() {
        with(composeTestRule) {
            setContent {
                VideoCompressionTile(
                    maximumNonChargingVideoCompressionSize = 500,
                    onItemClicked = {},
                )
            }

            onNodeWithTag(VIDEO_COMPRESSION_TILE).assertIsDisplayed()
            onNodeWithTag(VIDEO_COMPRESSION_TILE_DIVIDER).assertIsDisplayed()
        }
    }

    @Test
    fun `test that clicking the tile invokes the on item clicked lambda`() {
        val onItemClicked = mock<() -> Unit>()

        with(composeTestRule) {
            setContent {
                VideoCompressionTile(
                    maximumNonChargingVideoCompressionSize = 500,
                    onItemClicked = onItemClicked,
                )
            }

            onNodeWithTag(VIDEO_COMPRESSION_TILE).performClick()
        }

        verify(onItemClicked).invoke()
    }
}