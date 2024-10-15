package mega.privacy.android.feature.devicecenter.ui.bottomsheet.body

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.feature.devicecenter.ui.bottomsheet.tiles.BOTTOM_SHEET_TILE_CAMERA_UPLOADS
import mega.privacy.android.feature.devicecenter.ui.bottomsheet.tiles.BOTTOM_SHEET_TILE_INFO
import mega.privacy.android.feature.devicecenter.ui.bottomsheet.tiles.BOTTOM_SHEET_TILE_RENAME_DEVICE
import mega.privacy.android.feature.devicecenter.ui.bottomsheet.tiles.TEST_TAG_BOTTOM_SHEET_TILE_ADD_BACKUP
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
    fun `test that the bottom sheet body is displayed with the correct tiles`() {
        composeTestRule.setContent {
            OwnDeviceBottomSheetBody(
                isCameraUploadsEnabled = true,
                hasSyncedFolders = true,
                onRenameDeviceClicked = {},
                onInfoClicked = {},
                onAddNewSyncClicked = {},
                onAddBackupClicked = {},
                isFreeAccount = false,
                isBackupForAndroidEnabled = true,
                isAndroidSyncFeatureEnabled = true
            )
        }
        composeTestRule.onNodeWithTag(BOTTOM_SHEET_BODY_OWN_DEVICE).assertIsDisplayed()
        composeTestRule.onNodeWithTag(BOTTOM_SHEET_TILE_CAMERA_UPLOADS).assertIsNotDisplayed()
        composeTestRule.onNodeWithTag(TEST_TAG_BOTTOM_SHEET_TILE_ADD_NEW_SYNC).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TEST_TAG_BOTTOM_SHEET_TILE_ADD_BACKUP).assertIsDisplayed()
        composeTestRule.onNodeWithTag(BOTTOM_SHEET_TILE_RENAME_DEVICE).assertIsDisplayed()
        composeTestRule.onNodeWithTag(BOTTOM_SHEET_TILE_INFO).assertIsDisplayed()
    }
}