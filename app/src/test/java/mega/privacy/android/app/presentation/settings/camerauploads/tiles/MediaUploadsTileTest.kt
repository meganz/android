package mega.privacy.android.app.presentation.settings.camerauploads.tiles

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.settings.camerauploads.tiles.MEDIA_UPLOADS_TILE
import mega.privacy.android.app.presentation.settings.camerauploads.tiles.MEDIA_UPLOADS_TILE_DIVIDER
import mega.privacy.android.app.presentation.settings.camerauploads.tiles.MediaUploadsTile
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import mega.privacy.android.app.onNodeWithText

/**
 * Test class for [MediaUploadsTile]
 */
@RunWith(AndroidJUnit4::class)
internal class MediaUploadsTileTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that the tile is shown`() {
        initializeComposeContent(isMediaUploadsEnabled = true)

        composeTestRule.onNodeWithTag(MEDIA_UPLOADS_TILE).assertIsDisplayed()
    }

    @Test
    fun `test that the tile with correct title is shown when media uploads is enabled`() {
        initializeComposeContent(isMediaUploadsEnabled = true)

        composeTestRule.onNodeWithText(R.string.settings_secondary_upload_off).assertExists()
    }

    @Test
    fun `test that the tile with correct title is shown when media uploads is disabled`() {
        initializeComposeContent(isMediaUploadsEnabled = false)

        composeTestRule.onNodeWithText(R.string.settings_secondary_upload_on).assertExists()
    }

    @Test
    fun `test that the divider is shown when media uploads is enabled`() {
        initializeComposeContent(isMediaUploadsEnabled = true)

        composeTestRule.onNodeWithTag(MEDIA_UPLOADS_TILE_DIVIDER).assertIsDisplayed()
    }

    @Test
    fun `test that the divider is hidden when media uploads is disabled`() {
        initializeComposeContent(isMediaUploadsEnabled = false)

        composeTestRule.onNodeWithTag(MEDIA_UPLOADS_TILE_DIVIDER).assertDoesNotExist()
    }

    @Test
    fun `test that clicking the tile invokes the on item clicked lambda`() {
        val onItemClicked = mock<(Boolean) -> Unit>()
        composeTestRule.setContent {
            MediaUploadsTile(
                isMediaUploadsEnabled = true,
                onItemClicked = onItemClicked,
            )
        }
        composeTestRule.onNodeWithTag(MEDIA_UPLOADS_TILE).performClick()

        verify(onItemClicked).invoke(any())
    }

    private fun initializeComposeContent(isMediaUploadsEnabled: Boolean) {
        composeTestRule.setContent {
            MediaUploadsTile(
                isMediaUploadsEnabled = isMediaUploadsEnabled,
                onItemClicked = {},
            )
        }
    }
}