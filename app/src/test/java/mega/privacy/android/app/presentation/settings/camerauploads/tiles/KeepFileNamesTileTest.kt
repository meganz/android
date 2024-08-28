package mega.privacy.android.app.presentation.settings.camerauploads.tiles

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.settings.camerauploads.tiles.KEEP_FILE_NAMES_TILE
import mega.privacy.android.app.presentation.settings.camerauploads.tiles.KEEP_FILE_NAMES_TILE_CHECKBOX
import mega.privacy.android.app.presentation.settings.camerauploads.tiles.KEEP_FILE_NAMES_TILE_DIVIDER
import mega.privacy.android.app.presentation.settings.camerauploads.tiles.KeepFileNamesTile
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import mega.privacy.android.app.onNodeWithText

/**
 * Test class for [KeepFileNamesTile]
 */
@RunWith(AndroidJUnit4::class)
internal class KeepFileNamesTileTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that the tile is shown`() {
        composeTestRule.setContent {
            KeepFileNamesTile(
                isChecked = true,
                onCheckedChange = {},
            )
        }

        composeTestRule.onNodeWithTag(KEEP_FILE_NAMES_TILE).assertIsDisplayed()
        composeTestRule.onNodeWithText(R.string.settings_keep_file_names).assertExists()
        composeTestRule.onNodeWithTag(KEEP_FILE_NAMES_TILE_CHECKBOX).assertIsDisplayed()
        composeTestRule.onNodeWithTag(KEEP_FILE_NAMES_TILE_DIVIDER).assertIsDisplayed()
    }

    @Test
    fun `test that clicking the checkbox invokes the on checked change lambda`() {
        val onCheckedChange = mock<(Boolean) -> Unit>()
        composeTestRule.setContent {
            KeepFileNamesTile(
                isChecked = true,
                onCheckedChange = onCheckedChange,
            )
        }

        composeTestRule.onNodeWithTag(KEEP_FILE_NAMES_TILE_CHECKBOX).performClick()

        verify(onCheckedChange).invoke(any())
    }
}