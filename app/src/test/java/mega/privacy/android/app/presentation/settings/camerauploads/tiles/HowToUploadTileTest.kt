package mega.privacy.android.app.presentation.settings.camerauploads.tiles

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.settings.camerauploads.model.UploadConnectionType
import mega.privacy.android.app.presentation.settings.camerauploads.tiles.HOW_TO_UPLOAD_TILE
import mega.privacy.android.app.presentation.settings.camerauploads.tiles.HOW_TO_UPLOAD_TILE_DIVIDER
import mega.privacy.android.app.presentation.settings.camerauploads.tiles.HowToUploadTile
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import mega.privacy.android.app.onNodeWithText

/**
 * Test class for [HowToUploadTileTest]
 */
@RunWith(AndroidJUnit4::class)
internal class HowToUploadTileTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that the tile is shown`() {
        initializeComposeContent(uploadConnectionType = UploadConnectionType.WIFI)
        composeTestRule.onNodeWithTag(HOW_TO_UPLOAD_TILE).assertIsDisplayed()
        composeTestRule.onNodeWithTag(HOW_TO_UPLOAD_TILE_DIVIDER).assertIsDisplayed()
        composeTestRule.onNodeWithText(R.string.settings_camera_upload_how_to_upload).assertExists()
    }

    @Test
    fun `test that the tile with correct content is shown when the upload connection type is wifi`() {
        initializeComposeContent(uploadConnectionType = UploadConnectionType.WIFI)
        composeTestRule.onNodeWithText(R.string.cam_sync_wifi).assertExists()
    }

    @Test
    fun `test that the tile with correct content is shown when the upload connection type is wifi or mobile data`() {
        initializeComposeContent(uploadConnectionType = UploadConnectionType.WIFI_OR_MOBILE_DATA)
        composeTestRule.onNodeWithText(R.string.cam_sync_data).assertExists()
    }

    @Test
    fun `test that clicking the tile invokes the on action clicked lambda`() {
        val onActionClicked = mock<() -> Unit>()
        composeTestRule.setContent {
            HowToUploadTile(
                uploadConnectionType = UploadConnectionType.WIFI,
                onItemClicked = onActionClicked,
            )
        }
        composeTestRule.onNodeWithTag(HOW_TO_UPLOAD_TILE).performClick()
        verify(onActionClicked).invoke()
    }

    private fun initializeComposeContent(uploadConnectionType: UploadConnectionType) {
        composeTestRule.setContent {
            HowToUploadTile(
                uploadConnectionType = uploadConnectionType,
                onItemClicked = {},
            )
        }
    }
}