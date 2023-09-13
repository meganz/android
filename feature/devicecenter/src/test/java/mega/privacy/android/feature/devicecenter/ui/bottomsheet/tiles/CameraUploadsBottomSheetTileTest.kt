package mega.privacy.android.feature.devicecenter.ui.bottomsheet.tiles

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test class for [CameraUploadsBottomSheetTile]
 */
@RunWith(AndroidJUnit4::class)
internal class CameraUploadsBottomSheetTileTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that the tile is displayed`() {
        composeTestRule.setContent {
            CameraUploadsBottomSheetTile(
                isCameraUploadsEnabled = true,
                onActionClicked = {},
            )
        }
        composeTestRule.onNodeWithTag(BOTTOM_SHEET_TILE_CAMERA_UPLOADS).assertIsDisplayed()
    }
}