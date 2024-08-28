package mega.privacy.android.app.presentation.settings.camerauploads.tiles

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.settings.camerauploads.tiles.CAMERA_UPLOADS_FOLDER_NODE_TILE
import mega.privacy.android.app.presentation.settings.camerauploads.tiles.CAMERA_UPLOADS_FOLDER_NODE_TILE_DIVIDER
import mega.privacy.android.app.presentation.settings.camerauploads.tiles.CameraUploadsFolderNodeTile
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import mega.privacy.android.app.onNodeWithText

/**
 * Test class for [CameraUploadsFolderNodeTile]
 */
@RunWith(AndroidJUnit4::class)
internal class CameraUploadsFolderNodeTileTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that the tile is shown`() {
        val primaryFolderName = "Camera Uploads"
        with(composeTestRule) {
            setContent {
                CameraUploadsFolderNodeTile(
                    primaryFolderName = primaryFolderName,
                    onItemClicked = {},
                )
            }

            onNodeWithTag(CAMERA_UPLOADS_FOLDER_NODE_TILE).assertIsDisplayed()
            onNodeWithText(R.string.settings_mega_camera_upload_folder).assertExists()
            onNodeWithText(primaryFolderName).assertExists()
            onNodeWithTag(CAMERA_UPLOADS_FOLDER_NODE_TILE_DIVIDER).assertIsDisplayed()
        }
    }

    @Test
    fun `test that clicking the tile invokes the on item clicked lambda`() {
        val onItemClicked = mock<() -> Unit>()
        with(composeTestRule) {
            setContent {
                CameraUploadsFolderNodeTile(
                    primaryFolderName = "Camera Uploads",
                    onItemClicked = onItemClicked,
                )
            }

            onNodeWithTag(CAMERA_UPLOADS_FOLDER_NODE_TILE).performClick()

            verify(onItemClicked).invoke()
        }
    }
}