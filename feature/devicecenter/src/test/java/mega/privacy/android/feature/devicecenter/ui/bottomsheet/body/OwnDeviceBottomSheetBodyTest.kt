package mega.privacy.android.feature.devicecenter.ui.bottomsheet.body

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.feature.devicecenter.ui.bottomsheet.tiles.BOTTOM_SHEET_TILE_CAMERA_UPLOADS
import mega.privacy.android.feature.devicecenter.ui.bottomsheet.tiles.BOTTOM_SHEET_TILE_INFO
import mega.privacy.android.feature.devicecenter.ui.bottomsheet.tiles.BOTTOM_SHEET_TILE_RENAME_DEVICE
import mega.privacy.android.feature.devicecenter.ui.bottomsheet.tiles.TEST_TAG_BOTTOM_SHEET_TILE_ADD_NEW_SYNC
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test class for [OwnDeviceBottomSheetBody]
 */
@RunWith(AndroidJUnit4::class)
internal class OwnDeviceBottomSheetBodyTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that the bottom sheet body is displayed with the correct tiles when camera uploads is enabled`() {
        composeTestRule.setContent {
            OwnDeviceBottomSheetBody(
                isCameraUploadsEnabled = true,
                hasSyncedFolders = true,
                onCameraUploadsClicked = {},
                onRenameDeviceClicked = {},
                onInfoClicked = {},
                onAddNewSyncClicked = {},
                isFreeAccount = false,
            )
        }
        composeTestRule.onNodeWithTag(BOTTOM_SHEET_BODY_OWN_DEVICE).assertIsDisplayed()
        composeTestRule.onNodeWithTag(BOTTOM_SHEET_TILE_CAMERA_UPLOADS).assertIsDisplayed()
        composeTestRule.onNodeWithTag(BOTTOM_SHEET_TILE_RENAME_DEVICE).assertIsDisplayed()
        composeTestRule.onNodeWithTag(BOTTOM_SHEET_TILE_INFO).assertIsDisplayed()
    }

    @Test
    fun `test that the bottom sheet body is displayed with the correct tiles when camera uploads is disabled`() {
        composeTestRule.setContent {
            OwnDeviceBottomSheetBody(
                isCameraUploadsEnabled = false,
                hasSyncedFolders = true,
                onCameraUploadsClicked = {},
                onRenameDeviceClicked = {},
                onInfoClicked = {},
                onAddNewSyncClicked = {},
                isFreeAccount = false,
            )
        }
        composeTestRule.onNodeWithTag(BOTTOM_SHEET_BODY_OWN_DEVICE).assertIsDisplayed()
        composeTestRule.onNodeWithTag(BOTTOM_SHEET_TILE_CAMERA_UPLOADS).assertIsDisplayed()
        composeTestRule.onNodeWithTag(BOTTOM_SHEET_TILE_RENAME_DEVICE).assertIsDisplayed()
        composeTestRule.onNodeWithTag(BOTTOM_SHEET_TILE_INFO).assertDoesNotExist()
    }

    @Test
    fun `test that the bottom sheet body is displayed with the correct tiles when sync feature flag is enabled`() {
        composeTestRule.setContent {
            OwnDeviceBottomSheetBody(
                isCameraUploadsEnabled = true,
                hasSyncedFolders = true,
                onCameraUploadsClicked = {},
                onRenameDeviceClicked = {},
                onInfoClicked = {},
                onAddNewSyncClicked = {},
                isFreeAccount = false,
                isSyncFeatureFlagEnabled = true,
            )
        }
        composeTestRule.onNodeWithTag(BOTTOM_SHEET_BODY_OWN_DEVICE).assertIsDisplayed()
        composeTestRule.onNodeWithTag(BOTTOM_SHEET_TILE_CAMERA_UPLOADS).assertIsNotDisplayed()
        composeTestRule.onNodeWithTag(TEST_TAG_BOTTOM_SHEET_TILE_ADD_NEW_SYNC).assertIsDisplayed()
        composeTestRule.onNodeWithTag(BOTTOM_SHEET_TILE_RENAME_DEVICE).assertIsDisplayed()
        composeTestRule.onNodeWithTag(BOTTOM_SHEET_TILE_INFO).assertIsDisplayed()
    }
}