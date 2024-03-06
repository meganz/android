package mega.privacy.android.feature.devicecenter.ui.bottomsheet.body

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.feature.devicecenter.ui.bottomsheet.tiles.BOTTOM_SHEET_TILE_INFO
import mega.privacy.android.feature.devicecenter.ui.bottomsheet.tiles.BOTTOM_SHEET_TILE_RENAME_DEVICE
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test class for [OtherDeviceBottomSheetBody]
 */
@RunWith(AndroidJUnit4::class)
internal class OtherDeviceBottomSheetBodyTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that the bottom sheet body is displayed with the correct tiles`() {
        composeTestRule.setContent {
            OtherDeviceBottomSheetBody(
                onRenameDeviceClicked = {},
                onInfoClicked = {},
            )
        }
        composeTestRule.onNodeWithTag(BOTTOM_SHEET_BODY_OTHER_DEVICE).assertIsDisplayed()
        composeTestRule.onNodeWithTag(BOTTOM_SHEET_TILE_RENAME_DEVICE).assertIsDisplayed()
        composeTestRule.onNodeWithTag(BOTTOM_SHEET_TILE_INFO).assertIsDisplayed()
    }
}