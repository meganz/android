package mega.privacy.android.feature.sync.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.feature.sync.ui.newfolderpair.SyncNewFolderScreen
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SyncNewFolderScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Ignore
    @Test
    fun `test that all sync Sync New Folder components are visible`() {
        composeTestRule.setContent {
            SyncNewFolderScreen(
                folderPairName = "",
                selectedLocalFolder = "",
                selectedMegaFolder = null,
                localFolderSelected = {},
                folderNameChanged = {},
                syncClicked = {},
            )
        }

        composeTestRule.onNodeWithText("Device folder")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("MEGA folder")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Method")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Select")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Two way sync")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Sync")
            .assertIsDisplayed()
    }
}