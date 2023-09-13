package mega.privacy.android.feature.devicecenter.ui.bottomsheet.body

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.feature.devicecenter.ui.bottomsheet.tiles.BOTTOM_SHEET_TILE_INFO
import mega.privacy.android.feature.devicecenter.ui.bottomsheet.tiles.BOTTOM_SHEET_TILE_SHOW_IN_BACKUPS
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test class for [BackupFolderBottomSheetBody]
 */
@RunWith(AndroidJUnit4::class)
internal class BackupFolderBottomSheetBodyTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that the bottom sheet body is displayed with the correct tiles`() {
        composeTestRule.setContent {
            BackupFolderBottomSheetBody(
                onShowInBackupsClicked = {},
                onInfoClicked = {},
            )
        }
        composeTestRule.onNodeWithTag(BOTTOM_SHEET_BODY_BACKUP_FOLDER).assertIsDisplayed()
        composeTestRule.onNodeWithTag(BOTTOM_SHEET_TILE_SHOW_IN_BACKUPS).assertIsDisplayed()
        composeTestRule.onNodeWithTag(BOTTOM_SHEET_TILE_INFO).assertIsDisplayed()
    }
}