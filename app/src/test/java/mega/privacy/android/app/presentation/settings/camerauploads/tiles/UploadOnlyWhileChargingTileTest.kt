package mega.privacy.android.app.presentation.settings.camerauploads.tiles

import mega.privacy.android.shared.resources.R as SharedR
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.settings.camerauploads.tiles.UPLOAD_ONLY_WHILE_CHARGING_TILE
import mega.privacy.android.app.presentation.settings.camerauploads.tiles.UPLOAD_ONLY_WHILE_CHARGING_TILE_DIVIDER
import mega.privacy.android.app.presentation.settings.camerauploads.tiles.UPLOAD_ONLY_WHILE_CHARGING_TILE_SWITCH
import mega.privacy.android.app.presentation.settings.camerauploads.tiles.UploadOnlyWhileChargingTile
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import mega.privacy.android.app.onNodeWithText


/**
 * Test class for [UploadOnlyWhileChargingTile]
 */
@RunWith(AndroidJUnit4::class)
internal class UploadOnlyWhileChargingTileTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that the tile is shown`() {
        initializeComposeContent(isChecked = false)

        with(composeTestRule) {
            onNodeWithTag(UPLOAD_ONLY_WHILE_CHARGING_TILE).assertIsDisplayed()
            onNodeWithText(SharedR.string.settings_camera_uploads_upload_only_while_charging_option_name).assertExists()
            onNodeWithText(SharedR.string.settings_camera_uploads_upload_only_while_charging_option_body).assertExists()
            onNodeWithTag(UPLOAD_ONLY_WHILE_CHARGING_TILE_SWITCH).assertIsDisplayed()
            onNodeWithTag(UPLOAD_ONLY_WHILE_CHARGING_TILE_DIVIDER).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the switch is checked when upload only while charging is enabled`() {
        initializeComposeContent(isChecked = true)

        composeTestRule.onNodeWithTag(UPLOAD_ONLY_WHILE_CHARGING_TILE_SWITCH).assertIsOn()
    }

    @Test
    fun `test that the switch is not checked when upload only while charging is disabled`() {
        initializeComposeContent(isChecked = false)

        composeTestRule.onNodeWithTag(UPLOAD_ONLY_WHILE_CHARGING_TILE_SWITCH).assertIsOff()
    }

    @Test
    fun `test that clicking the switch invokes the on checked change lambda`() {
        val onCheckedChange = mock<(Boolean) -> Unit>()
        with(composeTestRule) {
            setContent {
                UploadOnlyWhileChargingTile(
                    isChecked = false,
                    onCheckedChange = onCheckedChange,
                )
            }

            onNodeWithTag(UPLOAD_ONLY_WHILE_CHARGING_TILE_SWITCH).performClick()
            verify(onCheckedChange).invoke(any())
        }
    }

    private fun initializeComposeContent(isChecked: Boolean) {
        composeTestRule.setContent {
            UploadOnlyWhileChargingTile(
                isChecked = isChecked,
                onCheckedChange = {},
            )
        }
    }
}