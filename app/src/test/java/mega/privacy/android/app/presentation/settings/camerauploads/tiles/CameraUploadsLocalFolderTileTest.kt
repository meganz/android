package mega.privacy.android.app.presentation.settings.camerauploads.tiles

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.settings.camerauploads.tiles.CAMERA_UPLOADS_LOCAL_FOLDER_TILE
import mega.privacy.android.app.presentation.settings.camerauploads.tiles.CAMERA_UPLOADS_LOCAL_FOLDER_TILE_DIVIDER
import mega.privacy.android.app.presentation.settings.camerauploads.tiles.CameraUploadsLocalFolderTile
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import mega.privacy.android.app.onNodeWithText

/**
 * Test class for [CameraUploadsLocalFolderTile]
 */
@RunWith(AndroidJUnit4::class)
internal class CameraUploadsLocalFolderTileTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that the tile is shown`() {
        val primaryFolderPath = "primary/folder/path"
        with(composeTestRule) {
            setContent {
                CameraUploadsLocalFolderTile(
                    primaryFolderPath = primaryFolderPath,
                    onItemClicked = {},
                )
            }

            onNodeWithTag(CAMERA_UPLOADS_LOCAL_FOLDER_TILE).assertIsDisplayed()
            onNodeWithText(R.string.settings_local_camera_upload_folder).assertExists()
            onNodeWithText(primaryFolderPath).assertExists()
            onNodeWithTag(CAMERA_UPLOADS_LOCAL_FOLDER_TILE_DIVIDER).assertIsDisplayed()
        }
    }

    @Test
    fun `test that clicking the tile invokes the on item clicked lambda`() {
        val onItemClicked = mock<() -> Unit>()
        with(composeTestRule) {
            setContent {
                CameraUploadsLocalFolderTile(
                    primaryFolderPath = "primary/folder/path",
                    onItemClicked = onItemClicked,
                )
            }

            onNodeWithTag(CAMERA_UPLOADS_LOCAL_FOLDER_TILE).performClick()

            verify(onItemClicked).invoke()
        }
    }
}