package mega.privacy.android.app.presentation.settings.camerauploads.tiles

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.settings.camerauploads.tiles.INCLUDE_LOCATION_TAGS_TILE
import mega.privacy.android.app.presentation.settings.camerauploads.tiles.INCLUDE_LOCATION_TAGS_TILE_DIVIDER
import mega.privacy.android.app.presentation.settings.camerauploads.tiles.INCLUDE_LOCATION_TAGS_TILE_SWITCH
import mega.privacy.android.app.presentation.settings.camerauploads.tiles.IncludeLocationTagsTile
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import mega.privacy.android.app.onNodeWithText

/**
 * Test class for [IncludeLocationTagsTile]
 */
@RunWith(AndroidJUnit4::class)
internal class IncludeLocationTagsTileTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that the tile is shown`() {
        initializeComposeContent(isChecked = false)

        composeTestRule.onNodeWithTag(INCLUDE_LOCATION_TAGS_TILE).assertIsDisplayed()
        composeTestRule.onNodeWithText(R.string.settings_camera_upload_include_gps).assertExists()
        composeTestRule.onNodeWithText(R.string.settings_camera_upload_include_gps_helper_label)
            .assertExists()
        composeTestRule.onNodeWithTag(INCLUDE_LOCATION_TAGS_TILE_SWITCH).assertIsDisplayed()
        composeTestRule.onNodeWithTag(INCLUDE_LOCATION_TAGS_TILE_DIVIDER).assertIsDisplayed()
    }

    @Test
    fun `test that the switch is checked when include location tags is enabled`() {
        initializeComposeContent(isChecked = true)

        composeTestRule.onNodeWithTag(INCLUDE_LOCATION_TAGS_TILE_SWITCH).assertIsOn()
    }

    @Test
    fun `test that the switch is not checked when include location tags is disabled`() {
        initializeComposeContent(isChecked = false)

        composeTestRule.onNodeWithTag(INCLUDE_LOCATION_TAGS_TILE_SWITCH).assertIsOff()
    }

    @Test
    fun `test that clicking the switch invokes the on checked change lambda`() {
        val onCheckedChange = mock<(Boolean) -> Unit>()
        composeTestRule.setContent {
            IncludeLocationTagsTile(
                isChecked = false,
                onCheckedChange = onCheckedChange,
            )
        }

        composeTestRule.onNodeWithTag(INCLUDE_LOCATION_TAGS_TILE_SWITCH).performClick()
        verify(onCheckedChange).invoke(any())
    }

    private fun initializeComposeContent(isChecked: Boolean) {
        composeTestRule.setContent {
            IncludeLocationTagsTile(
                isChecked = isChecked,
                onCheckedChange = {},
            )
        }
    }
}