package mega.privacy.android.app.presentation.settings.camerauploads.tiles

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.settings.camerauploads.tiles.MEDIA_UPLOADS_LOCAL_FOLDER_TILE
import mega.privacy.android.app.presentation.settings.camerauploads.tiles.MEDIA_UPLOADS_LOCAL_FOLDER_TILE_DIVIDER
import mega.privacy.android.app.presentation.settings.camerauploads.tiles.MediaUploadsLocalFolderTile
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import mega.privacy.android.app.onNodeWithText

/**
 * Test class for [MediaUploadsLocalFolderTile]
 */
@RunWith(AndroidJUnit4::class)
internal class MediaUploadsLocalFolderTileTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that the tile is shown`() {
        val secondaryFolderPath = "secondary/folder/path"
        with(composeTestRule) {
            setContent {
                MediaUploadsLocalFolderTile(
                    secondaryFolderPath = secondaryFolderPath,
                    onItemClicked = {},
                )
            }

            onNodeWithTag(MEDIA_UPLOADS_LOCAL_FOLDER_TILE).assertIsDisplayed()
            onNodeWithText(R.string.settings_local_secondary_folder).assertExists()
            onNodeWithText(secondaryFolderPath).assertExists()
            onNodeWithTag(MEDIA_UPLOADS_LOCAL_FOLDER_TILE_DIVIDER).assertIsDisplayed()
        }
    }

    @Test
    fun `test that clicking the tile invokes the on item clicked lambda`() {
        val onItemClicked = mock<() -> Unit>()
        with(composeTestRule) {
            setContent {
                MediaUploadsLocalFolderTile(
                    secondaryFolderPath = "secondary/folder/path",
                    onItemClicked = onItemClicked,
                )
            }

            onNodeWithTag(MEDIA_UPLOADS_LOCAL_FOLDER_TILE).performClick()

            verify(onItemClicked).invoke()
        }
    }
}