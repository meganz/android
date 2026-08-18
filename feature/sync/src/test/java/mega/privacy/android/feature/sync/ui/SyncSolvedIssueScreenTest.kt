package mega.privacy.android.feature.sync.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.feature.sync.ui.model.SolvedIssueUiItem
import mega.privacy.android.feature.sync.ui.synclist.solvedissues.SyncSolvedIssuesScreen
import mega.privacy.android.feature.sync.ui.views.TAG_SYNC_LIST_SCREEN_NO_ITEMS
import mega.privacy.android.icon.pack.R as IconPackR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "fr-rFr-w1080dp-h1920dp")
internal class SyncSolvedIssueScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()


    @Test
    fun `test that solved issues list is displayed with local folder name`() {
        val folderName = "Folder name"
        val folderPath = "/storage/emulated/0/Folder name"
        val resolutionExplanation = "Folders were merged"
        val solvedIssues = listOf(
            SolvedIssueUiItem(
                nodeIds = listOf(),
                nodeNames = listOf(folderName),
                localPaths = listOf(folderPath),
                resolutionExplanation = resolutionExplanation,
                icon = IconPackR.drawable.ic_folder_medium_solid,
            )
        )
        composeTestRule.setContent {
            SyncSolvedIssuesScreen(solvedIssues = solvedIssues)
        }

        composeTestRule.onNodeWithText(folderName).assertIsDisplayed()
        composeTestRule.onNodeWithText(folderPath).assertIsDisplayed()
        composeTestRule.onNodeWithText(resolutionExplanation).assertIsDisplayed()
    }

    @Test
    fun `test that solved issues list is displayed with MEGA folder name`() {
        val folderName = "Folder name"
        val resolutionExplanation = "Folders were merged"
        val solvedIssues = listOf(
            SolvedIssueUiItem(
                nodeIds = listOf(NodeId(1L)),
                nodeNames = listOf(folderName),
                localPaths = listOf(),
                resolutionExplanation = resolutionExplanation,
                icon = IconPackR.drawable.ic_folder_medium_solid,
            )
        )
        composeTestRule.setContent {
            SyncSolvedIssuesScreen(solvedIssues = solvedIssues)
        }

        composeTestRule.onNodeWithText(folderName).assertIsDisplayed()
        composeTestRule.onNodeWithText(resolutionExplanation).assertIsDisplayed()
    }

    @Test
    fun `test that solved issues list is empty where there are no solved issues`() {
        composeTestRule.setContent {
            SyncSolvedIssuesScreen(solvedIssues = emptyList())
        }

        composeTestRule.onNodeWithTag(TAG_SYNC_LIST_SCREEN_NO_ITEMS)
            .assertIsDisplayed()
    }
}
