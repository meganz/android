package mega.privacy.android.app.presentation.settings.camerauploads.tiles

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.settings.camerauploads.model.UploadOptionUiItem
import mega.privacy.android.app.presentation.settings.camerauploads.tiles.FILE_UPLOAD_TILE
import mega.privacy.android.app.presentation.settings.camerauploads.tiles.FILE_UPLOAD_TILE_DIVIDER
import mega.privacy.android.app.presentation.settings.camerauploads.tiles.FileUploadTile
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import mega.privacy.android.app.onNodeWithText

/**
 * Test class for [FileUploadTile]
 */
@RunWith(AndroidJUnit4::class)
internal class FileUploadTileTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that the tile is shown`() {
        initializeComposeContent(uploadOptionUiItem = UploadOptionUiItem.PhotosOnly)

        composeTestRule.onNodeWithTag(FILE_UPLOAD_TILE).assertIsDisplayed()
        composeTestRule.onNodeWithTag(FILE_UPLOAD_TILE_DIVIDER).assertIsDisplayed()
        composeTestRule.onNodeWithText(R.string.settings_camera_upload_what_to_upload)
            .assertExists()
    }

    @Test
    fun `test that the tile with correct content is shown when the upload option ui item is photos only`() {
        initializeComposeContent(uploadOptionUiItem = UploadOptionUiItem.PhotosOnly)

        composeTestRule.onNodeWithText(R.string.settings_camera_upload_only_photos)
            .assertExists()
    }

    @Test
    fun `test that the tile with correct content is shown whe the upload option ui item is videos only`() {
        initializeComposeContent(uploadOptionUiItem = UploadOptionUiItem.VideosOnly)

        composeTestRule.onNodeWithText(R.string.settings_camera_upload_only_videos)
            .assertExists()
    }

    @Test
    fun `test that the tile with correct content is shown whe the upload option ui item is photos and videos`() {
        initializeComposeContent(uploadOptionUiItem = UploadOptionUiItem.PhotosAndVideos)

        composeTestRule.onNodeWithText(R.string.settings_camera_upload_photos_and_videos)
            .assertExists()
    }

    @Test
    fun `test that clicking the tile invokes the on item clicked lambda`() {
        val onItemClicked = mock<() -> Unit>()
        composeTestRule.setContent {
            FileUploadTile(
                uploadOptionUiItem = UploadOptionUiItem.PhotosOnly,
                onItemClicked = onItemClicked,
            )
        }
        composeTestRule.onNodeWithTag(FILE_UPLOAD_TILE).performClick()

        verify(onItemClicked).invoke()
    }

    private fun initializeComposeContent(uploadOptionUiItem: UploadOptionUiItem) {
        composeTestRule.setContent {
            FileUploadTile(
                uploadOptionUiItem = uploadOptionUiItem,
                onItemClicked = {},
            )
        }
    }
}